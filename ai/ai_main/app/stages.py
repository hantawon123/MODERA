"""단계별 분석 실행 (명세 10-1) 과 결과 콜백 (명세 10-4).

프로토타입의 통짜 run_pipeline 을 stage 단위로 분리했다.
한 요청은 LLM / IMAGE_ANALYSIS / AGENT 중 하나만 실행하고, 다음 단계로
넘길지는 Spring 이 결정한다.
"""

import asyncio
import logging
import time
from contextlib import contextmanager
from datetime import datetime, timezone
from typing import Any

from . import gemini_client, search, spring_client, storage
from .category import CategoryResolution, resolve_category
from .config import get_settings
from .jobs import job_registry, job_store
from .schemas import (
    AnalyzeRequest,
    CallbackError,
    CallbackRequest,
    CategoryCandidate,
    KnowledgeCandidates,
)

logger = logging.getLogger(__name__)

# 아직 쌓인 카테고리가 하나도 없을 때 쓰는 초기 후보(콜드 스타트).
# 이후 카테고리는 AGENT 가 분석하면서 기존 후보에 못 붙일 때 새로 만든다.
# 사람이 직접 만드는 경로는 없다.
# 원칙적으로는 Spring DB 에 시드로 넣고 10-5 로 내려받는 편이 낫다.
DEFAULT_CATEGORIES = [
    "쇼핑", "음식", "여행", "예약", "할인", "금융",
    "미용", "학습", "취업", "IT", "뉴스", "부동산",
    "건강", "엔터", "자동차", "반려동물", "기타",
]


def _now_iso() -> str:
    return datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%S.%f")[:-3] + "Z"


@contextmanager
def _timed(timings: dict[str, float], name: str):
    """구간 소요 시간을 초 단위로 기록한다.

    "1장에 몇 초 걸리는가" 를 추정이 아니라 로그로 답하기 위한 계측이다.
    예외가 나도 그 시점까지의 시간은 남긴다.
    """
    started = time.perf_counter()
    try:
        yield
    finally:
        timings[name] = round(time.perf_counter() - started, 2)


# 동시 실행 제한용 세마포어. 실행 중인 이벤트 루프에 바인딩되도록 지연 생성한다.
_stage_semaphore: asyncio.Semaphore | None = None


def _get_semaphore() -> asyncio.Semaphore:
    global _stage_semaphore
    if _stage_semaphore is None:
        _stage_semaphore = asyncio.Semaphore(get_settings().max_concurrent_stages)
    return _stage_semaphore


# ── 1) LLM 정보성 판정 ────────────────────────────────────────────────────
def run_llm(ocr_text: str) -> tuple[str, dict[str, Any]]:
    """OCR 텍스트가 저장·검색할 가치가 있는 정보인지 판정한다."""
    if not ocr_text or not ocr_text.strip():
        # OCR 텍스트가 없으면 모델을 호출하지 않고 EMPTY 로 돌려준다.
        return "EMPTY", {"informative": False, "confidence": 0.0,
                         "reason": "OCR 텍스트가 비어 있음"}

    settings = get_settings()
    prompt = (
        "너는 스크린샷 정보 분석기다. 아래 OCR 텍스트가 개인 지식 DB에 저장·검색할 가치가 있는 "
        "'정보성' 콘텐츠인지 판단하라.\n"
        "정보성 예: 상품/가격, 일정/예약, 쿠폰/할인, 장소/맛집, 채용, 개발 지식/오류, 기사 요지 등 "
        "나중에 다시 찾아볼 실질 정보.\n"
        "비정보성 예: 순수 UI 요소(홈/검색/설정 같은 내비게이션 텍스트만 있는 화면), 빈 화면, "
        "의미 없는 밈/장식.\n"
        "반드시 아래 JSON만 출력. 마크다운·설명 금지.\n"
        '{"informative": true, "confidence": 0.0, "reason": "간단한 근거"}\n\n'
        f"OCR 텍스트:\n{ocr_text}"
    )
    parsed = gemini_client.generate_json(settings.llm_model_name, [prompt])
    return "COMPLETED", {
        "informative": bool(parsed.get("informative", False)),
        "confidence": float(parsed.get("confidence", 0.0)),
        "reason": parsed.get("reason", ""),
    }


# ── 2) 서버 이미지 분석 ───────────────────────────────────────────────────
def run_image_analysis(image_ref: str, image_bytes: bytes | None = None) -> dict[str, Any]:
    """image_ref 는 http(s) URL 또는 s3Key 다. storage 가 형태를 보고 처리한다.

    image_bytes 를 넘기면 원본을 다시 내려받지 않는다(썸네일 만들 때 읽어둔 것 재사용).
    """
    settings = get_settings()
    if image_bytes is None:
        image_bytes = storage.fetch_image(image_ref)
    prompt = (
        "이미지를 분석하라. 콘텐츠 유형을 한 문장으로 설명하고(description), "
        "눈에 보이는 주요 객체(objects)와 화면에서 읽히는 핵심 텍스트/브랜드/가격/날짜"
        "(detected_texts)를 뽑아라.\n"
        "반드시 아래 JSON만 출력. 마크다운·설명 금지.\n"
        '{"description":"...","detected_texts":["..."],"objects":["..."]}'
    )
    parsed = gemini_client.generate_json(
        settings.vision_model_name, [prompt, gemini_client.image_part(image_bytes)]
    )
    return {
        "description": parsed.get("description", ""),
        "detectedTexts": parsed.get("detected_texts", []),
        "objects": parsed.get("objects", []),
    }


# ── 3) AGENT (OCR + 이미지 분석 종합) ─────────────────────────────────────
def run_agent_generation(
    ocr_text: str,
    image_analysis: dict[str, Any],
    candidate_names: list[str],
    max_tags: int | None,
    language: str | None,
    existing_tags: list[str] | None = None,
) -> dict[str, Any]:
    settings = get_settings()
    tag_rule = (
        f"[태그] 핵심 키워드 위주로 최대 {max_tags}개, 중복·과도한 일반 태그 제외.\n"
        if max_tags else "[태그] 중복·과도한 일반 태그 제외.\n"
    )
    # 기존 태그를 기준점으로 준다. 의미가 같은 태그의 난립('맛집' vs '맛집추천')을 막고
    # 어휘를 수렴시킨다. 카테고리의 기존 후보 제시와 같은 취지. 없으면 자유 생성(콜드스타트).
    if existing_tags:
        tag_rule += (
            f"기존 태그 목록: {existing_tags}. 의미가 같은 태그가 이 목록에 있으면 "
            "새로 만들지 말고 그대로 재사용하라. 목록에 맞는 것이 없을 때만 새 태그를 만들어라.\n"
        )
    tag_rule += "\n"
    language_rule = f"출력 언어는 {language} 로 한다.\n" if language else ""
    prompt = (
        "OCR 텍스트와 이미지 분석 결과를 종합해 개인 지식 DB용 메타데이터를 생성하라.\n\n"
        "[카테고리]\n"
        f"기존 카테고리 후보: {candidate_names}. 가능한 한 이 중에서 정확히 하나를 고르고, "
        "정말로 맞는 것이 없을 때만 새 카테고리 이름을 제안하라. "
        "categories 에는 최종 카테고리명 하나만 넣어라.\n\n"
        + tag_rule
        + "[주요정보] 사용자에게 보여줄 핵심 정보를 '항목: 값' 형태 문자열로 담아라. "
        "확인되지 않은 값은 넣지 마라(추측 금지).\n\n"
        + language_rule
        + "반드시 아래 JSON만 출력. 마크다운·설명 금지.\n"
        '{"title":"...","summary":"...","tags":["..."],"categories":["..."],'
        '"key_information":["..."],"analysis_confidence":0.0}\n\n'
        f"OCR: {ocr_text}\n"
        f"이미지 분석: {image_analysis}"
    )
    return gemini_client.generate_json(settings.llm_model_name, [prompt])


def _build_candidates(candidates: list[CategoryCandidate]) -> list[CategoryCandidate]:
    if candidates:
        return candidates
    # 이 사용자에게 쌓인 카테고리가 아직 없으면 기본 후보로 시작한다
    # (categoryId 없음 = 아직 저장되지 않은 후보).
    return [CategoryCandidate(category_id=None, name=name) for name in DEFAULT_CATEGORIES]


def _existing_tag_names(
    user_id: int, knowledge: KnowledgeCandidates, limit: int = 30
) -> list[str]:
    """태그 생성의 '기준점'. 사용자에게 이미 쌓인 태그를 모아 AGENT 가 재사용하게 한다.

    카테고리에 기존 후보를 제시하는 것과 대응된다. 두 소스를 합친다:
    - OpenSearch 태그 집계(자기 인덱스라 Spring 없이도 동작) — 사용 빈도 순
    - Spring 10-5 knowledge.tags (복귀 시)
    없으면 빈 리스트(콜드스타트 → 기존처럼 자유 생성).
    """
    names: list[str] = []
    seen: set[str] = set()

    def add(name: str) -> None:
        n = (name or "").strip()
        key = n.lower()
        if n and key not in seen:
            seen.add(key)
            names.append(n)

    # best-effort — 집계가 실패해도 태그 생성 자체는 막지 않는다.
    try:
        for row in search.aggregate_tags(user_id, limit=limit):
            add(row.get("name", ""))
    except Exception as e:
        logger.warning("기존 태그 집계 실패(무시하고 진행): %s", e)
    for tag in knowledge.tags:
        add(tag.name)

    return names[:limit]


async def run_agent(request: AnalyzeRequest) -> dict[str, Any]:
    ocr = request.input.ocr
    image_analysis = request.input.image_analysis
    ocr_text = (ocr.refined_text or ocr.raw_text) if ocr else ""
    analysis_dict = image_analysis.model_dump(by_alias=True) if image_analysis else {}
    options = request.options
    return await run_agent_core(
        user_id=request.user_id,
        image_id=request.image_id,
        ocr_text=ocr_text,
        analysis_dict=analysis_dict,
        max_tags=options.max_tags if options else None,
        language=options.language if options else None,
    )


async def run_agent_core(
    user_id: int,
    image_id: int,
    ocr_text: str,
    analysis_dict: dict[str, Any],
    max_tags: int | None = None,
    language: str | None = None,
) -> dict[str, Any]:
    """AGENT 본체. Spring 연동 경로와 앱 직결 경로가 함께 쓴다."""
    settings = get_settings()

    # 10-5 기존 태그·카테고리 후보 조회 (실패하면 기본 후보로 진행)
    knowledge = await spring_client.fetch_knowledge_candidates(user_id)
    candidates = _build_candidates(knowledge.categories)
    # 태그 난립 방지용 기준점(기존 태그). OpenSearch 집계라 스레드로 돌린다.
    existing_tags = await asyncio.to_thread(_existing_tag_names, user_id, knowledge)

    max_tags = max_tags or settings.default_max_tags

    generated = await asyncio.to_thread(
        run_agent_generation,
        ocr_text, analysis_dict, [c.name for c in candidates], max_tags, language,
        existing_tags,
    )

    summary = generated.get("summary", "") or generated.get("title", "")
    proposed = (generated.get("categories") or ["기타"])[0]

    # 요약 임베딩: 카테고리 판정에 쓰고, 검색 적재용으로 콜백에도 실어 보낸다.
    embedding_model, vectors = await asyncio.to_thread(
        gemini_client.embed, [summary], "DOCUMENT"
    )
    resolution: CategoryResolution = await asyncio.to_thread(
        resolve_category,
        vectors[0],
        proposed,
        candidates,
        settings.similarity_threshold,
        lambda texts: gemini_client.embed(texts, "DOCUMENT")[1],
    )
    logger.info(
        "카테고리 판정 imageId=%s 제안=%s → %s (유사도=%.3f, 기준=%s, 신규=%s)",
        image_id, proposed, resolution.name,
        resolution.similarity, resolution.matched_by, resolution.created,
    )

    tags = [str(t) for t in (generated.get("tags") or [])][:max_tags]
    return {
        "title": generated.get("title", ""),
        "summary": summary,
        "tags": tags,
        "categories": [resolution.name],
        "keyInformation": generated.get("key_information") or [],
        "analysisConfidence": float(generated.get("analysis_confidence", 0.0)),
        # 구조화 데이터는 MVP 범위에서 제외. 형태만 유지한다.
        "structuredData": {"type": None, "fields": {}},
        # 신규 카테고리 여부(스프링이 생성 판단에 사용). 계약 확정 필요.
        "categoryCreated": resolution.created,
        # 명세 6-2 categories[].confidence 로 내려줄 값
        "categoryConfidence": round(resolution.similarity, 3),
        # 검색용 문서 임베딩. Spring 이 pgvector 등에 그대로 적재한다.
        # (요약 텍스트 기준. purpose=DOCUMENT 로 생성)
        "documentVector": vectors[0],
        "embeddingModel": embedding_model,
        "embeddingDimension": len(vectors[0]),
    }


async def _index_for_search(request: AnalyzeRequest, result: dict[str, Any]) -> None:
    """AGENT 결과를 OpenSearch 에 색인한다(검색 전담).

    색인 실패가 분석 자체를 실패시키지 않도록 best-effort 로 처리한다.
    (OpenSearch 가 잠시 죽어도 분석·콜백은 정상 진행)
    """
    ocr = request.input.ocr
    raw_text = (ocr.refined_text or ocr.raw_text) if ocr else ""
    categories = result.get("categories") or []
    try:
        await asyncio.to_thread(
            search.index_document,
            image_id=request.image_id,
            user_id=request.user_id,
            title=result.get("title", ""),
            summary=result.get("summary", ""),
            tags=result.get("tags", []),
            category_name=categories[0] if categories else None,
            raw_text=raw_text,
            created_at=_now_iso(),
        )
    except Exception as e:
        logger.warning("검색 색인 실패 imageId=%s: %s (분석은 정상 진행)",
                       request.image_id, e)


# ── 앱 직결 전체 분석 ─────────────────────────────────────────────────────
# OCR 이 비었거나 정보성이 없다고 판정된 이미지에 붙는 시스템 카테고리.
OTHER_CATEGORY = "기타"


async def _index_as_other(
    job_id: int, image_id: int, user_id: int, s3_key: str, ocr_text: str
) -> None:
    """분석을 생략하고 '기타' 로만 분류해 색인한다(명세 10-4 처리 규칙 3·4).

    AGENT 를 돌리지 않으므로 제목·태그는 비어 있지만, 이미지 자체는 목록과
    검색에 남는다. 상태는 EMPTY 로 두어 앱이 '분석 생략'임을 구분할 수 있게 한다.
    """
    job_store.update(job_id, "PROCESSING", stage="INDEXING")
    result = {
        "title": "",
        "summary": "",
        "tags": [],
        "category": OTHER_CATEGORY,
        "key_information": [],
        "confidence": 0.0,
        "s3_key": s3_key,
    }
    try:
        await asyncio.to_thread(
            search.index_document,
            image_id=image_id,
            user_id=user_id,
            title="",
            summary="",
            tags=[],
            category_name=OTHER_CATEGORY,
            raw_text=ocr_text,
            created_at=_now_iso(),
            s3_key=s3_key,
            key_information=[],
            # 명세 1.4: OCR 이 비었거나 비정보성이면 LLM 단계 결과가 EMPTY 다.
            status="EMPTY",
        )
    except Exception as e:
        logger.warning("검색 색인 실패 imageId=%s: %s", image_id, e)

    job_store.update(job_id, "EMPTY", result=result, stage="INDEXING")


async def run_app_analysis(
    job_id: int, image_id: int, user_id: int, s3_key: str, ocr_text: str
) -> None:
    """한 번의 요청으로 정보성 판정 → 이미지 분석 → AGENT → 색인까지 수행한다.

    Spring 콜백 대신 결과를 job_store 에 담아 두고, 앱이 폴링으로 가져간다.
    동시 실행 제한을 함께 적용하므로 수백 장이 몰려도 순차적으로 소화된다.
    """
    queued_at = time.perf_counter()
    async with _get_semaphore():
        timings: dict[str, float] = {"WAIT": round(time.perf_counter() - queued_at, 2)}
        started = time.perf_counter()
        job_store.update(job_id, "PROCESSING", stage="LLM")
        # 색인 문서의 status 도 함께 올려 6-1 목록에서 진행 중임이 보이게 한다.
        try:
            await asyncio.to_thread(search.set_status, image_id, "PROCESSING")
        except Exception as e:
            logger.warning("status 갱신 실패 imageId=%s: %s", image_id, e)
        logger.info("앱 분석 시작 jobId=%s imageId=%s", job_id, image_id)
        try:
            # 0) 썸네일 생성 후 썸네일 버킷에 저장.
            #    분석을 건너뛰는 이미지(빈 OCR·비정보성)도 목록에는 나오므로
            #    정보성 판정보다 먼저 만든다. 여기서 읽은 원본은 이미지 분석에서 재사용한다.
            #    실패해도 분석은 계속한다(썸네일 조회 시 즉석 생성으로 메꿔진다).
            original: bytes | None = None
            try:
                with _timed(timings, "THUMBNAIL"):
                    original = await asyncio.to_thread(storage.fetch_image, s3_key)
                    await asyncio.to_thread(storage.store_thumbnail, s3_key, original)
            except Exception as e:
                logger.warning("썸네일 저장 실패 imageId=%s: %s (분석은 계속)", image_id, e)

            # 1) 저장할 가치가 있는 정보인지 판정한다.
            #    명세 10-4 처리 규칙: OCR 이 비었거나(EMPTY) 정보성이 없으면
            #    IMAGE_ANALYSIS·AGENT 를 생략하고 '기타' 로 분류한 뒤 색인한다.
            #    분석을 건너뛸 뿐 사용자 이미지는 목록·검색에서 사라지지 않는다.
            if not ocr_text.strip():
                logger.info("OCR 비어 있음 → '기타' 분류 후 색인 jobId=%s", job_id)
                with _timed(timings, "INDEXING"):
                    await _index_as_other(job_id, image_id, user_id, s3_key, ocr_text)
                timings["TOTAL"] = round(time.perf_counter() - started, 2)
                logger.info("앱 분석 완료(EMPTY) jobId=%s 소요=%s", job_id, timings)
                return

            with _timed(timings, "LLM"):
                _, verdict = await asyncio.to_thread(run_llm, ocr_text)
            if not verdict.get("informative", False):
                logger.info("비정보성 판정 → '기타' 분류 후 색인 jobId=%s 사유=%s",
                            job_id, verdict.get("reason", ""))
                with _timed(timings, "INDEXING"):
                    await _index_as_other(job_id, image_id, user_id, s3_key, ocr_text)
                timings["TOTAL"] = round(time.perf_counter() - started, 2)
                logger.info("앱 분석 완료(비정보성) jobId=%s 소요=%s", job_id, timings)
                return

            # 2) 이미지 분석. 실패해도 OCR 만으로 계속 진행한다.
            job_store.update(job_id, "PROCESSING", stage="IMAGE_ANALYSIS")
            try:
                with _timed(timings, "IMAGE_ANALYSIS"):
                    analysis = await asyncio.to_thread(
                        run_image_analysis, s3_key, original
                    )
            except Exception as e:
                logger.warning("이미지 분석 실패 imageId=%s: %s — OCR 만으로 진행",
                               image_id, e)
                analysis = {}

            # 3) 메타데이터 생성 + 카테고리 판정
            job_store.update(job_id, "PROCESSING", stage="AGENT")
            with _timed(timings, "AGENT"):
                generated = await run_agent_core(
                    user_id=user_id,
                    image_id=image_id,
                    ocr_text=ocr_text,
                    analysis_dict=analysis,
                )
            categories = generated.get("categories") or []
            category = categories[0] if categories else None
            result = {
                "title": generated.get("title", ""),
                "summary": generated.get("summary", ""),
                "tags": generated.get("tags", []),
                "category": category,
                "key_information": generated.get("keyInformation") or [],
                "confidence": float(generated.get("analysisConfidence", 0.0)),
                "s3_key": s3_key,
            }

            # 4) 검색 색인. 실패해도 분석 결과 자체는 살린다.
            job_store.update(job_id, "PROCESSING", stage="INDEXING")
            try:
                with _timed(timings, "INDEXING"):
                    await asyncio.to_thread(
                        search.index_document,
                        image_id=image_id,
                        user_id=user_id,
                        title=result["title"],
                        summary=result["summary"],
                        tags=result["tags"],
                        category_name=category,
                        raw_text=ocr_text,
                        created_at=_now_iso(),
                        s3_key=s3_key,
                        key_information=result["key_information"],
                        status="COMPLETED",
                        category_confidence=generated.get("categoryConfidence"),
                    )
            except Exception as e:
                logger.warning("검색 색인 실패 imageId=%s: %s (분석 결과는 유지)",
                               image_id, e)

            job_store.update(job_id, "COMPLETED", result=result, stage="INDEXING")
            timings["TOTAL"] = round(time.perf_counter() - started, 2)
            logger.info("앱 분석 완료 jobId=%s imageId=%s category=%s 소요=%s",
                        job_id, image_id, category, timings)
        except Exception as e:
            timings["TOTAL"] = round(time.perf_counter() - started, 2)
            logger.exception("앱 분석 실패 jobId=%s 소요=%s", job_id, timings)
            try:
                await asyncio.to_thread(search.set_status, image_id, "FAILED")
            except Exception:
                pass
            job_store.update(
                job_id,
                "FAILED",
                error={"code": "ANALYSIS_FAILED",
                       "message": str(e)[:500],
                       "retryable": True},
            )


# ── 단계 실행 + 콜백 ──────────────────────────────────────────────────────
async def execute_stage(request: AnalyzeRequest) -> None:
    # 동시 실행 제한: 수백 장이 한꺼번에 들어와도 세마포어가 꽉 차면 여기서 대기한다.
    # 초과분은 버려지지 않고 자연스럽게 큐잉되므로 500장+ 도 순차적으로 소화된다.
    async with _get_semaphore():
        await _execute_stage(request)


async def _execute_stage(request: AnalyzeRequest) -> None:
    settings = get_settings()
    stage = request.stage
    status = "FAILED"
    result: dict[str, Any] | None = None
    error: CallbackError | None = None
    model_version = settings.llm_model_name

    logger.info("단계 시작 jobId=%s imageId=%s stage=%s",
                request.job_id, request.image_id, stage)
    try:
        if stage == "LLM":
            ocr = request.input.ocr
            status, result = await asyncio.to_thread(
                run_llm, (ocr.raw_text if ocr else "")
            )
        elif stage == "IMAGE_ANALYSIS":
            model_version = settings.vision_model_name
            result = await asyncio.to_thread(
                run_image_analysis, request.input.image.s3_key  # type: ignore[union-attr]
            )
            status = "COMPLETED"
        else:  # AGENT
            result = await run_agent(request)
            status = "COMPLETED"
            await _index_for_search(request, result)
    except Exception as e:
        logger.exception("단계 실패 jobId=%s stage=%s", request.job_id, stage)
        status, result = "FAILED", None
        error = CallbackError(
            code=f"{stage}_FAILED",
            message=str(e)[:500],
            retryable=True,
        )

    job_registry.mark(request.job_id, stage, status)
    logger.info("단계 종료 jobId=%s imageId=%s stage=%s status=%s",
                request.job_id, request.image_id, stage, status)

    payload = CallbackRequest(
        job_id=request.job_id,
        image_id=request.image_id,
        stage=stage,
        status=status,  # type: ignore[arg-type]
        result=result,
        error=error,
        model_version=model_version,
        completed_at=_now_iso(),
    )
    await spring_client.post_callback(payload, request.callback_url)
