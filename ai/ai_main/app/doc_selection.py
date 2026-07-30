"""문서화 이미지 선택 — 이 기능의 전부가 이 한 파일에 있다.

    ① 사용자가 문서화를 시작하고 첫 이미지를 고른다
    ② POST /internal/v1/documents/candidates {imageIds: [101]}      → 추천 목록
    ③ 하나 고른다 → {imageIds: [101, 204]}  → **두 장의 중심 벡터**로 추천
    ④ 또 고른다   → {imageIds: [101, 204, 77]} → 세 장의 중심 벡터로 추천
    ⑤ 최대 10장(`related.MAX_SELECTION`). 선택을 마치면 Spring 이 그 id 들로
      POST /internal/v1/documents 를 부른다 — **이 파일은 문서를 만들지 않는다.**

**선택 목록은 서버가 들고 있지 않다.** 매 호출에 전체 `imageIds` 를 받는다.
서버 세션으로 만들면 `jobs.py` 처럼 프로세스 메모리가 되어 재기동·다중 인스턴스에서
선택이 날아간다. 정수 10개를 다시 보내는 비용으로 무상태를 산다. 그래서 뒤로 가기·
앱 재시작·중간 실패가 전부 "그 시점 목록으로 다시 호출" 하나로 복구된다.

검색·중심 벡터·사용자 격리는 **`related.similar_images` 하나만** 쓴다. 이 파일이
따로 들고 있는 것은 화면 계약(`imageIds`·선택 상한·`selectedImageIds` 응답)뿐이다.
kNN 질의와 격리 필터를 복사하면 한쪽에서만 user_id 필터가 빠지는 사고가 난다.

main.py 는 `include_router` 한 줄만 닿는다. 되돌리려면 이 파일과 그 한 줄만 지운다.
"""

import asyncio
import logging
import uuid
from typing import Literal

from fastapi import APIRouter
from fastapi.responses import JSONResponse
from pydantic import Field, model_validator

from . import related
from .schemas import CamelModel, SearchHit

logger = logging.getLogger(__name__)

router = APIRouter()


class DocSelectionRequest(CamelModel):
    # userId 는 격리의 전부다. 0 이하를 막는 이유는 main.resolve_user_id 와 같다.
    user_id: int = Field(gt=0)
    # 지금까지 고른 이미지 전부. 순서는 결과에 영향을 주지 않는다(중심 벡터는
    # 집합 연산이라 모든 선택이 같은 무게다).
    image_ids: list[int] = Field(min_length=1, max_length=related.MAX_SELECTION)
    # Spring 이 요청을 추적하려고 주는 값. 없으면 서버가 만든다(응답에는 항상 있다).
    correlation_id: str | None = None
    page: int = Field(default=0, ge=0)
    size: int = Field(default=10, ge=1, le=100)

    @model_validator(mode="after")
    def _clean_selection(self):
        if any(i <= 0 for i in self.image_ids):
            raise ValueError("imageIds 는 1 이상이어야 합니다.")
        # 중복을 남기면 그 이미지가 중심 벡터에 두 번 반영돼, 사용자가 한 번만
        # 고른 이미지 쪽으로 추천이 조용히 쏠린다. 입력 순서는 유지한다.
        self.image_ids = list(dict.fromkeys(self.image_ids))
        return self


class DocSelectionPayload(CamelModel):
    correlation_id: str
    total: int
    page: int = 0
    size: int = 10
    hits: list[SearchHit] = []
    # 이번 요청이 기준으로 삼은(그래서 결과에서 제외된) 이미지들. 중복 제거 뒤의
    # 값이라, 앱이 자기 누적 목록과 대조해 어긋남을 잡을 수 있다.
    selected_image_ids: list[int] = []


class DocSelectionEvent(CamelModel):
    """Spring 이 받는 이벤트 봉투. 연관 이미지와 같은 `eventType` 을 쓴다 —
    Spring 쪽에서 보면 "이미지 검색이 끝났다" 는 같은 사건이다."""
    event_type: Literal["IMAGE_SEARCH_COMPLETED"] = "IMAGE_SEARCH_COMPLETED"
    version: int = 1
    payload: DocSelectionPayload


# 토큰 검사는 main.py 가 include_router 에서 걸어 준다(내부 API 공통 규칙).
@router.post("/internal/v1/documents/candidates",
             response_model=DocSelectionEvent,
             responses={404: {"description": "IMAGE_NOT_FOUND"},
                        502: {"description": "SEARCH_FAILED"}})
async def document_candidates(request: DocSelectionRequest):
    """지금까지 고른 이미지들의 중심 벡터로 다음 후보를 추천한다.

    고른 이미지는 전부 결과에서 빠진다. 추천이 0건인 것은 **정상 상태**다 —
    더 붙일 만한 이미지가 없다는 뜻이고, 사용자는 그 상태로 문서 생성으로 넘어갈 수 있다.
    """
    correlation_id = request.correlation_id or str(uuid.uuid4())
    selection = request.image_ids
    try:
        hits, total = await asyncio.to_thread(
            related.similar_images, request.user_id, selection,
            request.size, request.page,
        )
    except related.ImageNotFoundError as e:
        return related.error_response("IMAGE_NOT_FOUND", str(e), 404)
    except Exception as e:
        logger.exception("문서화 후보 검색 실패 imageIds=%s", selection)
        return related.error_response("SEARCH_FAILED", str(e)[:500], 502)

    event = DocSelectionEvent(payload=DocSelectionPayload(
        correlation_id=correlation_id, total=total,
        page=request.page, size=request.size,
        hits=[SearchHit(**h) for h in hits],
        selected_image_ids=selection,
    ))
    return JSONResponse(status_code=200, content=event.model_dump(by_alias=True))
