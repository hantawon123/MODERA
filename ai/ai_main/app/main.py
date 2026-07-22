"""FastAPI 내부 API 서버.

공통 응답 envelope 를 쓰지 않는 서비스 간 통신 전용이며,
에러는 {"error": CODE, "message": ..., "detail": {}} 형식으로 반환한다.
"""

import logging
from typing import Any

from fastapi import BackgroundTasks, Depends, FastAPI, Header, Request
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse

from . import gemini_client, search
from .config import get_settings
from .jobs import job_registry
from .schemas import (
    AnalyzeAccepted,
    AnalyzeRequest,
    EmbedRequest,
    EmbedResponse,
    EmbeddingItem,
    ParsedConditions,
    QueryParseRequest,
    QueryParseResponse,
    SearchHit,
    SearchRequest,
    SearchResponse,
)
from .stages import execute_stage

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s %(levelname)s [%(name)s] %(message)s",
)
logger = logging.getLogger(__name__)

app = FastAPI(title="Screenshot Knowledge AI (internal)", docs_url=None, redoc_url=None)


def _error(code: str, message: str, detail: Any = None, http_status: int = 400) -> JSONResponse:
    return JSONResponse(
        status_code=http_status,
        content={"error": code, "message": message, "detail": detail or {}},
    )


async def require_internal_token(x_internal_token: str = Header(default="")) -> None:
    if x_internal_token != get_settings().internal_token:
        raise PermissionError("내부 토큰 불일치")


@app.exception_handler(PermissionError)
async def _permission_handler(request: Request, exc: PermissionError) -> JSONResponse:
    return _error("UNAUTHORIZED", str(exc), http_status=401)


@app.exception_handler(RequestValidationError)
async def _validation_handler(request: Request, exc: RequestValidationError) -> JSONResponse:
    return _error("INVALID_REQUEST", "요청 값이 올바르지 않습니다.",
                  detail={"errors": exc.errors()[:5]}, http_status=400)


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
