"""FastAPI 내부 API 서버.

공통 응답 envelope 를 쓰지 않는 서비스 간 통신 전용이며,
에러는 {"error": CODE, "message": ..., "detail": {}} 형식으로 반환한다.
"""

import asyncio
import logging
import os
from contextlib import asynccontextmanager
from typing import Any

from fastapi import BackgroundTasks, Depends, FastAPI, Query, Request
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse, Response

from . import (
    app_images, app_library, doc_selection, document, gemini_client, internal_api,
    reanalyze, related, responses, search, storage,
)
from .config import get_settings
from .deps import (
    CurrentUserId,
    _category_refs,
    _error,
    _image_url,
    _is_app_api,
    _MissingUserId,
    _owned_image,
    _resolve_filters,
    _tag_refs,
    _thumbnail_url,
    _to_list_item,
    require_internal_token,
)
from .jobs import job_registry, job_store
from .schemas import (
    AnalysisJob,
    AnalysisSummary,
    AnalyzeAccepted,
    AnalyzeRequest,
    CategoryCard,
    CategoryListData,
    CategoryRef,
    ActiveAnalysis,
    ApiResponse,
    DocumentRequest,
    DocumentResponse,
    EmbedRequest,
    EmbedResponse,
    EmbeddingItem,
    HomeAnalysisStatus,
    HomeCategory,
    HomeRecentImage,
    HomeResponse,
    ImageDetail,
    ImageListItem,
    OcrInput,
    OcrSubmitResponse,
    PageData,
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
    ThumbnailResponse,
    UploadCompleteResponse,
    UploadRequest,
    UploadResponse,
    UploadUrlResponse,
    RegisteredUpload,
    DuplicatedUpload,
    FailedUpload,
)
from .stages import (
    execute_stage,
    requeue_interrupted,
    run_app_analysis,
    seed_default_category_vectors,
)

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s %(levelname)s [%(name)s] %(message)s",
)
logger = logging.getLogger(__name__)

# 문서 UI 는 개발·팀 공유 편의를 위해 켜둔다. 외부에 노출되는 서버라면
# ENABLE_DOCS=false 로 꺼야 한다(엔드포인트 구조가 그대로 드러난다).
# get_settings() 는 자격증명이 없으면 예외를 던지므로 여기서는 환경변수를 직접 읽는다.
_docs_enabled = os.environ.get("ENABLE_DOCS", "true").lower() == "true"


@asynccontextmanager
async def _lifespan(app: FastAPI):
    """기동 시 기본 카테고리 이름 벡터를 전역 시드로 심는다(멱등, best-effort).

    OpenSearch·Gemini 가 아직 안 떴어도 기동을 막지 않는다 — 시드 함수가 예외를
    삼키고, 다음 기동에서 다시 시도한다. 요청 처리 중이 아니라 기동 시점에 하는
    이유는, 판정 경로(resolve_category)에서 외부 호출을 없애기 위해서다.

    이어서 재기동으로 끊긴 분석을 되살린다. 조회·재큐잉이 오래 걸릴 수 있어
    태스크로 흘려보내고 기동은 막지 않는다(헬스체크가 먼저 통과해야 한다).
    """
    # 잘못된 매핑(auto-create 사고)을 요청 받기 전에 교정한다. 멱등·best-effort.
    await asyncio.to_thread(search.migrate_image_index_mapping)
    await asyncio.to_thread(seed_default_category_vectors)
    _startup = asyncio.create_task(requeue_interrupted())
    yield
    # 남아 있으면 취소한다. 종료 중에 새 분석을 시작해 봐야 어차피 끊긴다.
    _startup.cancel()


app = FastAPI(
    lifespan=_lifespan,
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


# ── OpenAPI 후처리 ────────────────────────────────────────────────────────
# FastAPI 는 검증 가능한 파라미터가 있는 엔드포인트에 422 를 자동으로 문서화한다.
# 그런데 우리는 RequestValidationError 를 400 INVALID_PARAMETER 로 바꿔 내보내므로
# 422 는 실제로 나가지 않는다. 문서에 남아 있으면 프론트가 없는 응답을 처리하게 된다.
# 대신 명세 1.3 의 실제 에러 응답을 채워 넣는다.
_APP_ERROR_EXAMPLE = {
    "code": "IMAGE_NOT_FOUND",
    "message": "이미지를 찾을 수 없습니다.",
    "data": "imageId: 1024",
    "timestamp": "2026-07-16T06:00:00.000Z",
}
_APP_ERRORS = {
    "400": ("INVALID_PARAMETER / INVALID_SORT", {
        "code": "INVALID_PARAMETER",
        "message": "요청 값이 올바르지 않습니다.",
        "data": [{"field": "sort", "message": "지원하지 않는 정렬입니다."}],
        "timestamp": "2026-07-16T06:00:00.000Z",
    }),
    "401": ("UNAUTHORIZED", {
        "code": "UNAUTHORIZED", "message": "인증이 필요합니다.",
        "data": None, "timestamp": "2026-07-16T06:00:00.000Z",
    }),
    "404": ("IMAGE_NOT_FOUND / THUMBNAIL_NOT_FOUND", _APP_ERROR_EXAMPLE),
}


def _custom_openapi() -> dict[str, Any]:
    if app.openapi_schema:
        return app.openapi_schema
    from fastapi.openapi.utils import get_openapi

    schema = get_openapi(
        title=app.title, version=app.version,
        description=app.description, routes=app.routes,
    )
    for path, ops in schema.get("paths", {}).items():
        if not path.startswith("/api/v1"):
            continue
        for op in ops.values():
            resp = op.get("responses", {})
            resp.pop("422", None)
            for code, (desc, example) in _APP_ERRORS.items():
                resp.setdefault(code, {
                    "description": desc,
                    "content": {"application/json": {"example": example}},
                })
    app.openapi_schema = schema
    return schema


app.openapi = _custom_openapi  # type: ignore[method-assign]


# 연관 이미지(연쇄 선택). 기능 전체가 related.py 안에 있어 여기는 이 한 줄만 닿는다.
app.include_router(related.router, dependencies=[Depends(require_internal_token)])
# 문서화 이미지 선택. 검색·격리는 related 를 재사용하고 화면 계약만 따로 든다.
app.include_router(doc_selection.router, dependencies=[Depends(require_internal_token)])
# 카테고리 재분석(결과가 맘에 안 들 때). 판정 코어는 category/stages 를 재사용한다.
app.include_router(reanalyze.router, dependencies=[Depends(require_internal_token)])


@app.exception_handler(search.InvalidSortError)
async def _invalid_sort_handler(request: Request, exc: Exception) -> JSONResponse:
    """명세 11: INVALID_SORT(400) — 지원하지 않는 정렬 필드 또는 방향."""
    return responses.failure("INVALID_SORT", str(exc),
                             [{"field": "sort", "message": str(exc)}], http_status=400)


@app.exception_handler(PermissionError)
async def _permission_handler(request: Request, exc: PermissionError) -> JSONResponse:
    if _is_app_api(request):
        # 명세 1.1 공통 envelope + 1.3 UNAUTHORIZED(401).
        # 인증 실패 사유는 밖으로 내보내지 않는다.
        return responses.failure("UNAUTHORIZED", "인증이 필요합니다.", http_status=401)
    return _error("UNAUTHORIZED", str(exc), http_status=401)


@app.exception_handler(RequestValidationError)
async def _validation_handler(request: Request, exc: RequestValidationError) -> JSONResponse:
    if _is_app_api(request):
        # 명세 1.1: 검증 오류는 data 에 {field, message} 배열을 담는다.
        fields = [
            {
                # loc 은 ("query", "page") 처럼 위치까지 담고 있어 마지막 요소만 쓴다.
                "field": str(e["loc"][-1]) if e.get("loc") else "",
                "message": e.get("msg", ""),
            }
            for e in exc.errors()[:5]
        ]
        return responses.failure("INVALID_PARAMETER", "요청 값이 올바르지 않습니다.",
                                 fields, http_status=400)
    # pydantic v2 는 커스텀 validator 가 올린 **예외 객체 자체**를 ctx 에 담아 준다.
    # 그대로 응답에 넣으면 json.dumps 가 터져 400 이 500(스택트레이스)으로 바뀐다.
    # 문자열로 확정된 키만 남긴다.
    return _error("INVALID_REQUEST", "요청 값이 올바르지 않습니다.",
                  detail={"errors": [
                      {"loc": [str(p) for p in e.get("loc", ())],
                       "msg": str(e.get("msg", "")),
                       "type": str(e.get("type", ""))}
                      for e in exc.errors()[:5]
                  ]}, http_status=400)


# ── 사용자 식별 ───────────────────────────────────────────────────────────
# 식별 자체(_MissingUserId·resolve_user_id·CurrentUserId)는 deps.py 에 있다.
# 여기 남는 것은 app 객체가 필요한 예외 핸들러뿐이다.
@app.exception_handler(_MissingUserId)
async def _missing_user_id_handler(request: Request, exc: _MissingUserId) -> JSONResponse:
    return responses.failure("INVALID_PARAMETER", "요청 값이 올바르지 않습니다.",
                             [{"field": "userId", "message": "1 이상의 값이 필요합니다."}])


@app.get("/health")
async def health() -> dict[str, str]:
    return {"status": "ok"}


# 내부 API(`/internal/v1/*`, 명세 10 장). 라우트 전부가 internal_api.py 에 있다.
# 여기에 두는 이유는 등록 순서다 — 옮기면 openapi.json 의 path 순서가 바뀐다.
app.include_router(internal_api.router)
# 앱 API(`/api/v1/*`, Spring 우회 구간). 명세 4·6 장은 app_images.py,
# 5·7·8 장은 app_library.py 에 있다. 규약 배너도 app_images.py 로 함께 옮겼다.
app.include_router(app_images.router)
app.include_router(app_library.router)
