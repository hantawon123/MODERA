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


def _why(what: str, key: str, e: Exception) -> str:
    """원인 예외를 메시지에 남긴다.

    래퍼 메시지만 남기면 호출 측이 str(e) 를 응답에 실을 때 '객체 없음'인지
    '연결 실패'인지 구분할 수 없다. 클래스명과 원문을 함께 붙인다.
    """
    return f"{what}: {key} ({e.__class__.__name__}: {e})"


def _build_client(endpoint: str):
    try:
        import boto3
        from botocore.client import Config
    except ImportError as e:  # pragma: no cover
        raise ImageFetchError("boto3 가 설치되지 않았습니다.") from e

    settings = get_settings()
    # 스토리지가 죽었을 때 요청이 무한정 매달리지 않게 타임아웃을 건다.
    # 기본값(연결 60초 × 재시도)이면 MinIO 가 잠깐 내려가도 앱 화면이 통째로 멈춘다.
    common = {
        "connect_timeout": settings.s3_connect_timeout,
        "read_timeout": settings.s3_read_timeout,
        "retries": {"max_attempts": settings.s3_max_attempts, "mode": "standard"},
        # SigV4 를 명시한다. 지정하지 않으면 botocore 가 presigned URL 을 SigV2
        # (?AWSAccessKeyId=...&Signature=...) 로 만드는데, SigV2 는 Content-Type 까지
        # 서명 대상이라 발급 시점(빈 값)과 다르면 403 이 난다. OkHttp 는 RequestBody 에
        # MediaType 이 필수라 항상 Content-Type 을 붙이므로 앱 업로드가 전부 깨진다.
        # SigV4 는 host 만 서명하므로 Content-Type 을 자유롭게 붙일 수 있다.
        "signature_version": "s3v4",
    }
    kwargs: dict = {"region_name": settings.aws_region}
    if endpoint:
        # MinIO 등 S3 호환 스토리지. path-style 로 붙어야 버킷이 경로로 해석된다.
        kwargs["endpoint_url"] = endpoint
        if settings.s3_path_style:
            common["s3"] = {"addressing_style": "path"}
    kwargs["config"] = Config(**common)
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
        raise ImageFetchError(_why("S3 객체를 읽지 못했습니다", s3_key, e)) from e


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
        raise ImageFetchError(_why("이미지를 저장하지 못했습니다", key, e)) from e


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
        raise ImageFetchError(_why("업로드 URL 을 발급하지 못했습니다", key, e)) from e
    return url, expires


def presigned_get_url(
    key: str, bucket: str | None = None, expires_in: int = 3600
) -> str | None:
    """앱이 이미지를 직접 받을 presigned GET URL 을 발급한다.

    S3_PUBLIC_ENDPOINT 가 없으면 앱이 닿을 수 없는 주소가 만들어지므로 None 을
    돌려주고, 호출 측이 서버 경유 경로로 대체하게 한다.
    """
    settings = get_settings()
    if not settings.s3_public_endpoint:
        return None
    try:
        return _public_client().generate_presigned_url(
            "get_object",
            Params={"Bucket": bucket or settings.s3_bucket, "Key": key},
            ExpiresIn=expires_in,
        )
    except Exception as e:
        logger.warning("조회 URL 발급 실패 key=%s: %s", key, e)
        return None


def _is_not_found(e: Exception) -> bool:
    """S3 예외가 '객체 없음'인지 판별한다. 연결 실패·권한 오류와 구분하기 위한 것."""
    status = getattr(e, "response", {}).get("ResponseMetadata", {}).get("HTTPStatusCode")
    code = getattr(e, "response", {}).get("Error", {}).get("Code")
    return status == 404 or code in ("404", "NoSuchKey", "NotFound", "NoSuchBucket")


def object_exists(key: str, bucket: str | None = None) -> bool:
    """객체가 실제로 있는지 확인한다(명세 4-2 UPLOAD_NOT_FOUND 판정용).

    bucket 을 주면 그 버킷을, 비우면 원본 버킷을 본다.

    응답(False)은 그대로 두되, '객체 없음'과 스토리지 장애를 로그에서 구분한다.
    둘 다 False 로 삼키면 MinIO 가 죽은 것과 사용자가 업로드를 안 한 것이 똑같이
    UPLOAD_NOT_FOUND 로 보여서 원인을 못 찾는다.
    """
    settings = get_settings()
    target = bucket or settings.s3_bucket
    try:
        _client().head_object(Bucket=target, Key=key)
        return True
    except Exception as e:
        if _is_not_found(e):
            logger.info("객체 없음 bucket=%s key=%s", target, key)
        else:
            # ponytail: 응답은 여전히 409 UPLOAD_NOT_FOUND 다. 장애를 별도 코드로
            # 구분해 주려면 여기서 예외를 올리고 4-2 에서 503 으로 매핑할 것.
            logger.warning("객체 확인 실패(스토리지 장애) bucket=%s key=%s: %s: %s",
                           target, key, e.__class__.__name__, e)
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


def make_thumbnail(image_bytes: bytes, focus_y: float | None = None) -> bytes:
    """목록·카테고리 카드에 쓸 썸네일을 만든다. 사진 1장당 1개다.

    THUMBNAIL_SQUARE=true(기본)면 정사각으로 잘라내되, **색채감(colorfulness)이
    가장 높은 위치**를 골라 자른다(+엔트로피는 동점 보조). 처음엔 엔트로피만
    썼는데, 엔트로피는 빽빽한 텍스트 벽이 최고점이라 상품·음식 사진 대신 설명
    문단이 썸네일이 되는 문제가 실측됐다(2026-07-31 실사진 111장 검수). 흑백
    텍스트는 색채감이 0 에 가깝고 사진·그림 영역이 높으므로 "이미지의 시각적
    핵심"이 남는다. 전부 텍스트인 문서는 어느 창이든 비슷해 엔트로피 보조가
    결정한다. 카드와 격자 목록이 모두 정사각이라 여기서 모양을 맞춰 두면 앱이
    자를 필요가 없다. false 면 원본 비율을 그대로 둔다.

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
            span = max(width, height) - side
            if span:
                # 정사각 윈도우를 긴 축을 따라 슬라이드하며 점수 최대인 곳을 자른다.
                # 9지점 샘플링 휴리스틱 — 부족하면 saliency 모델로 교체
                def _box(offset: int) -> tuple[int, int, int, int]:
                    if width > height:
                        return (offset, 0, offset + side, side)
                    return (0, offset, side, offset + side)

                def _score(offset: int) -> float:
                    # 색채감(Hasler-Süsstrunk 근사) 주점수 + 엔트로피 보조.
                    # 64px 로 줄여 순수 파이썬으로 계산해도 창당 수 ms 다.
                    crop = image.crop(_box(offset))
                    small = crop.resize((64, 64))
                    px = list(small.getdata())
                    n = len(px)
                    rg = [r - g for r, g, b in px]
                    yb = [(r + g) / 2 - b for r, g, b in px]
                    mrg = sum(rg) / n
                    myb = sum(yb) / n
                    var = (sum((v - mrg) ** 2 for v in rg) / n
                           + sum((v - myb) ** 2 for v in yb) / n)
                    colorfulness = var ** 0.5 + 0.3 * (mrg * mrg + myb * myb) ** 0.5
                    return colorfulness + 2.0 * crop.entropy()

                if focus_y is not None:
                    # 융합 AGENT 가 이미지를 직접 보고 짚어준 시각적 핵심 위치(0~1).
                    # 휴리스틱보다 우선한다 — 쇼핑 화면처럼 상품 사진(흰 배경)보다
                    # 가격·버튼 UI 가 더 화려한 경우 색채감 점수가 역선택하는 것을
                    # 모델의 장면 이해로 대체한다(2026-07-31 실측).
                    long_side = max(width, height)
                    clamped = min(1.0, max(0.0, focus_y))
                    best = min(span, max(0, round(clamped * long_side - side / 2)))
                else:
                    best = max(range(0, span + 1, max(1, span // 8)), key=_score)
                image = image.crop(_box(best))
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
        raise ImageFetchError(_why("썸네일을 저장하지 못했습니다", key, e)) from e


def fetch_thumbnail(key: str) -> bytes:
    settings = get_settings()
    if not settings.s3_thumbnail_bucket:
        raise ImageFetchError("환경변수 S3_THUMBNAIL_BUCKET 이 설정되지 않았습니다.")
    try:
        obj = _client().get_object(Bucket=settings.s3_thumbnail_bucket, Key=key)
        return obj["Body"].read()
    except Exception as e:
        raise ImageFetchError(_why("썸네일 객체를 읽지 못했습니다", key, e)) from e


# ── 카테고리 아이콘 버킷 ──────────────────────────────────────────────────
# key 는 "{categoryId}.png" 하나뿐이다(app/category_icon.py). 사진 썸네일과
# 섞지 않는 이유: 수명(카테고리가 살아 있는 동안)과 개수(카테고리 수)가 달라
# 라이프사이클 정책·용량 산정을 따로 걸어야 한다. 내용은 항상 PNG(알파 보존).


def put_category_icon(key: str, data: bytes) -> None:
    settings = get_settings()
    if not settings.s3_category_icon_bucket:
        raise ImageFetchError("환경변수 S3_CATEGORY_ICON_BUCKET 이 설정되지 않았습니다.")
    try:
        _client().put_object(
            Bucket=settings.s3_category_icon_bucket,
            Key=key,
            Body=data,
            ContentType="image/png",
            # 카테고리당 1장이고 내용이 바뀌지 않는다 — 길게 캐시해도 안전하다.
            CacheControl="public, max-age=604800",
        )
    except Exception as e:
        raise ImageFetchError(_why("카테고리 아이콘을 저장하지 못했습니다", key, e)) from e


def fetch_category_icon(key: str) -> bytes:
    settings = get_settings()
    if not settings.s3_category_icon_bucket:
        raise ImageFetchError("환경변수 S3_CATEGORY_ICON_BUCKET 이 설정되지 않았습니다.")
    try:
        obj = _client().get_object(Bucket=settings.s3_category_icon_bucket, Key=key)
        return obj["Body"].read()
    except Exception as e:
        raise ImageFetchError(_why("카테고리 아이콘을 읽지 못했습니다", key, e)) from e


def store_thumbnail(image_ref: str, image_bytes: bytes | None = None,
                    focus_y: float | None = None) -> bytes:
    """원본을 읽어 썸네일을 만들고 썸네일 버킷에 올린다. 만든 바이트를 돌려준다.

    image_bytes 를 넘기면 원본을 다시 내려받지 않는다(분석 때 이미 읽어둔 것 재사용).
    image_ref 가 http(s) URL 이면 저장할 key 를 정할 수 없으므로 생성만 하고 넘어간다.
    """
    raw = image_bytes if image_bytes is not None else fetch_image(image_ref)
    thumb = make_thumbnail(raw, focus_y)
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


