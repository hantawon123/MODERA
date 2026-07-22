"""단계별 분석 실행 (명세 10-1) 과 결과 콜백 (명세 10-4).

프로토타입의 통짜 run_pipeline 을 stage 단위로 분리했다.
한 요청은 LLM / IMAGE_ANALYSIS / AGENT 중 하나만 실행하고, 다음 단계로
넘길지는 Spring 이 결정한다.
"""

import asyncio
import logging
from datetime import datetime, timezone
from typing import Any

from . import gemini_client, spring_client, storage
from .category import CategoryResolution, resolve_category
from .config import get_settings
from .jobs import job_registry
from .schemas import (
    AnalyzeRequest,
    CallbackError,
    CallbackRequest,
    CategoryCandidate,
)

logger = logging.getLogger(__name__)

# Spring 에 사용자 카테고리가 하나도 없을 때 쓰는 초기 후보.
# 원칙적으로는 Spring DB 에 시드로 넣고 10-5 로 내려받는 편이 낫다.
DEFAULT_CATEGORIES = [
    "쇼핑", "음식·맛집", "여행", "예약·예매", "쿠폰·할인", "금융·재테크",
    "뷰티·미용", "학습·공부", "채용·취업", "IT·개발", "뉴스·시사", "부동산",
    "건강·운동", "엔터·콘텐츠", "자동차", "반려동물", "기타",
]


def _now_iso() -> str:
    return datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%S.%f")[:-3] + "Z"


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
def run_image_analysis(s3_key: str) -> dict[str, Any]:
    settings = get_settings()
    image_bytes = storage.fetch_image_bytes(s3_key)
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
) -> dict[str, Any]:
    settings = get_settings()
    tag_rule = (
        f"[태그] 핵심 키워드 위주로 최대 {max_tags}개, 중복·과도한 일반 태그 제외.\n\n"
        if max_tags else "[태그] 중복·과도한 일반 태그 제외.\n\n"
    )
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
    # 사용자 카테고리가 아직 없으면 기본 후보로 시작한다(categoryId 없음 = 미저장).
    return [CategoryCandidate(category_id=None, name=name) for name in DEFAULT_CATEGORIES]


async def run_agent(request: AnalyzeRequest) -> dict[str, Any]:
    settings = get_settings()
    ocr = request.input.ocr
    image_analysis = request.input.image_analysis
    ocr_text = (ocr.refined_text or ocr.raw_text) if ocr else ""
    analysis_dict = image_analysis.model_dump(by_alias=True) if image_analysis else {}

    # 10-5 기존 태그·카테고리 후보 조회
    knowledge = await spring_client.fetch_knowledge_candidates(request.user_id)
    candidates = _build_candidates(knowledge.categories)

    options = request.options
    max_tags = (options.max_tags if options and options.max_tags else settings.default_max_tags)
    language = options.language if options else None

    generated = await asyncio.to_thread(
        run_agent_generation,
        ocr_text, analysis_dict, [c.name for c in candidates], max_tags, language,
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
        request.image_id, proposed, resolution.name,
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
        # 검색용 문서 임베딩. Spring 이 pgvector 등에 그대로 적재한다.
        # (요약 텍스트 기준. purpose=DOCUMENT 로 생성)
        "documentVector": vectors[0],
        "embeddingModel": embedding_model,
        "embeddingDimension": len(vectors[0]),
    }


# ── 단계 실행 + 콜백 ──────────────────────────────────────────────────────
async def execute_stage(request: AnalyzeRequest) -> None:
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
