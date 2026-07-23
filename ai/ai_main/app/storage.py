"""원본 이미지 조회.

두 가지 경로를 지원한다.
  - http(s) URL  : presigned GET URL / 공개 URL 을 그대로 내려받는다(자격증명 불필요).
  - s3Key        : boto3 로 버킷에서 읽는다(자격증명 필요).

앱이 S3 에 직접 업로드한 뒤 주소만 넘겨주는 흐름이라 URL 경로를 기본으로 쓴다.
"""

import logging
from functools import lru_cache

import httpx

from .config import get_settings

logger = logging.getLogger(__name__)


class ImageFetchError(RuntimeError):
    pass


def _build_client(endpoint: str):
    try:
        import boto3
        from botocore.client import Config
    except ImportError as e:  # pragma: no cover
        raise ImageFetchError("boto3 가 설치되지 않았습니다.") from e

    settings = get_settings()
    kwargs: dict = {"region_name": settings.aws_region}
    if endpoint:
        # MinIO 등 S3 호환 스토리지. path-style 로 붙어야 버킷이 경로로 해석된다.
        kwargs["endpoint_url"] = endpoint
        kwargs["config"] = Config(s3={"addressing_style": "path"}) \
            if settings.s3_path_style else Config()
    if settings.s3_access_key:
        kwargs["aws_access_key_id"] = settings.s3_access_key
        kwargs["aws_secret_access_key"] = settings.s3_secret_key
    return boto3.client("s3", **kwargs)


@lru_cache(maxsize=1)
def _client():
    """서버가 직접 읽고 쓸 때 쓰는 클라이언트(내부 주소)."""
    return _build_client(get_settings().s3_endpoint)


@lru_cache(maxsize=1)
def _public_client():
    """앱에 줄 presigned URL 을 만들 때만 쓰는 클라이언트(공개 주소).

    서명은 이 클라이언트의 endpoint 로 만들어지므로, 앱이 실제로 접근할 수 있는
    주소여야 한다. S3_PUBLIC_ENDPOINT 가 없으면 내부 주소를 그대로 쓴다.
    """
    settings = get_settings()
    return _build_client(settings.s3_public_endpoint or settings.s3_endpoint)


def fetch_image_bytes(s3_key: str) -> bytes:
    settings = get_settings()
    if not settings.s3_bucket:
        raise ImageFetchError("환경변수 S3_BUCKET 이 설정되지 않았습니다.")
    try:
        obj = _client().get_object(Bucket=settings.s3_bucket, Key=s3_key)
        return obj["Body"].read()
    except Exception as e:
        raise ImageFetchError(f"S3 객체를 읽지 못했습니다: {s3_key}") from e


def put_image(key: str, data: bytes, content_type: str = "application/octet-stream") -> None:
    """원본 이미지를 버킷에 올린다.

    원래는 앱이 Spring 4-1 이 발급한 presigned URL 로 직접 올리는 구조다.
    Spring 우회 구간에서는 그 경로가 없어 AI 가 대신 받아 준다.
    """
    settings = get_settings()
    if not settings.s3_bucket:
        raise ImageFetchError("환경변수 S3_BUCKET 이 설정되지 않았습니다.")
    try:
        _client().put_object(
            Bucket=settings.s3_bucket, Key=key, Body=data, ContentType=content_type
        )
    except Exception as e:
        raise ImageFetchError(f"이미지를 저장하지 못했습니다: {key}") from e


def presigned_put_url(key: str, expires_in: int | None = None) -> tuple[str, int]:
    """앱이 원본을 직접 올릴 presigned PUT URL 을 발급한다(명세 4-1·4-5).

    (url, expiresIn) 을 돌려준다. 이 URL 로는 AI 서버를 거치지 않고
    앱 → 스토리지로 바이너리가 바로 간다.
    """
    settings = get_settings()
    if not settings.s3_bucket:
        raise ImageFetchError("환경변수 S3_BUCKET 이 설정되지 않았습니다.")
    expires = expires_in or settings.upload_url_expires_in
    try:
        url = _public_client().generate_presigned_url(
            "put_object",
            Params={"Bucket": settings.s3_bucket, "Key": key},
            ExpiresIn=expires,
        )
    except Exception as e:
        raise ImageFetchError(f"업로드 URL 을 발급하지 못했습니다: {key}") from e
    return url, expires


def object_exists(key: str) -> bool:
    """원본이 실제로 올라왔는지 확인한다(명세 4-2 UPLOAD_NOT_FOUND 판정용)."""
    settings = get_settings()
    try:
        _client().head_object(Bucket=settings.s3_bucket, Key=key)
        return True
    except Exception:
        return False


def fetch_image_bytes_from_url(url: str) -> bytes:
    """presigned/공개 URL 에서 이미지를 내려받는다."""
    settings = get_settings()
    try:
        response = httpx.get(url, timeout=settings.http_timeout, follow_redirects=True)
        response.raise_for_status()
        return response.content
    except Exception as e:
        raise ImageFetchError(f"이미지 URL 을 읽지 못했습니다: {url[:120]}") from e


def fetch_image(image_ref: str) -> bytes:
    """http(s) 주소면 직접 내려받고, 그 외에는 s3Key 로 간주해 버킷에서 읽는다."""
    if image_ref.startswith("http://") or image_ref.startswith("https://"):
        return fetch_image_bytes_from_url(image_ref)
    return fetch_image_bytes(image_ref)


def make_thumbnail(image_bytes: bytes) -> bytes:
    """목록·카테고리 카드에 쓸 썸네일을 만든다. 사진 1장당 1개다.

    THUMBNAIL_SQUARE=true(기본)면 가운데를 정사각으로 잘라낸다. 카드와 격자 목록이
    모두 정사각이라 여기서 모양을 맞춰 두면 앱이 자를 필요가 없다.
    false 면 원본 비율을 그대로 둔다.

    THUMBNAIL_MAX_SIZE 는 축소 상한이다. 0(기본)이면 축소하지 않고 해상도를 원본
    그대로 둔다 — 모양만 정사각으로 맞추는 용도. 값을 주면 그 변까지 줄인다.
    어느 쪽이든 확대는 하지 않는다.
    """
    try:
        from PIL import Image
    except ImportError as e:  # pragma: no cover
        raise ImageFetchError("pillow 가 설치되지 않았습니다.") from e

    import io

    settings = get_settings()
    limit = settings.thumbnail_max_size
    with Image.open(io.BytesIO(image_bytes)) as image:
        image = image.convert("RGB")

        if settings.thumbnail_square:
            width, height = image.size
            side = min(width, height)
            left, top = (width - side) // 2, (height - side) // 2
            image = image.crop((left, top, left + side, top + side))
            if 0 < limit < side:
                image = image.resize((limit, limit), Image.LANCZOS)
        elif limit > 0:
            image.thumbnail((limit, limit))

        buffer = io.BytesIO()
        image.save(buffer, format="JPEG", quality=settings.thumbnail_quality)
        return buffer.getvalue()


# ── 썸네일 버킷 ───────────────────────────────────────────────────────────
# 원본 버킷과 같은 key 를 쓴다. 별도 매핑 테이블이 필요 없고, 백엔드·앱도
# "같은 key, 버킷만 thumbnail" 규칙만 알면 직접 읽을 수 있다.
# 내용은 항상 JPEG 이므로 확장자가 .png 여도 ContentType 으로 구분한다.


def put_thumbnail(key: str, data: bytes) -> None:
    settings = get_settings()
    if not settings.s3_thumbnail_bucket:
        raise ImageFetchError("환경변수 S3_THUMBNAIL_BUCKET 이 설정되지 않았습니다.")
    try:
        _client().put_object(
            Bucket=settings.s3_thumbnail_bucket,
            Key=key,
            Body=data,
            ContentType="image/jpeg",
            CacheControl="public, max-age=86400",
        )
    except Exception as e:
        raise ImageFetchError(f"썸네일을 저장하지 못했습니다: {key}") from e


def fetch_thumbnail(key: str) -> bytes:
    settings = get_settings()
    if not settings.s3_thumbnail_bucket:
        raise ImageFetchError("환경변수 S3_THUMBNAIL_BUCKET 이 설정되지 않았습니다.")
    try:
        obj = _client().get_object(Bucket=settings.s3_thumbnail_bucket, Key=key)
        return obj["Body"].read()
    except Exception as e:
        raise ImageFetchError(f"썸네일 객체를 읽지 못했습니다: {key}") from e


def store_thumbnail(image_ref: str, image_bytes: bytes | None = None) -> bytes:
    """원본을 읽어 썸네일을 만들고 썸네일 버킷에 올린다. 만든 바이트를 돌려준다.

    image_bytes 를 넘기면 원본을 다시 내려받지 않는다(분석 때 이미 읽어둔 것 재사용).
    image_ref 가 http(s) URL 이면 저장할 key 를 정할 수 없으므로 생성만 하고 넘어간다.
    """
    raw = image_bytes if image_bytes is not None else fetch_image(image_ref)
    thumb = make_thumbnail(raw)
    settings = get_settings()
    if settings.s3_thumbnail_bucket and not image_ref.startswith(("http://", "https://")):
        # 저장 실패는 치명적이지 않다. 썸네일 자체는 이미 만들었으므로 그대로 돌려주고
        # (앱은 정상적으로 이미지를 본다) 다음 요청 때 다시 시도한다.
        try:
            put_thumbnail(image_ref, thumb)
            logger.info("썸네일 저장 bucket=%s key=%s (%s bytes)",
                        settings.s3_thumbnail_bucket, image_ref, len(thumb))
        except Exception as e:
            logger.warning("썸네일 저장 실패 key=%s: %s (생성본은 그대로 사용)", image_ref, e)
    return thumb


