"""FastAPI 내부 API 서버.

공통 응답 envelope 를 쓰지 않는 서비스 간 통신 전용이며,
에러는 {"error": CODE, "message": ..., "detail": {}} 형식으로 반환한다.
"""

import logging
import os
from typing import Annotated, Any

from fastapi import BackgroundTasks, Depends, FastAPI, Query, Request
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse, Response
from fastapi.security import APIKeyHeader

from . import gemini_client, responses, search, storage
from .config import get_settings
from .jobs import job_registry, job_store
from .schemas import (
    AnalysisJob,
    AnalysisSummary,
    AnalyzeAccepted,
    AnalyzeRequest,
    AppAnalyzeAccepted,
    AppAnalyzeRequest,
    CategoryCard,
    CategoryRef,
    EmbedRequest,
    EmbedResponse,
    EmbeddingItem,
    ImageDetail,
    ImageListItem,
    OcrInput,
    ParsedConditions,
    QueryParseRequest,
    QueryParseResponse,
    SearchHit,
    SearchRequest,
    SearchResponse,
    SearchResultItem,
    TagCount,
    TagItem,
    TagRef,
)
from .stages import execute_stage, run_app_analysis

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s %(levelname)s [%(name)s] %(message)s",
)
logger = logging.getLogger(__name__)

# 문서 UI 는 개발·팀 공유 편의를 위해 켜둔다. 외부에 노출되는 서버라면
# ENABLE_DOCS=false 로 꺼야 한다(엔드포인트 구조가 그대로 드러난다).
# get_settings() 는 자격증명이 없으면 예외를 던지므로 여기서는 환경변수를 직접 읽는다.
_docs_enabled = os.environ.get("ENABLE_DOCS", "true").lower() == "true"

app = FastAPI(
    title="Screenshot Knowledge AI",
    description=(
        "스크린샷 지식 DB의 AI 서비스.\n\n"
        "- `/api/v1/*` : 앱 직결 API (Spring 우회 구간)\n"
        "- `/internal/v1/*` : Spring 연동용 내부 API\n\n"
        "모든 요청에 `X-Internal-Token` 헤더가 필요하다. "
        "우측 상단 **Authorize** 에서 토큰을 넣으면 아래 예제들을 그대로 실행할 수 있다."
    ),
    version="1.0.0",
    docs_url="/docs" if _docs_enabled else None,
    redoc_url="/redoc" if _docs_enabled else None,
    openapi_url="/openapi.json" if _docs_enabled else None,
)


def _error(code: str, message: str, detail: Any = None, http_status: int = 400) -> JSONResponse:
    return JSONResponse(
        status_code=http_status,
        content={"error": code, "message": message, "detail": detail or {}},
    )


# APIKeyHeader 로 선언하면 Swagger 에 Authorize 버튼이 생겨 토큰을 넣고
# 문서에서 바로 호출해 볼 수 있다. 동작은 헤더 검사로 동일하다.
_internal_token_header = APIKeyHeader(
    name="X-Internal-Token",
    auto_error=False,
    description="서비스 간 공유 토큰. 팀에서 전달받은 값을 넣는다.",
)


async def require_internal_token(
    token: str | None = Depends(_internal_token_header),
) -> None:
    if token != get_settings().internal_token:
        raise PermissionError("내부 토큰 불일치")


@app.exception_handler(PermissionError)
async def _permission_handler(request: Request, exc: PermissionError) -> JSONResponse:
    return _error("UNAUTHORIZED", str(exc), http_status=401)


@app.exception_handler(RequestValidationError)
async def _validation_handler(request: Request, exc: RequestValidationError) -> JSONResponse:
    return _error("INVALID_REQUEST", "요청 값이 올바르지 않습니다.",
                  detail={"errors": exc.errors()[:5]}, http_status=400)


# ── 사용자 식별 ───────────────────────────────────────────────────────────
# MVP 는 회원가입·로그인이 없다. 요청이 보내는 userId 를 검증할 방법이 없으므로
# FIXED_USER_ID 가 설정돼 있으면 요청 값을 무시하고 그 값으로 고정한다.
# 파라미터 자체는 그대로 받아 두기 때문에 로그인이 붙으면 설정만 0 으로 바꾸면 된다.
class _MissingUserId(Exception):
    pass


@app.exception_handler(_MissingUserId)
async def _missing_user_id_handler(request: Request, exc: _MissingUserId) -> JSONResponse:
    return responses.failure("INVALID_PARAMETER", "요청 값이 올바르지 않습니다.",
                             [{"field": "userId", "message": "필수 값입니다."}])


def resolve_user_id(requested: int | None) -> int:
    fixed = get_settings().fixed_user_id
    if fixed:
        return fixed
    if requested is None:
        raise _MissingUserId()
    return requested


async def _current_user_id(
    user_id: int | None = Query(
        None,
        alias="userId",
        description="로그인 미구현 구간에서는 FIXED_USER_ID 로 고정되며 이 값은 무시된다.",
    ),
) -> int:
    return resolve_user_id(user_id)


CurrentUserId = Annotated[int, Depends(_current_user_id)]


@app.get("/health")
async def health() -> dict[str, str]:
    return {"status": "ok"}


# ── 10-1 단계별 분석 실행 요청 ────────────────────────────────────────────
_REQUIRED_INPUT = {
    "LLM": ("ocr",),
    "IMAGE_ANALYSIS": ("image",),
    "AGENT": ("ocr", "image_analysis"),
}


@app.post("/internal/v1/analyze", dependencies=[Depends(require_internal_token)])
async def analyze(request: AnalyzeRequest, background_tasks: BackgroundTasks):
    missing = [
        field for field in _REQUIRED_INPUT[request.stage]
        if getattr(request.input, field, None) is None
    ]
    if missing:
        return _error("INVALID_REQUEST",
                      f"{request.stage} 단계에 필요한 input 이 없습니다.",
                      detail={"missing": missing}, http_status=400)

    if not job_registry.try_claim(request.job_id, request.stage):
        # 멱등 처리: 이미 처리 중이거나 완료된 작업 (명세상 HTTP 200)
        return JSONResponse(
            status_code=200,
            content={"error": "DUPLICATE_JOB",
                     "message": "동일 작업을 이미 처리 중이거나 완료했습니다.",
                     "detail": {"jobId": request.job_id, "stage": request.stage}},
        )

    background_tasks.add_task(execute_stage, request)
    accepted = AnalyzeAccepted(
        job_id=request.job_id, image_id=request.image_id,
        stage=request.stage, accepted=True, status="QUEUED",
    )
    return JSONResponse(status_code=202,
                        content=accepted.model_dump(by_alias=True))


# ── 10-2 텍스트 임베딩 생성 ───────────────────────────────────────────────
@app.post("/internal/v1/embed", dependencies=[Depends(require_internal_token)])
async def embed(request: EmbedRequest):
    if not request.texts:
        return _error("INVALID_REQUEST", "texts 가 비어 있습니다.", http_status=400)
    try:
        import asyncio
        model_name, vectors = await asyncio.to_thread(
            gemini_client.embed, request.texts, request.purpose
        )
    except Exception as e:
        logger.exception("임베딩 실패")
        return _error("EMBEDDING_FAILED", str(e)[:500], http_status=502)

    response = EmbedResponse(
        model=model_name,
        model_version=model_name,
        dimension=len(vectors[0]) if vectors else 0,
        embeddings=[EmbeddingItem(index=i, vector=v) for i, v in enumerate(vectors)],
    )
    return JSONResponse(status_code=200, content=response.model_dump(by_alias=True))


# ── 10-3 자연어 → 구조화 조건 변환 ────────────────────────────────────────
@app.post("/internal/v1/query/parse", dependencies=[Depends(require_internal_token)])
async def query_parse(request: QueryParseRequest):
    settings = get_settings()
    prompt = (
        "사용자의 자연어 검색어를 구조화된 검색 조건으로 변환하라. "
        "확인되지 않는 값은 null 로 두고 추측하지 마라.\n"
        f"상대 날짜 해석 기준 시각: {request.now or '서버 현재 시각'}\n"
        "반드시 아래 JSON만 출력. 마크다운·설명 금지.\n"
        '{"keywords":["..."],"price_min":null,"price_max":null,"brand":null,'
        '"category_hints":["..."],"date_from":null,"date_to":null,'
        '"expires_before":null,"confidence":0.0}\n\n'
        f"검색어: {request.query}"
    )
    try:
        import asyncio
        parsed = await asyncio.to_thread(
            gemini_client.generate_json, settings.llm_model_name, [prompt]
        )
        conditions = ParsedConditions.model_validate(
            {k: v for k, v in parsed.items() if k != "confidence"}
        )
        response = QueryParseResponse(
            model_version=settings.llm_model_name,
            parsed_conditions=conditions,
            confidence=float(parsed.get("confidence", 0.0)),
        )
    except Exception as e:
        # 완전 실패도 에러 코드로 올리지 않고 parsedConditions=null 로 degrade
        logger.warning("조건 변환 실패: %s", e)
        response = QueryParseResponse(
            model_version=settings.llm_model_name,
            parsed_conditions=None,
            confidence=0.0,
        )
    return JSONResponse(status_code=200, content=response.model_dump(by_alias=True))


# ── 키워드 검색 (OpenSearch BM25) ─────────────────────────────────────────
@app.post("/internal/v1/search", dependencies=[Depends(require_internal_token)])
async def keyword_search(request: SearchRequest):
    if not request.query or not request.query.strip():
        return _error("INVALID_REQUEST", "query 가 비어 있습니다.", http_status=400)
    try:
        import asyncio
        hits, total = await asyncio.to_thread(
            search.keyword_search,
            request.user_id, request.query, request.category, request.size,
        )
    except Exception as e:
        logger.exception("검색 실패")
        return _error("SEARCH_FAILED", str(e)[:500], http_status=502)

    response = SearchResponse(total=total, hits=[SearchHit(**h) for h in hits])
    return JSONResponse(status_code=200, content=response.model_dump(by_alias=True))


# ─────────────────────────────────────────────────────────────────────────
# 앱 API (Spring 우회 구간)
#
# 팀 API 명세의 외부 API 규약을 따른다: 공통 envelope, 페이지 형식, 에러 코드.
# Spring 이 복귀하면 앱은 호출 대상만 Spring 으로 바꾸면 되도록 응답 구조를 맞춰 둔다.
# 이 서비스가 채울 수 없는 값(favorite, fileName, structuredData 등)은
# 필드를 유지한 채 null 로 내려보낸다.
# ─────────────────────────────────────────────────────────────────────────


def _tag_refs(names: list[str]) -> list[TagRef]:
    return [TagRef(tag_id=search.stable_id(n), name=n) for n in names or []]


def _category_refs(name: str | None) -> list[CategoryRef]:
    if not name:
        return []
    return [CategoryRef(category_id=search.stable_id(name), name=name)]


def _thumbnail_url(image_id: int) -> str:
    """앱이 쓸 썸네일 주소. 만료가 없는 고정 경로다."""
    return f"/api/v1/images/{image_id}/thumbnail"


def _to_list_item(img: dict[str, Any]) -> ImageListItem:
    return ImageListItem(
        image_id=img["image_id"],
        title=img.get("title", ""),
        summary=img.get("summary", ""),
        status="COMPLETED",
        thumbnail_url=_thumbnail_url(img["image_id"]),
        tags=_tag_refs(img.get("tags") or []),
        categories=_category_refs(img.get("category")),
        created_at=img.get("created_at"),
    )


async def _resolve_filters(
    user_id: int, category_id: int | None, tag_id: int | None
) -> tuple[str | None, str | None]:
    """categoryId·tagId 를 실제 이름으로 되돌린다. 없는 ID 면 None 이 된다."""
    import asyncio

    category = tag = None
    if category_id is not None:
        category = await asyncio.to_thread(
            search.resolve_name_by_id, user_id, "category_name", category_id
        )
    if tag_id is not None:
        tag = await asyncio.to_thread(
            search.resolve_name_by_id, user_id, "tags.keyword", tag_id
        )
    return category, tag


# ── 분석 요청 (임시: Spring 복귀 시 4-1~4-3 으로 대체) ────────────────────
@app.post("/api/v1/analyze", dependencies=[Depends(require_internal_token)])
async def app_analyze(request: AppAnalyzeRequest, background_tasks: BackgroundTasks):
    if not request.s3_key.strip():
        return responses.failure("INVALID_PARAMETER", "요청 값이 올바르지 않습니다.",
                                 [{"field": "s3Key", "message": "필수 값입니다."}])
    # rawText 가 비어 있어도 거부하지 않는다. 명세 4-3 에 따라 LLM 을 실행하지 않고
    # '기타' 로 분류한 뒤 색인하는 흐름으로 넘어간다.
    ocr_text = (request.ocr.refined_text or request.ocr.raw_text or "").strip()

    # 로그인 미구현 구간이라 본문의 userId 도 신뢰하지 않고 고정값으로 덮어쓴다.
    # 조회 API 와 같은 값을 써야 분석한 이미지가 목록·검색에 나타난다.
    user_id = resolve_user_id(request.user_id)
    job = job_store.create(user_id, request.s3_key, request.image_id)
    background_tasks.add_task(
        run_app_analysis,
        job["job_id"], job["image_id"], user_id, request.s3_key, ocr_text,
    )
    accepted = AppAnalyzeAccepted(
        image_id=job["image_id"], job_id=job["job_id"],
        stage=job["stage"], status=job["status"],
    )
    return responses.success(accepted.model_dump(by_alias=True),
                             "분석 요청이 접수되었습니다.", http_status=202)


# ── 5-1 분석 현황 요약 ────────────────────────────────────────────────────
@app.get("/api/v1/analysis/summary", dependencies=[Depends(require_internal_token)])
async def app_analysis_summary(user_id: CurrentUserId):
    jobs = job_store.list_by_user(user_id)

    stage_counts: dict[str, dict[str, int]] = {}
    overall = {"QUEUED": 0, "PROCESSING": 0, "COMPLETED": 0,
               "FAILED": 0, "EMPTY": 0, "CANCELED": 0}
    for job in jobs:
        stage, status = job["stage"], job["status"]
        stage_counts.setdefault(stage, {})
        stage_counts[stage][status] = stage_counts[stage].get(status, 0) + 1
        if status in overall:
            overall[status] += 1

    summary = AnalysisSummary(
        total=len(jobs), stage_counts=stage_counts, overall_counts=overall
    )
    return responses.success(summary.model_dump(by_alias=True))


# ── 5-6 분석 작업 목록 ────────────────────────────────────────────────────
@app.get("/api/v1/analysis/jobs", dependencies=[Depends(require_internal_token)])
async def app_analysis_jobs(
    user_id: CurrentUserId,
    status: str | None = Query(None),
    page: int = Query(0),
    size: int = Query(20),
):
    jobs = job_store.list_by_user(user_id)
    if status:
        wanted = {s.strip().upper() for s in status.split(",") if s.strip()}
        jobs = [j for j in jobs if j["status"] in wanted]

    # 최근 갱신 순으로 보여 준다.
    jobs.sort(key=lambda j: j.get("updated_at") or "", reverse=True)
    total = len(jobs)
    window = jobs[max(0, page) * size: max(0, page) * size + size]

    items = [
        AnalysisJob(
            job_id=j["job_id"], image_id=j["image_id"],
            stage=j["stage"], status=j["status"],
            retryable=bool(j.get("error", {}) or {}) and
                      bool((j.get("error") or {}).get("retryable")),
            error_code=(j.get("error") or {}).get("code"),
            updated_at=j.get("updated_at"),
        ).model_dump(by_alias=True)
        for j in window
    ]
    return responses.success(responses.page_data(items, page, size, total))


# ── 6-1 이미지 목록 ───────────────────────────────────────────────────────
@app.get("/api/v1/images", dependencies=[Depends(require_internal_token)])
async def app_image_list(
    user_id: CurrentUserId,
    category_id: int | None = Query(None, alias="categoryId"),
    tag_id: int | None = Query(None, alias="tagId"),
    page: int = Query(0),
    size: int = Query(20),
):
    try:
        import asyncio
        category, tag = await _resolve_filters(user_id, category_id, tag_id)
        images, total = await asyncio.to_thread(
            search.list_images, user_id, page, size, category, tag
        )
    except Exception as e:
        logger.exception("이미지 목록 조회 실패")
        return responses.failure("INTERNAL_ERROR", "이미지 목록을 조회하지 못했습니다.",
                                 str(e)[:300], http_status=500)

    items = [_to_list_item(img).model_dump(by_alias=True) for img in images]
    return responses.success(responses.page_data(items, page, size, total))


# ── 6-2 이미지 상세 ───────────────────────────────────────────────────────
@app.get("/api/v1/images/{image_id}", dependencies=[Depends(require_internal_token)])
async def app_image_detail(image_id: int):
    try:
        import asyncio
        found = await asyncio.to_thread(search.get_image, image_id)
    except Exception as e:
        logger.exception("이미지 상세 조회 실패")
        return responses.failure("INTERNAL_ERROR", "이미지를 조회하지 못했습니다.",
                                 str(e)[:300], http_status=500)

    if found is None:
        return responses.failure("IMAGE_NOT_FOUND", "이미지를 찾을 수 없습니다.",
                                 f"imageId: {image_id}", http_status=404)

    detail = ImageDetail(
        image_id=found["image_id"],
        title=found.get("title", ""),
        summary=found.get("summary", ""),
        ocr=OcrInput(raw_text=found.get("raw_text", "")),
        tags=_tag_refs(found.get("tags") or []),
        categories=_category_refs(found.get("category")),
        key_information=found.get("key_information") or [],
        thumbnail_url=_thumbnail_url(found["image_id"]),
        created_at=found.get("created_at"),
        updated_at=found.get("created_at"),
    )
    return responses.success(detail.model_dump(by_alias=True))


# ── 6-6 썸네일 조회 ───────────────────────────────────────────────────────
@app.get("/api/v1/images/{image_id}/thumbnail",
         dependencies=[Depends(require_internal_token)])
async def app_image_thumbnail(image_id: int):
    """썸네일 버킷에 저장해 둔 JPEG 를 돌려준다.

    분석할 때 원본과 같은 key 로 썸네일 버킷에 올려 두므로 보통은 그것을 그대로 읽는다.
    없으면(분석 전 이미지, 저장 실패) 원본에서 즉석 생성해 내려주고 그때 버킷에도 채운다.

    presigned URL 을 저장했다가 돌려주면 만료로 깨지므로 주소는 이 경로로 고정한다.
    이 응답만 공통 envelope 를 쓰지 않는다.
    """
    import asyncio

    found = await asyncio.to_thread(search.get_image, image_id)
    if found is None:
        return responses.failure("IMAGE_NOT_FOUND", "이미지를 찾을 수 없습니다.",
                                 f"imageId: {image_id}", http_status=404)

    image_ref = found.get("s3_key")
    if not image_ref:
        return responses.failure("THUMBNAIL_NOT_FOUND", "썸네일을 만들 원본이 없습니다.",
                                 f"imageId: {image_id}", http_status=404)

    thumb: bytes | None = None
    if get_settings().s3_thumbnail_bucket:
        try:
            thumb = await asyncio.to_thread(storage.fetch_thumbnail, image_ref)
        except Exception as e:
            logger.info("저장된 썸네일 없음 imageId=%s: %s — 즉석 생성", image_id, e)

    if thumb is None:
        try:
            thumb = await asyncio.to_thread(storage.store_thumbnail, image_ref)
        except Exception as e:
            logger.warning("썸네일 생성 실패 imageId=%s: %s", image_id, e)
            return responses.failure("THUMBNAIL_NOT_FOUND", "썸네일을 가져오지 못했습니다.",
                                     str(e)[:200], http_status=404)

    return Response(content=thumb, media_type="image/jpeg",
                    headers={"Cache-Control": "public, max-age=86400"})


# ── 7-1 태그 목록 ─────────────────────────────────────────────────────────
@app.get("/api/v1/tags", dependencies=[Depends(require_internal_token)])
async def app_tag_list(
    user_id: CurrentUserId,
    q: str | None = Query(None),
    page: int = Query(0),
    size: int = Query(20),
):
    try:
        import asyncio
        items = await asyncio.to_thread(search.aggregate_tags, user_id, 500)
    except Exception as e:
        logger.exception("태그 조회 실패")
        return responses.failure("INTERNAL_ERROR", "태그를 조회하지 못했습니다.",
                                 str(e)[:300], http_status=500)

    if q:
        needle = q.strip().lower()
        items = [i for i in items if needle in i["name"].lower()]

    total = len(items)
    window = items[max(0, page) * size: max(0, page) * size + size]
    tags = [
        TagItem(tag_id=search.stable_id(i["name"]), name=i["name"],
                usage_count=i["count"]).model_dump(by_alias=True)
        for i in window
    ]
    return responses.success(responses.page_data(tags, page, size, total))


# ── 7-2 카테고리 목록 ─────────────────────────────────────────────────────
@app.get("/api/v1/categories", dependencies=[Depends(require_internal_token)])
async def app_category_list(
    user_id: CurrentUserId,
    page: int = Query(0),
    size: int = Query(20),
):
    try:
        import asyncio
        items = await asyncio.to_thread(search.aggregate_categories, user_id, 500)
    except Exception as e:
        logger.exception("카테고리 조회 실패")
        return responses.failure("INTERNAL_ERROR", "카테고리를 조회하지 못했습니다.",
                                 str(e)[:300], http_status=500)

    total = len(items)
    window = items[max(0, page) * size: max(0, page) * size + size]
    cards = [
        CategoryCard(
            category_id=search.stable_id(i["name"]),
            name=i["name"],
            # 이 카테고리에 가장 최근 분류된 사진의 썸네일을 그대로 가리킨다.
            # 카테고리 전용 이미지를 따로 만들지 않는다(사진마다 썸네일 1장).
            thumbnail_url=(_thumbnail_url(i["thumbnail_image_id"])
                           if i.get("thumbnail_image_id") else None),
            image_count=i["count"],
            # 이 카테고리 안에서 각 태그가 몇 장에 붙어 있는지까지 함께 내려준다.
            tags=[TagCount(tag_id=search.stable_id(t["name"]),
                           name=t["name"], image_count=t["count"])
                  for t in i.get("tags") or []],
            updated_at=i.get("last_updated_at"),
        ).model_dump(by_alias=True)
        for i in window
    ]
    return responses.success(responses.page_data(cards, page, size, total))


# ── 8-1 통합 검색 ─────────────────────────────────────────────────────────
@app.get("/api/v1/search", dependencies=[Depends(require_internal_token)])
async def app_search(
    user_id: CurrentUserId,
    q: str = Query(...),
    category_id: int | None = Query(None, alias="categoryId"),
    tag_id: int | None = Query(None, alias="tagId"),
    page: int = Query(0),
    size: int = Query(20),
):
    if not q.strip():
        return responses.failure("INVALID_PARAMETER", "요청 값이 올바르지 않습니다.",
                                 [{"field": "q", "message": "필수 값입니다."}])
    try:
        import asyncio
        category, tag = await _resolve_filters(user_id, category_id, tag_id)
        hits, total = await asyncio.to_thread(
            search.keyword_search, user_id, q, category, size, page, tag
        )
    except Exception as e:
        logger.exception("검색 실패")
        return responses.failure("INTERNAL_ERROR", "검색에 실패했습니다.",
                                 str(e)[:300], http_status=500)

    items = [
        SearchResultItem(
            image_id=h["image_id"],
            title=h.get("title", ""),
            summary=h.get("summary", ""),
            thumbnail_url=_thumbnail_url(h["image_id"]),
            score=h.get("score", 0.0),
            tags=_tag_refs(h.get("tags") or []),
            created_at=h.get("created_at"),
        ).model_dump(by_alias=True)
        for h in hits
    ]
    return responses.success(responses.page_data(items, page, size, total))

