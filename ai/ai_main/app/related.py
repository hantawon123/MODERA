"""연관 이미지(연쇄 선택) + 벡터 인근 검색 **공용 코어**.

    사용자가 이미지 한 장을 고르고 '연관 이미지 선택' 을 누른다
      → Spring → POST /internal/v1/search/related {imageId}
      → 그 이미지의 **색인된 요약 벡터로** kNN → 인근 이미지 id 목록
      → 사용자가 그중 한 장을 골라 같은 엔드포인트를 다시 부른다(연쇄)

`similar_images` 는 선택 이미지를 **목록으로** 받아 중심 벡터로 검색한다. 1장이면
중심 = 그 벡터라 연관 이미지가 되고, N장이면 문서화 선택이 된다. 문서화 쪽 화면
계약(imageIds·상한·응답 필드)은 `doc_selection.py` 가 따로 들고 있고, 검색·격리는
여기 하나만 쓴다 — 복사하면 kNN 질의에서 user_id 필터가 빠지는 사고가 한쪽에서만
생긴다.

스키마·검색 로직·라우터를 한 파일에 모은 이유는 **머지 충돌 표면을 없애기** 위해서다.
main.py 는 `include_router` 한 줄만 닿고, search.py·schemas.py 는 전혀 닿지 않는다.
이 기능을 되돌리려면 이 파일과 그 한 줄만 지우면 된다.

search.py 의 내부 헬퍼(`_client`·`_knn_body`·`_search_filters`·`_hit_dict`)를
`search.` 로 명시해 빌려 쓴다. 복사하면 사용자 격리 필터·hit 키집합이 두 곳에서
따로 늙는다 — kNN 질의에 user_id 필터가 빠지는 사고는 그렇게 생긴다.
"""

import asyncio
import logging
import math
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

# 한 번의 질의에 기준으로 삼을 수 있는 이미지 수 상한. 문서화 화면의 "최대 10장
# 선택" 이 이 값이고, doc_selection.py 가 이걸 그대로 쓴다. 문서 생성기 쪽
# 상한(document.MAX_IMAGES=30)과는 별개 노브다 — 저건 "만드는 쪽" 의 상한이다.
MAX_SELECTION = 10


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

    **선택 목록 중 하나라도** 걸리면 올린다. 하나만 허용해도 남의 벡터가 중심에
    섞이고, 그러면 내 이미지 중 무엇이 올라오는지로 남의 이미지 내용을 추론할 수 있다.
    """


# ── 벡터 ──────────────────────────────────────────────────────────────────
def _centroid(vectors: list[list[float]]) -> list[float] | None:
    """선택 벡터들의 평균을 **정규화해서** 돌려준다. 만들 수 없으면 None.

    정규화가 핵심이다. bge-m3 는 단위벡터지만 단위벡터 k개의 평균은 단위벡터가
    아니다 — `|m| = sqrt((k + k(k-1)c) / k²)`, c 는 선택 간 평균 코사인. 그리고
    `dot(m, e) <= |m|` 이므로 정규화하지 않으면 **어떤 후보도 |m| 보다 높은 점수를
    받을 수 없다**. 주제가 다양한 10장을 고르면 |m| 이 0.44 까지 내려가 임계값
    0.45 를 아무도 못 넘는다 — 에러 없이 추천이 마르는 조용한 고장이다.

    OpenSearch cosinesimil kNN 은 스케일 무관이라 후보 순서는 정규화 여부와 무관하고,
    우리가 다시 계산하는 점수·컷만 영향을 받는다. 그래서 눈에 안 띈다.
    """
    if not vectors:
        return None
    dim = len(vectors[0])
    total = [0.0] * dim
    for vector in vectors:
        for i, value in enumerate(vector):
            total[i] += value
    norm = math.sqrt(sum(v * v for v in total))
    if norm < 1e-9:
        # 서로 정반대인 벡터들의 평균. 방향이 없어 "중심" 이 성립하지 않는다.
        return None
    return [v / norm for v in total]


def _load_selected(
    client: Any, index: str, user_id: int, image_ids: list[int]
) -> list[list[float]]:
    """선택 이미지들을 한 번에 읽어 소유를 검증하고 벡터만 모은다.

    mget 1왕복이다 — 10장을 get 10번 하지 않는다. 벡터가 없는 이미지(빈 OCR 등)는
    비교할 게 없어 조용히 빠진다. 소유가 아니거나 없는 문서는 예외다.
    """
    resp = client.mget(
        index=index,
        body={"ids": [str(i) for i in image_ids]},
        _source_includes=["embedding", "user_id"],
    )
    vectors: list[list[float]] = []
    for doc in resp.get("docs", []):
        if not doc.get("found"):
            raise ImageNotFoundError(f"이미지를 찾을 수 없습니다: {doc.get('_id')}")
        source = doc.get("_source", {})
        if source.get("user_id") != user_id:
            raise ImageNotFoundError(f"이미지를 찾을 수 없습니다: {doc.get('_id')}")
        vector = source.get("embedding")
        if vector:
            vectors.append([float(v) for v in vector])
    return vectors


# ── 검색 ──────────────────────────────────────────────────────────────────
def similar_images(
    user_id: int, image_ids: list[int], size: int = 10, page: int = 0
) -> tuple[list[dict[str, Any]], int]:
    """선택한 이미지들의 중심 벡터로 인근 이미지를 찾는다.

    질의 벡터를 새로 만들지 않고 **색인된 embedding(요약 기반 bge-m3)을 그대로**
    쓴다 — 임베딩 호출 0회. 그래서 선택을 한 장 늘려 다시 부르는 연쇄 호출이
    사실상 mget 1회 + kNN 1회 값이다.

    선택된 이미지는 전부 결과에서 뺀다. 벡터가 있는 선택이 하나도 없으면
    (빈 OCR 만 골랐거나 중심을 만들 수 없으면) 빈 결과다 — 에러가 아니다.

    **순위와 컷을 분리한다.**

      순위: 정규화한 중심 벡터와의 코사인. 화면에 내려가는 `score` 도 이 값이다.
      컷:   선택한 이미지 **아무 한 장과의** 최대 코사인이 임계값 이상.

    컷을 중심 벡터로 하지 않는 이유는 임계값이 k 에 따라 의미가 달라지기 때문이다.
    중심 벡터는 선택이 늘수록 어느 개별 문서와도 멀어져서(다양한 10장이면 관련
    문서도 0.37 수준) 고정 임계값 0.45 에 다 걸린다. 최대 유사도는 k 와 무관하므로
    캘리브레이션된 `SEARCH_KNN_MIN_COSINE` 값을 그대로 쓸 수 있다.
    선택이 1장이면 중심 = 그 벡터라 두 기준이 같아진다(연관 이미지 기존 동작).

    total 은 page 무관 고정 후보풀 위의 생존자 수라 페이지 간 안정적이다.

    **사용자 격리.** 벡터는 사용자별 인덱스가 아니라 하나의 인덱스·하나의
    `embedding` 필드에 전부 들어 있다(벡터 공간은 물리적으로 공유된다). 격리는
    공간 분리가 아니라 **모든 질의에 걸리는 `user_id` term 필터**로 한다.
    이 함수는 세 겹으로 건다:

      1. 선택 이미지 전부 소유 검증(mget) — 하나라도 남의 것이면 404
      2. kNN 질의의 pre-filter (`search._search_filters`)
      3. 돌아온 hit 마다 `user_id` 를 다시 대조 — 안 맞으면 버리고 에러로 남긴다

    3번이 있는 이유: 2번은 OpenSearch 2.4+ lucene 엔진의 knn filter 에 의존한다.
    그게 어떤 이유로든(엔진·버전·매핑 변경) 안 먹으면 남의 이미지가 결과에 섞인다.
    필터가 조용히 무시되는 상황을 코드가 스스로 알아내지 못하므로, 값을 직접 본다.
    """
    if user_id <= 0:
        # 스키마에서 이미 막지만(400), 스크립트·테스트가 직접 부르는 경로도 있다.
        raise ValueError(f"userId 는 1 이상이어야 합니다: {user_id}")
    selection = list(dict.fromkeys(image_ids or []))
    if not selection:
        raise ValueError("imageIds 가 비어 있습니다.")
    if len(selection) > MAX_SELECTION:
        raise ValueError(f"최대 {MAX_SELECTION}장까지 선택할 수 있습니다.")

    search.ensure_index()
    settings = get_settings()
    client = search._client()

    try:
        vectors = _load_selected(
            client, settings.opensearch_index, user_id, selection
        )
    except ImageNotFoundError:
        raise
    except Exception as e:
        # mget 자체가 실패(인덱스 없음·연결 끊김)한 경우와 소유 실패를 섞지 않는다.
        raise search.SearchError(f"선택 이미지 조회 실패: {e}") from e

    qvec = _centroid(vectors)
    if qvec is None:
        return [], 0

    # 선택된 것들이 후보 앞자리를 다 차지하므로 그만큼 더 받는다.
    start = max(0, page) * size
    n = max(settings.search_hybrid_pool_size, start + size + len(selection))
    body = search._knn_body(qvec, search._search_filters(user_id, None, None), n)
    # 2차 대조에 쓸 user_id 를 _source 에 추가로 받는다(search._POOL_SOURCE 엔 없다).
    body["_source"] = {"includes": [*search._POOL_SOURCE, "user_id"]}
    resp = client.search(index=settings.opensearch_index, body=body)

    excluded = set(selection)
    dim = len(qvec)
    threshold = settings.search_knn_min_cosine
    hits: list[dict[str, Any]] = []
    foreign = 0
    for hit in resp["hits"]["hits"]:
        src = hit["_source"]
        if src.get("user_id") != user_id:
            # 여기 들어오면 kNN pre-filter 가 안 먹었다는 뜻이다. 버리고 계속하되,
            # 조용히 넘기지 않는다 — 인프라가 바뀌어 격리가 깨진 신호다.
            foreign += 1
            continue
        if src.get("image_id") in excluded:
            continue
        emb = src.get("embedding")
        if not emb or len(emb) != dim:
            continue
        # 컷은 선택 중 아무 한 장과의 최대 유사도로, 순위는 중심 벡터로.
        if max(sum(a * b for a, b in zip(v, emb)) for v in vectors) < threshold:
            continue
        score = sum(a * b for a, b in zip(qvec, emb))
        hits.append(search._hit_dict(src, round(score, 6)))
    if foreign:
        logger.error(
            "연관 이미지 kNN 이 남의 문서 %s건을 돌려줬다 — user_id pre-filter 가 "
            "동작하지 않는다(OpenSearch 버전·엔진 확인 필요). userId=%s imageIds=%s",
            foreign, user_id, selection,
        )
    # kNN 이 준 순서는 중심 벡터 기준이라 이미 score desc 다. 동점은 image_id desc 로
    # 고정해 페이지를 넘길 때 순서가 흔들리지 않게 한다.
    hits.sort(key=lambda h: (-h["score"], -(h["image_id"] or 0)))
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
    여러 장을 누적해 고르는 문서화 선택은 `doc_selection.py` 가 담당한다.
    """
    correlation_id = request.correlation_id or str(uuid.uuid4())
    try:
        hits, total = await asyncio.to_thread(
            similar_images, request.user_id, [request.image_id],
            request.size, request.page,
        )
    except ImageNotFoundError as e:
        return error_response("IMAGE_NOT_FOUND", str(e), 404)
    except Exception as e:
        logger.exception("연관 이미지 검색 실패 imageId=%s", request.image_id)
        return error_response("SEARCH_FAILED", str(e)[:500], 502)

    event = ImageSearchCompletedEvent(payload=ImageSearchPayload(
        correlation_id=correlation_id, total=total,
        page=request.page, size=request.size,
        hits=[SearchHit(**h) for h in hits],
    ))
    return JSONResponse(status_code=200, content=event.model_dump(by_alias=True))


def error_response(code: str, message: str, http_status: int) -> JSONResponse:
    """내부 API 공통 에러 형식(main._error 와 같은 모양). main 을 import 하면
    순환 참조가 되므로 이 네 줄만 여기 둔다. doc_selection.py 도 이걸 쓴다."""
    return JSONResponse(
        status_code=http_status,
        content={"error": code, "message": message, "detail": {}},
    )
