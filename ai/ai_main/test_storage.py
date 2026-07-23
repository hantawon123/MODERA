"""storage 회귀 방지용 자체 점검. 네트워크·MinIO 없이 도는 것만 담는다.

    python test_storage.py
"""

import os

os.environ.setdefault("GEMINI_API_KEY", "test")
os.environ.setdefault("INTERNAL_TOKEN", "test")
os.environ.setdefault("S3_BUCKET", "pictures")
os.environ.setdefault("S3_ENDPOINT", "http://minio:9000")
os.environ.setdefault("S3_PUBLIC_ENDPOINT", "https://example.test/s3")
os.environ.setdefault("S3_ACCESS_KEY", "admin")
os.environ.setdefault("S3_SECRET_KEY", "secret")

from app import storage  # noqa: E402


def test_presigned_url_is_sigv4():
    """SigV2 로 돌아가면 앱이 붙이는 Content-Type 때문에 403 이 난다."""
    url, expires = storage.presigned_put_url("u/1/1.png")
    assert "X-Amz-Algorithm=AWS4-HMAC-SHA256" in url, f"SigV4 가 아니다: {url}"
    assert "AWSAccessKeyId=" not in url, f"SigV2 로 서명됐다: {url}"
    assert expires > 0


def test_presigned_url_uses_public_endpoint():
    """내부 주소로 서명되면 휴대폰이 접근할 수 없다."""
    url, _ = storage.presigned_put_url("u/1/1.png")
    assert url.startswith("https://example.test/s3/pictures/u/1/1.png"), url


def test_not_found_is_distinguished_from_outage():
    """'객체 없음'과 스토리지 장애를 구분해야 4-2 의 409 를 신뢰할 수 있다."""
    not_found = Exception()
    not_found.response = {"Error": {"Code": "NoSuchKey"},
                          "ResponseMetadata": {"HTTPStatusCode": 404}}
    assert storage._is_not_found(not_found)

    outage = Exception("Could not connect to the endpoint URL")
    assert not storage._is_not_found(outage)

    denied = Exception()
    denied.response = {"Error": {"Code": "AccessDenied"},
                       "ResponseMetadata": {"HTTPStatusCode": 403}}
    assert not storage._is_not_found(denied)


def test_error_message_keeps_cause():
    """원인이 사라지면 응답만 보고 연결 실패인지 파일 없음인지 알 수 없다."""
    msg = storage._why("S3 객체를 읽지 못했습니다", "u/1/1.png", KeyError("NoSuchKey"))
    assert "u/1/1.png" in msg
    assert "KeyError" in msg


if __name__ == "__main__":
    for name, fn in sorted(globals().items()):
        if name.startswith("test_"):
            fn()
            print("ok", name)
    print("모두 통과")
