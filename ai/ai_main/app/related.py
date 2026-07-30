"""연관 이미지(연쇄 선택) — 이 기능의 전부가 이 한 파일에 있다.

    사용자가 이미지 한 장을 고르고 '연관 이미지 선택' 을 누른다
      → Spring → POST /internal/v1/search/related {imageId}
      → 그 이미지의 **색인된 요약 벡터로** kNN → 인근 이미지 id 목록
      → 사용자가 그중 한 장을 골라 같은 엔드포인트를 다시 부른다(연쇄)

스키마·검색 로직·라우터를 한 파일에 모은 이유는 **머지 충돌 표면을 없애기** 위해서다.
main.py 는 `include_router` 한 줄만 닿고, search.py·schemas.py 는 전혀 닿지 않는다.
이 기능을 되돌리려면 이 파일과 그 한 줄만 지우면 된다.

search.py 의 내부 헬퍼(`_client`·`_knn_body`·`_search_filters`·`_hit_dict`)를
`search.` 로 명시해 빌려 쓴다. 복사하면 사용자 격리 필터·hit 키집합이 두 곳에서
따로 늙는다 — kNN 질의에 user_id 필터가 빠지는 사고는 그렇게 생긴다.
"""

import asyncio
import logging
import uuid
from typing import Any, Literal

from fastapi import APIRouter
from fastapi.responses import JSONResponse
from pydantic import Field

from . import search
from .config import get_settings
from .schemas import CamelModel, SearchHit

logger = logging.getLogger(__name__)

router = APIRouter()


# ── 스키마 ────────────────────────────────────────────────────────────────
class RelatedSearchRequest(CamelModel):
    # userId 는 격리의 전부다. 0 이하를 막는 이유는 main.resolve_user_id 와 같다 —
    # 0 은 카테고리 벡터 저장소의 전역 시드 소유자(search.SEED_USER_ID)로 쓰이는
    # 값이라 실제 사용자로 들어와선 안 된다. 여기서 막으면 400 으로 떨어진다.
    user_id: int = Field(gt=0)
    image_id: int = Field(gt=0)      # 사용자가 고른 기준 이미지
    # Spring 이 요청을 추적하려고 주는 값. 없으면 서버가 만든다(응답에는 항상 있다).
    correlation_id: str | None = None
    page: int = Field(default=0, ge=0)
    # 상한을 둔다. size 가 크면 후보풀 n 도 같이 커져 OpenSearch 를 그만큼 때린다.
    size: int = Field(default=10, ge=1, le=100)


class ImageSearchPayload(CamelModel):
    correlation_id: str
    total: int
    page: int = 0
    size: int = 10
    hits: list[SearchHit] = []


class ImageSearchCompletedEvent(CamelModel):
    """Spring 이 받는 이벤트 봉투."""
    event_type: Literal["IMAGE_SEARCH_COMPLETED"] = "IMAGE_SEARCH_COMPLETED"
    version: int = 1
    payload: ImageSearchPayload


class ImageNotFoundError(LookupError):
    """대상 이미지가 색인에 없거나 요청자 소유가 아니다.

    둘을 구분해 주지 않는다 — 403 으로 갈라 주면 "그 imageId 는 존재한다" 를
    알려 주는 셈이라 남의 imageId 열거에 쓰인다(main._owned_image 와 같은 이유).
    """


# ── 검색 ──────────────────────────────────────────────────────────────────
def similar_images(
    user_id: int, image_id: int, size: int = 10, page: int = 0
) -> tuple[list[dict[str, Any]], int]:
    """기준 이미지의 검색 벡터로 인근 이미지를 찾는다.

    질의 벡터를 새로 만들지 않고 **색인된 embedding(요약 기반 bge-m3)을 그대로**
    질의로 쓴다 — 임베딩 호출 0회. 그래서 방금 받은 결과 중 하나를 다시 대상으로
    넣는 연쇄 호출이 사실상 kNN 한 번 값이다.

    자기 자신은 결과에서 뺀다. 벡터가 없는 문서(빈 OCR 등)는 비교할 게 없으므로
    빈 결과다 — 에러가 아니다.

    점수는 재계산 코사인이다(bge-m3 는 단위벡터라 cos = dot). 하이브리드 검색과
    같은 임계값으로 잘라 "인근" 이라 부를 수 없는 문서를 결과에 넣지 않는다.
    total 은 page 무관 고정 후보풀 위의 생존자 수라 페이지 간 안정적이다.

    **사용자 격리.** 벡터는 사용자별 인덱스가 아니라 하나의 인덱스·하나의
    `embedding` 필드에 전부 들어 있다(벡터 공간은 물리적으로 공유된다). 격리는
    공간 분리가 아니라 **모든 질의에 걸리는 `user_id` term 필터**로 한다.
    이 함수는 그 필터를 두 겹으로 건다:

      1. kNN 질의의 pre-filter (`search._search_filters` → `{"term": {"user_id": ...}}`)
      2. 돌아온 hit 마다 `user_id` 를 다시 대조 — 안 맞으면 버리고 에러로 남긴다

    2번이 있는 이유: 1번은 OpenSearch 2.4+ lucene 엔진의 knn filter 에 의존한다.
    그게 어떤 이유로든(엔진·버전·매핑 변경) 안 먹으면 남의 이미지가 결과에 섞인다.
    필터가 조용히 무시되는 상황을 코드가 스스로 알아내지 못하므로, 값을 직접 본다.
    """
    if user_id <= 0:
        # 스키마에서 이미 막지만(400), 스크립트·테스트가 직접 부르는 경로도 있다.
        raise ValueError(f"userId 는 1 이상이어야 합니다: {user_id}")

    search.ensure_index()
    settings = get_settings()
    client = search._client()

    try:
        doc = client.get(
            index=settings.opensearch_index,
            id=str(image_id),
            _source_includes=["embedding", "user_id"],
        )
    except Exception as e:
        raise ImageNotFoundError(f"이미지를 찾을 수 없습니다: {image_id}") from e
    source = doc.get("_source", {}) if doc.get("found") else {}
    if source.get("user_id") != user_id:
        raise ImageNotFoundError(f"이미지를 찾을 수 없습니다: {image_id}")

    qvec = source.get("embedding")
    if not qvec:
        return [], 0

    # 자기 자신이 1위로 걸리므로 한 칸 더 받는다.
    start = max(0, page) * size
    n = max(settings.search_hybrid_pool_size, start + size + 1)
    body = search._knn_body(qvec, search._search_filters(user_id, None, None), n)
    # 2차 대조에 쓸 user_id 를 _source 에 추가로 받는다(search._POOL_SOURCE 엔 없다).
    body["_source"] = {"includes": [*search._POOL_SOURCE, "user_id"]}
    resp = client.search(index=settings.opensearch_index, body=body)

    qn = len(qvec)
    hits: list[dict[str, Any]] = []
    foreign = 0
    for hit in resp["hits"]["hits"]:
        src = hit["_source"]
        if src.get("user_id") != user_id:
            # 여기 들어오면 kNN pre-filter 가 안 먹었다는 뜻이다. 버리고 계속하되,
            # 조용히 넘기지 않는다 — 인프라가 바뀌어 격리가 깨진 신호다.
            foreign += 1
            continue
        if src.get("image_id") == image_id:
            continue
        emb = src.get("embedding")
        if not emb or len(emb) != qn:
            continue
        cos = sum(a * b for a, b in zip(qvec, emb))
        if cos < settings.search_knn_min_cosine:
            continue
        hits.append(search._hit_dict(src, round(cos, 6)))
    if foreign:
        logger.error(
            "연관 이미지 kNN 이 남의 문서 %s건을 돌려줬다 — user_id pre-filter 가 "
            "동작하지 않는다(OpenSearch 버전·엔진 확인 필요). userId=%s imageId=%s",
            foreign, user_id, image_id,
        )
    return hits[start:start + size], len(hits)


# ── 엔드포인트 ────────────────────────────────────────────────────────────
# 토큰 검사는 main.py 가 include_router 에서 걸어 준다(내부 API 공통 규칙).
@router.post("/internal/v1/search/related",
             response_model=ImageSearchCompletedEvent,
             responses={404: {"description": "IMAGE_NOT_FOUND"},
                        502: {"description": "SEARCH_FAILED"}})
async def related_search(request: RelatedSearchRequest):
    """기준 이미지의 색인 벡터로 인근 이미지를 찾는다.

    상태를 들고 있지 않다 — 매 호출이 독립이라 연쇄가 어디서 끊겨도 다시 부르면 된다.
    """
    correlation_id = request.correlation_id or str(uuid.uuid4())
    try:
        hits, total = await asyncio.to_thread(
            similar_images, request.user_id, request.image_id, request.size, request.page
        )
    except ImageNotFoundError as e:
        return _error("IMAGE_NOT_FOUND", str(e), 404)
    except Exception as e:
        logger.exception("연관 이미지 검색 실패 imageId=%s", request.image_id)
        return _error("SEARCH_FAILED", str(e)[:500], 502)

    event = ImageSearchCompletedEvent(payload=ImageSearchPayload(
        correlation_id=correlation_id, total=total,
        page=request.page, size=request.size,
        hits=[SearchHit(**h) for h in hits],
    ))
    return JSONResponse(status_code=200, content=event.model_dump(by_alias=True))


def _error(code: str, message: str, http_status: int) -> JSONResponse:
    """내부 API 공통 에러 형식(main._error 와 같은 모양). main 을 import 하면
    순환 참조가 되므로 이 네 줄만 여기 둔다."""
    return JSONResponse(
        status_code=http_status,
        content={"error": code, "message": message, "detail": {}},
    )
