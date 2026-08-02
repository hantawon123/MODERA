"""라우터들이 함께 쓰는 의존성·응답 헬퍼.

main.py 가 1300 줄을 넘어가 라우터를 파일별로 쪼갰다. 라우트 모듈이 main.py 의
헬퍼를 import 하면 순환이 되므로(main 이 그 모듈을 include_router 한다) 공유
헬퍼만 여기로 내렸다. **함수 본문은 main.py 에 있던 것과 한 글자도 다르지 않다.**

여기에는 라우트가 없다. 인증(`require_internal_token`), 사용자 식별
(`CurrentUserId`), 응답 조립(`_to_list_item`·`_thumbnail_url` 등), 소유권 확인
(`_owned_image`) 만 있다.
"""

import asyncio
from typing import Annotated, Any

from fastapi import Depends, Query, Request
from fastapi.responses import JSONResponse
from fastapi.security import APIKeyHeader

from . import search, storage
from .config import get_settings
from .schemas import CategoryRef, ImageListItem, TagRef


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
    request: Request,
    token: str | None = Depends(_internal_token_header),
) -> None:
    # 임시: APP_API_AUTH=false 인 동안 /api/v1/* 은 토큰 없이 통과시킨다.
    # Android 가 Spring 을 거치지 않고 직접 부르는 기간용이며, 설정 설명은 config.py 참고.
    if _is_app_api(request) and not get_settings().app_api_auth:
        return
    if token != get_settings().internal_token:
        raise PermissionError("내부 토큰 불일치")


def _is_app_api(request: Request) -> bool:
    """앱 직결 API 인지. 에러 형식이 내부 API 와 다르다(명세 1.1 vs 10 장)."""
    return request.url.path.startswith("/api/v1")


# ── 사용자 식별 ───────────────────────────────────────────────────────────
# MVP 는 회원가입·로그인이 없다. 요청이 보내는 userId 를 검증할 방법이 없으므로
# FIXED_USER_ID 가 설정돼 있으면 요청 값을 무시하고 그 값으로 고정한다.
# 파라미터 자체는 그대로 받아 두기 때문에 로그인이 붙으면 설정만 0 으로 바꾸면 된다.
class _MissingUserId(Exception):
    pass


def resolve_user_id(requested: int | None) -> int:
    fixed = get_settings().fixed_user_id
    if fixed:
        return fixed
    # 0 이하는 사용자 id 로 받지 않는다. 0 은 카테고리 벡터 저장소에서 전역 시드의
    # 소유자(search.SEED_USER_ID)로 이미 쓰고 있어서, 실제 사용자로 들어오면
    # upsert_category_vector 가 문서 id `0:{이름}` 으로 시드를 덮어쓴다 —
    # 그 뒤로는 모든 사용자의 콜드 스타트 판정이 한 사람의 centroid 로 오염된다.
    if requested is None or requested <= 0:
        raise _MissingUserId()
    return requested


async def _current_user_id(
    user_id: int | None = Query(
        None,
        alias="userId",
        # 명세 어디에도 userId 쿼리 파라미터는 없다(인증 토큰에서 사용자를 얻는 설계).
        # 값이 와도 FIXED_USER_ID 로 덮어쓰므로 문서에 노출하지 않는다.
        # 노출하면 프론트가 "무엇을 보내야 하나" 를 고민하게 되고, 보낸 값이
        # 무시되는 것도 혼란스럽다. 받기는 계속 받으므로 기존 호출은 깨지지 않는다.
        include_in_schema=False,
    ),
) -> int:
    return resolve_user_id(user_id)


CurrentUserId = Annotated[int, Depends(_current_user_id)]


def _tag_refs(names: list[str], source: str | None = None) -> list[TagRef]:
    """source 는 상세(6-2)에서만 채운다. 목록(6-1)에는 명세상 없는 필드다."""
    return [TagRef(tag_id=search.stable_id(n), name=n, source=source)
            for n in names or []]


def _category_refs(
    name: str | None, confidence: float | None = None
) -> list[CategoryRef]:
    if not name:
        return []
    return [CategoryRef(category_id=search.stable_id(name), name=name,
                        confidence=confidence)]


def _thumbnail_url(image_id: int, s3_key: str | None = None) -> str | None:
    """목록 격자용 썸네일 주소(정사각 crop).

    기본은 만료 없는 서버 경유 경로다. PRESIGNED_READ_URLS=true 로 켜면 스토리지
    presigned URL 을 준다(트래픽은 줄지만 만료가 생겨 앱 캐시가 매번 깨진다).
    """
    settings = get_settings()
    if s3_key and settings.presigned_read_urls and settings.s3_public_endpoint:
        url = storage.presigned_get_url(s3_key, settings.s3_thumbnail_bucket)
        if url:
            return url
    return f"/api/v1/images/{image_id}/thumbnail/raw"


def _image_url(image_id: int, s3_key: str | None = None) -> str | None:
    """원본 이미지 주소.

    썸네일은 정사각으로 잘려 있어 스크린샷 내용을 다 볼 수 없다. 상세 화면처럼
    전체를 봐야 하는 곳은 이 주소를 쓴다.

    조회 URL 방식은 _thumbnail_url 과 동일하게 PRESIGNED_READ_URLS 로 정한다.
    """
    if not s3_key:
        return None
    settings = get_settings()
    if settings.presigned_read_urls and settings.s3_public_endpoint:
        url = storage.presigned_get_url(s3_key, settings.s3_bucket)
        if url:
            return url
    return f"/api/v1/images/{image_id}/source"


def _to_list_item(img: dict[str, Any]) -> ImageListItem:
    return ImageListItem(
        image_id=img["image_id"],
        file_name=img.get("file_name"),
        title=img.get("title", ""),
        summary=img.get("summary", ""),
        status=img.get("status") or "COMPLETED",
        favorite=img.get("favorite", False),
        thumbnail_url=_thumbnail_url(img["image_id"], img.get("s3_key")),
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


async def _owned_image(image_id: int, user_id: int) -> dict[str, Any] | None:
    """이미지를 읽고 요청자 소유인지 확인한다. 없거나 남의 것이면 None.

    imageId 하나로 문서를 찾는 경로(상세·썸네일·원본·업로드 완료·OCR 제출·URL
    재발급)는 전부 이걸 거친다. search.get_image 에는 사용자 필터가 없어서
    (목록·검색·집계에만 user_id term 이 걸려 있다) 여기서 대조하지 않으면
    imageId 를 바꿔가며 남의 원본 이미지와 OCR 원문을 그대로 가져갈 수 있다.

    FIXED_USER_ID 가 켜져 있는 동안은 전원이 같은 사용자라 항상 통과하지만,
    그 스위치를 끄는 순간 이 검사가 유일한 격리 장치가 된다.

    **없는 것과 남의 것을 구분해 주지 않는다**(둘 다 None → 404). 403 으로 갈라
    주면 "그 imageId 는 존재한다" 를 알려 주는 셈이라 남의 imageId 열거에 쓰인다.
    """
    found = await asyncio.to_thread(search.get_image, image_id)
    if found is None or found.get("user_id") != user_id:
        return None
    return found
