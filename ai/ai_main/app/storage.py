"""S3 원본 이미지 조회.

서버에서는 모바일이 presigned URL 로
올린 S3 객체를 키(s3Key)로 받아 읽는다.
"""

import logging
from functools import lru_cache

from .config import get_settings

logger = logging.getLogger(__name__)


class ImageFetchError(RuntimeError):
    pass


@lru_cache(maxsize=1)
def _client():
    try:
        import boto3
    except ImportError as e:  # pragma: no cover
        raise ImageFetchError("boto3 가 설치되지 않았습니다.") from e
    return boto3.client("s3", region_name=get_settings().aws_region)


def fetch_image_bytes(s3_key: str) -> bytes:
    settings = get_settings()
    if not settings.s3_bucket:
        raise ImageFetchError("환경변수 S3_BUCKET 이 설정되지 않았습니다.")
    try:
        obj = _client().get_object(Bucket=settings.s3_bucket, Key=s3_key)
        return obj["Body"].read()
    except Exception as e:
        raise ImageFetchError(f"S3 객체를 읽지 못했습니다: {s3_key}") from e
