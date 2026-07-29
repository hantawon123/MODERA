"""단계별 분석 실행 (명세 10-1) 과 결과 콜백 (명세 10-4).

프로토타입의 통짜 run_pipeline 을 stage 단위로 분리했다.
한 요청은 LLM / IMAGE_ANALYSIS / AGENT 중 하나만 실행하고, 다음 단계로
넘길지는 Spring 이 결정한다.
"""

import asyncio
import logging
import time
from contextlib import contextmanager
from datetime import datetime, timedelta, timezone
from typing import Any

from . import gemini_client, search, spring_client, storage
from .category import CategoryResolution, normalize_name, resolve_category
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


def seed_default_category_vectors() -> int:
    """기본 카테고리 이름 벡터를 전역 시드로 심는다(멱등). 심은 개수를 돌려준다.

    이미지가 0장인 카테고리는 centroid 를 만들 수 없어 이름 임베딩으로 대신하는데,
    그 값은 사용자와 무관하게 같다. 그래서 사용자별로 복제하지 않고 user_id=0
    문서 17개로 한 번만 저장한다.

    기동마다 부르지만 이미 있는 시드는 다시 임베딩하지 않는다 — 조회 1회로 빠진
    이름만 골라 배치로 임베딩한다. 임베딩 모델·차원을 바꾸면 조회 필터가 기존
    시드를 무효로 보므로 자동으로 전량 재생성된다.

    실패해도 예외를 올리지 않는다. 시드가 없으면 콜드 스타트 판정이 이름
    완전일치로만 동작하고(품질만 떨어진다), 다음 기동에서 다시 시도한다.
    """
    try:
        existing = search.load_category_vectors(search.SEED_USER_ID)
        missing = [n for n in DEFAULT_CATEGORIES if normalize_name(n) not in existing]
        if not missing:
            logger.info("카테고리 시드 %s건 이미 존재 — 임베딩 생략", len(existing))
            return 0
        _, vectors = gemini_client.embed(missing, "DOCUMENT")
        written = search.put_seed_category_vectors(dict(zip(missing, vectors)))
        logger.info("카테고리 시드 %s건 생성 (기존 %s건)", written, len(existing))
        return written
    except Exception as e:
        logger.warning("카테고리 시드 준비 실패: %s — 콜드 스타트는 이름 일치로 진행", e)
        return 0


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
#
# ⚠️ asyncio.Semaphore 는 재진입이 안 된다. 같은 태스크가 두 번 잡으면 자기 자신이
#    놓아주기를 기다리며 영원히 멈춘다. 그래서 **진입점에서만** 잡는다:
#      - execute_stage        (/internal/v1/analyze → 단계 실행, FULL 포함)
#      - run_app_analysis     (/api/v1/.../upload-complete → 앱 직결 전체 분석)
#    실제 파이프라인 본체인 _run_full_pipeline 은 절대 잡지 않는다.
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
    # 캘린더용 일정 추출(schedule). 상대 날짜는 기준 시각(서버 현재 KST)으로 해석한다.
    # 스크린샷 캡처 시각이 요청에 없어 분석 시각을 기준으로 쓴다(대부분 일정은 절대 날짜라 영향 작음).
    ref_now = datetime.now(timezone(timedelta(hours=9))).strftime("%Y-%m-%dT%H:%M:%S+09:00")
    schedule_rule = (
        "[일정] 예약·티켓·행사·마감처럼 캘린더에 올릴 일정이 화면에 있으면 schedule 로 뽑아라.\n"
        "- startAt: 행사 시작 시각. endAt: 행사 종료 시각. **마감 기한('~까지')은 endAt 에 넣고 startAt 은 null 로 둬라.**\n"
        f"- ISO-8601(KST). 시각을 알면 '2026-08-03T14:30:00+09:00', 날짜만 알면 '2026-08-03'. 상대 날짜(내일·이번 주 금요일 등)는 기준 시각 {ref_now} 로 계산.\n"
        "- 연도가 없으면 기준 시각 기준 가장 가까운 미래로. 확인 안 된 값은 null(추측 금지). 일정이 전혀 없으면 둘 다 null.\n\n"
    )
    prompt = (
        "OCR 텍스트와 이미지 분석 결과를 종합해 개인 지식 DB용 메타데이터를 생성하라.\n\n"
        "[카테고리]\n"
        f"기존 카테고리 후보: {candidate_names}. 가능한 한 이 중에서 정확히 하나를 고르고, "
        "정말로 맞는 것이 없을 때만 새 카테고리 이름을 제안하라. "
        "categories 에는 최종 카테고리명 하나만 넣어라.\n\n"
        + tag_rule
        + "[주요정보] 사용자에게 보여줄 핵심 정보를 '항목: 값' 형태 문자열로 담아라. "
        "확인되지 않은 값은 넣지 마라(추측 금지).\n\n"
        + schedule_rule
        + language_rule
        + "반드시 아래 JSON만 출력. 마크다운·설명 금지.\n"
        '{"title":"...","summary":"...","tags":["..."],"categories":["..."],'
        '"key_information":["..."],"analysis_confidence":0.0,'
        '"schedule":{"startAt":null,"endAt":null}}\n\n'
        f"OCR: {ocr_text}\n"
        f"이미지 분석: {image_analysis}"
    )
    return gemini_client.generate_json(settings.llm_model_name, [prompt])


def _split_dt(value: str | None) -> dict[str, Any] | None:
    """ISO-8601 문자열(날짜 또는 날짜+시각)을 연·월·일 정수와 시각(HH:MM) 문자열로 분해한다.

    시각이 없는 날짜('2026-08-03')는 time 을 null 로 둔다(종일). 빈 값·파싱 불가면 None.
    타임존 표기(+09:00)는 무시하고 벽시계 시각 그대로 쓴다(계약이 KST 로컬).
    """
    s = (value or "").strip()
    if len(s) < 10:
        return None
    try:
        year, month, day = (int(x) for x in s[:10].split("-"))
    except ValueError:
        return None
    time = None
    if "T" in s:
        hm = s.split("T", 1)[1][:5]  # 'HH:MM'
        if len(hm) == 5 and hm[2] == ":" and hm[:2].isdigit() and hm[3:].isdigit():
            time = hm
    return {"year": year, "month": month, "day": day, "time": time}


def _build_schedule_data(raw: Any) -> dict[str, Any]:
    """AGENT 가 뽑은 일정(startAt·endAt ISO)을 콜백 scheduleData 계약 형태로 만든다.

    일정이 있으면 {"type":"schedule","fields":{startYear,startMonth,startDay,startTime,
    endYear,endMonth,endDay,endTime}}, 없으면 {"type": None, "fields": {}}.
    시각을 모르는 날짜는 startTime/endTime 이 null(종일).

    백엔드 계약: **시점이 하나뿐인 일정(예약·마감 등)은 end* 에 담는다.**
    start* 는 기간 일정의 시작에만 쓴다 — 단일 일정은 end만 보면 되도록 통일.
    """
    if not isinstance(raw, dict):
        return {"type": None, "fields": {}}
    start = _split_dt(raw.get("startAt"))
    end = _split_dt(raw.get("endAt"))
    if not start and not end:
        return {"type": None, "fields": {}}
    if start and not end:
        # 예약처럼 시작 시점만 뽑힌 경우 → 단일 시점 규칙에 따라 end 로 옮긴다.
        start, end = None, start
    fields: dict[str, Any] = {}
    for prefix, comp in (("start", start), ("end", end)):
        fields[prefix + "Year"] = comp["year"] if comp else None
        fields[prefix + "Month"] = comp["month"] if comp else None
        fields[prefix + "Day"] = comp["day"] if comp else None
        fields[prefix + "Time"] = comp["time"] if comp else None
    return {"type": "schedule", "fields": fields}


def _build_candidates(
    user_id: int, candidates: list[CategoryCandidate]
) -> list[CategoryCandidate]:
    """판정에 쓸 카테고리 후보를 모은다. 세 소스를 합친다.

    - Spring 10-5 knowledge.categories — categoryId 를 가진 유일한 소스
    - 이 서버의 카테고리 벡터 저장소 — 사용자 centroid + 전역 시드. AGENT 가 새로
      만든 카테고리도 여기 남으므로 Spring 미연동 모드에서도, 재기동 뒤에도 유지된다
    - 둘 다 비면 DEFAULT_CATEGORIES (시드도 없는 최악의 경우. categoryId 없음)

    대표 벡터는 Spring 의 representativeVector 가 우선, 없으면 저장소 값을 채운다
    (저장소가 사용자 centroid > 전역 시드 순으로 이미 정리해 준다).
    """
    stored = search.load_category_vectors(user_id)
    merged = list(candidates)
    seen = {normalize_name(c.name) for c in merged}
    for key, entry in stored.items():
        if key not in seen:
            seen.add(key)
            merged.append(CategoryCandidate(category_id=None, name=entry["name"]))
    if not merged:
        merged = [CategoryCandidate(category_id=None, name=name)
                  for name in DEFAULT_CATEGORIES]
    for candidate in merged:
        if not candidate.representative_vector:
            entry = stored.get(normalize_name(candidate.name))
            if entry:
                candidate.representative_vector = entry["vector"]
    return merged


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
    # 저장된 카테고리 벡터를 읽어 합친다(OpenSearch 조회라 스레드로 돌린다).
    candidates = await asyncio.to_thread(
        _build_candidates, user_id, knowledge.categories
    )
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
    # 순수 계산이라 스레드로 안 넘긴다(외부 호출이 없다 — 벡터는 후보에 이미 붙어 있다).
    resolution: CategoryResolution = resolve_category(
        vectors[0], proposed, candidates, settings.similarity_threshold
    )
    logger.info(
        "카테고리 판정 imageId=%s 제안=%s → %s (유사도=%.3f, 기준=%s, 신규=%s)",
        image_id, proposed, resolution.name,
        resolution.similarity, resolution.matched_by, resolution.created,
    )

    # 판정된 카테고리의 대표 벡터를 이 요약 임베딩으로 갱신한다(없으면 생성).
    # 이 저장이 있어야 카테고리와 그 벡터가 서버 재기동 뒤에도 남고, 이미지가
    # 쌓일수록 centroid 가 정확해진다.
    await asyncio.to_thread(
        search.upsert_category_vector, user_id, resolution.name, vectors[0]
    )

    tags = [str(t) for t in (generated.get("tags") or [])][:max_tags]
    return {
        "title": generated.get("title", ""),
        "summary": summary,
        "tags": tags,
        # 카테고리는 항상 하나다(프롬프트가 하나만 고르게 하고 resolve_category 도
        # 하나를 돌려준다). 배열로 감싸면 Spring 이 없는 다중 카테고리를 처리해야 한다.
        "category": resolution.name,
        "keyInformation": generated.get("key_information") or [],
        "analysisConfidence": float(generated.get("analysis_confidence", 0.0)),
        # 캘린더용 일정. {type:"schedule", fields:{start*/end* 연·월·일·시각(HH:MM)}} 또는
        # 일정 없으면 {type:null, fields:{}}. 백엔드가 DB 저장 후 앱 캘린더로 서빙한다.
        "scheduleData": _build_schedule_data(generated.get("schedule")),
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
    try:
        await asyncio.to_thread(
            search.index_document,
            image_id=request.image_id,
            user_id=request.user_id,
            title=result.get("title", ""),
            summary=result.get("summary", ""),
            tags=result.get("tags", []),
            category_name=result.get("category"),
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


def _empty_callback_result(reason: str, thumbnail_key: str | None = None) -> dict[str, Any]:
    """분석을 건너뛴(EMPTY) 경우의 콜백 result.

    Spring 의 AnalysisCallbackService 는 EMPTY 도 성공으로 취급해 결과 행을 남기므로
    (`case "COMPLETED", "EMPTY" -> handleSuccess`), **키 구조를 COMPLETED 와 완전히
    동일하게** 맞추고 값만 비운다(2026-07-29 백엔드 요청 — 만들지 않은 값은 null).
    informative·ocrRefinedText 는 콜백에서 제외(정보성 여부는 status=EMPTY 로 구분).
    """
    return {
        "title": "",
        "summary": "",
        "tags": [],
        "category": OTHER_CATEGORY,
        "keyInformation": [],
        "analysisConfidence": 0.0,
        "scheduleData": {"type": None, "fields": {}},
        # '기타' 는 기본 카테고리라 신규 생성 판단이 필요 없다.
        "categoryCreated": False,
        "categoryConfidence": 0.0,
        # 요약이 없어 임베딩을 만들지 않는다 — 구조만 유지하고 null.
        "documentVector": None,
        "embeddingModel": None,
        "embeddingDimension": None,
        "thumbnailKey": thumbnail_key,
        "reason": reason,
    }


def _text_from_image_analysis(analysis: dict[str, Any]) -> str:
    """이미지 분석 결과에서 OCR 대용 텍스트를 만든다.

    analysis-worker 의 FULL 요청에는 OCR 이 실려 오지 않는다(FastApiAnalysisClient 가
    input 에 image 만 담는다). 그대로 두면 정보성 판정 입력이 항상 비어 EMPTY 로
    떨어지므로, 비전 모델이 화면에서 읽어낸 텍스트를 대신 쓴다.
    """
    parts = [analysis.get("description") or ""]
    parts += [str(t) for t in (analysis.get("detectedTexts") or [])]
    return "\n".join(p for p in parts if p.strip()).strip()


async def run_app_analysis(
    job_id: int, image_id: int, user_id: int, s3_key: str, ocr_text: str
) -> None:
    """앱 직결(4-2 업로드 완료) 진입점.

    결과는 콜백 대신 job_store 에 담기고 앱이 폴링으로 가져간다. 동시 실행 제한은
    여기서 잡는다 — 본체(_run_full_pipeline)는 잡지 않는다.
    """
    async with _get_semaphore():
        await _run_full_pipeline(job_id, image_id, user_id, s3_key, ocr_text)


async def _run_full_pipeline(
    job_id: int,
    image_id: int,
    user_id: int,
    s3_key: str,
    ocr_text: str,
    max_tags: int | None = None,
    language: str | None = None,
    allow_vision_ocr: bool = False,
) -> tuple[str, dict[str, Any] | None, CallbackError | None]:
    """정보성 판정 → 이미지 분석 → AGENT → 색인을 한 번에 수행한다.

    앱 직결 경로(run_app_analysis)와 Spring 연동의 FULL stage 가 함께 쓴다.
    `(status, result, error)` 를 돌려주며, status 는 COMPLETED / EMPTY / FAILED 다.
    result 는 명세 10-4 콜백 result 형태(camelCase)라 그대로 실어 보내면 된다.

    **세마포어를 잡지 않는다.** 호출자(run_app_analysis / execute_stage)가 이미
    잡고 있으며, 여기서 또 잡으면 재진입 데드락이 난다.

    allow_vision_ocr=True 면 OCR 텍스트가 없을 때 이미지 분석을 먼저 돌려 거기서
    읽어낸 텍스트로 진행한다(OCR 없이 오는 FULL 요청용). 앱 직결 경로는 온디바이스
    OCR 이 항상 함께 오므로 기본값 False 로 두어 기존 동작을 유지한다.
    """
    timings: dict[str, float] = {}
    started = time.perf_counter()
    job_store.update(job_id, "PROCESSING", stage="LLM")
    # 색인 문서의 status 도 함께 올려 6-1 목록에서 진행 중임이 보이게 한다.
    try:
        await asyncio.to_thread(search.set_status, image_id, "PROCESSING")
    except Exception as e:
        logger.warning("status 갱신 실패 imageId=%s: %s", image_id, e)
    logger.info("전체 분석 시작 jobId=%s imageId=%s", job_id, image_id)
    try:
        # 0) 썸네일 생성 후 썸네일 버킷에 저장.
        #    분석을 건너뛰는 이미지(빈 OCR·비정보성)도 목록에는 나오므로
        #    정보성 판정보다 먼저 만든다. 여기서 읽은 원본은 이미지 분석에서 재사용한다.
        #    실패해도 분석은 계속한다(썸네일 조회 시 즉석 생성으로 메꿔진다).
        original: bytes | None = None
        # 썸네일은 원본과 같은 key 로 썸네일 버킷에 저장된다(관례). 성공 시 그 key 를
        # 콜백 thumbnailKey 로 알려 Spring 이 thumbnail 테이블(image_id, s3_key)을
        # 채울 수 있게 한다. 실패하면 null — Spring 은 썸네일 없음으로 처리한다.
        thumbnail_key: str | None = None
        try:
            with _timed(timings, "THUMBNAIL"):
                original = await asyncio.to_thread(storage.fetch_image, s3_key)
                await asyncio.to_thread(storage.store_thumbnail, s3_key, original)
            thumbnail_key = s3_key
        except Exception as e:
            logger.warning("썸네일 저장 실패 imageId=%s: %s (분석은 계속)", image_id, e)

        # 1) 저장할 가치가 있는 정보인지 판정한다.
        #    명세 10-4 처리 규칙: OCR 이 비었거나(EMPTY) 정보성이 없으면
        #    IMAGE_ANALYSIS·AGENT 를 생략하고 '기타' 로 분류한 뒤 색인한다.
        #    분석을 건너뛸 뿐 사용자 이미지는 목록·검색에서 사라지지 않는다.
        analysis: dict[str, Any] | None = None
        if not ocr_text.strip() and allow_vision_ocr:
            # OCR 이 없는 FULL 요청. 이미지 분석을 앞당겨 읽어낸 텍스트로 대신한다.
            job_store.update(job_id, "PROCESSING", stage="IMAGE_ANALYSIS")
            try:
                with _timed(timings, "IMAGE_ANALYSIS"):
                    analysis = await asyncio.to_thread(
                        run_image_analysis, s3_key, original
                    )
                ocr_text = _text_from_image_analysis(analysis)
            except Exception as e:
                logger.warning("이미지 분석 실패 imageId=%s: %s — OCR 대체 불가",
                               image_id, e)
                analysis = {}

        if not ocr_text.strip():
            logger.info("읽어낼 텍스트 없음 → '기타' 분류 후 색인 jobId=%s", job_id)
            with _timed(timings, "INDEXING"):
                await _index_as_other(job_id, image_id, user_id, s3_key, ocr_text)
            timings["TOTAL"] = round(time.perf_counter() - started, 2)
            logger.info("전체 분석 완료(EMPTY) jobId=%s 소요=%s", job_id, timings)
            return "EMPTY", _empty_callback_result("텍스트 없음", thumbnail_key), None

        with _timed(timings, "LLM"):
            _, verdict = await asyncio.to_thread(run_llm, ocr_text)
        if not verdict.get("informative", False):
            reason = verdict.get("reason", "")
            logger.info("비정보성 판정 → '기타' 분류 후 색인 jobId=%s 사유=%s",
                        job_id, reason)
            with _timed(timings, "INDEXING"):
                await _index_as_other(job_id, image_id, user_id, s3_key, ocr_text)
            timings["TOTAL"] = round(time.perf_counter() - started, 2)
            logger.info("전체 분석 완료(비정보성) jobId=%s 소요=%s", job_id, timings)
            return "EMPTY", _empty_callback_result(reason, thumbnail_key), None

        # 2) 이미지 분석. 실패해도 OCR 만으로 계속 진행한다.
        #    위에서 이미 돌렸으면(OCR 대체) 다시 부르지 않는다.
        if analysis is None:
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
                max_tags=max_tags,
                language=language,
            )
        category = generated.get("category")
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
        logger.info("전체 분석 완료 jobId=%s imageId=%s category=%s 소요=%s",
                    job_id, image_id, category, timings)

        # 콜백 result. Spring 의 AnalysisCallbackService 가 읽는 키를 이 이름으로 담는다.
        # informative·ocrRefinedText 는 싣지 않는다(2026-07-29 백엔드 합의) —
        # 정보성 여부는 status(COMPLETED/EMPTY)로 구분되고, OCR 원문은 앱이 4-1 로
        # 보낸 것을 Spring 이 자체 보관(image_schema.ocr.content)한다.
        callback_result = dict(generated)
        callback_result["thumbnailKey"] = thumbnail_key
        return "COMPLETED", callback_result, None
    except Exception as e:
        timings["TOTAL"] = round(time.perf_counter() - started, 2)
        logger.exception("전체 분석 실패 jobId=%s 소요=%s", job_id, timings)
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
        return "FAILED", None, CallbackError(
            code="ANALYSIS_FAILED", message=str(e)[:500], retryable=True
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
        elif stage == "FULL":
            # 세마포어는 execute_stage 가 이미 잡고 있다. 본체를 직접 부른다.
            ocr = request.input.ocr
            ocr_text = ((ocr.refined_text or ocr.raw_text) if ocr else "") or ""
            options = request.options
            status, result, error = await _run_full_pipeline(
                job_id=request.job_id,
                image_id=request.image_id,
                user_id=request.user_id,
                s3_key=request.input.image.s3_key,  # type: ignore[union-attr]
                ocr_text=ocr_text,
                max_tags=options.max_tags if options else None,
                language=options.language if options else None,
                allow_vision_ocr=True,
            )
            # 색인은 _run_full_pipeline 안에서 이미 끝냈다(_index_for_search 중복 호출 금지).
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
