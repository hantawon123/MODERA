"""로컬 MinIO 에 버킷과 테스트 이미지를 만든다.

backend/local-infra 의 MinIO 는 버킷을 자동 생성하지 않는다. FULL 요청이
s3Key 로 원본을 읽으므로 미리 넣어 둬야 한다.

    python scripts/seed_minio.py                    # 기본 3장
    python scripts/seed_minio.py --count 4          # 동시성 테스트용 4장

.env.local 의 S3_* 값을 그대로 쓴다(환경변수로 덮어쓸 수 있다).
성공하면 만들어진 s3Key 를 한 줄씩 출력한다.
"""

import argparse
import io
import os
import sys

DEFAULT_ENDPOINT = "http://localhost:9002"


def _client(endpoint: str):
    import boto3
    from botocore.client import Config

    return boto3.client(
        "s3",
        endpoint_url=endpoint,
        aws_access_key_id=os.environ.get("S3_ACCESS_KEY", "minioadmin"),
        aws_secret_access_key=os.environ.get("S3_SECRET_KEY", "minioadmin"),
        region_name=os.environ.get("S3_REGION", "us-east-1"),
        config=Config(signature_version="s3v4", s3={"addressing_style": "path"}),
    )


def _ensure_bucket(client, name: str) -> None:
    try:
        client.head_bucket(Bucket=name)
        print(f"버킷 있음: {name}")
    except Exception:
        client.create_bucket(Bucket=name)
        print(f"버킷 생성: {name}")


def _make_png(index: int) -> bytes:
    """분석할 거리가 있는 텍스트를 그려 넣은 PNG 를 만든다.

    MOCK_AI=false 로 실제 Gemini 를 태울 때 OCR·비전이 읽을 내용이 있어야
    '정보성 있음' 판정이 나온다. 빈 이미지면 EMPTY 로만 떨어져 3단계가 안 돈다.
    """
    from PIL import Image, ImageDraw

    image = Image.new("RGB", (800, 1200), "white")
    draw = ImageDraw.Draw(image)
    lines = [
        "교보문고 온라인",
        "C++ 프로그래밍 입문 (개정 3판)",
        f"32,000원  ->  28,800원 (10% 할인)",
        "쿠폰: BOOK2026  /  2026-08-15 까지",
        "무료배송 · 내일 도착",
        f"[test image #{index}]",
    ]
    y = 80
    for line in lines:
        draw.text((60, y), line, fill="black")
        y += 60

    buffer = io.BytesIO()
    image.save(buffer, format="PNG")
    return buffer.getvalue()


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--endpoint",
                        default=os.environ.get("S3_ENDPOINT", DEFAULT_ENDPOINT))
    parser.add_argument("--bucket", default=os.environ.get("S3_BUCKET", "pictures"))
    parser.add_argument("--thumbnail-bucket",
                        default=os.environ.get("S3_THUMBNAIL_BUCKET", "thumbnails"))
    parser.add_argument("--user-id", type=int, default=1)
    parser.add_argument("--count", type=int, default=3)
    args = parser.parse_args()

    client = _client(args.endpoint)
    # 썸네일 버킷이 없으면 파이프라인 0단계가 매번 경고를 남긴다(분석은 계속되지만
    # 로그가 지저분해지고 6-6 썸네일 조회가 원본에서 매번 다시 만든다).
    _ensure_bucket(client, args.bucket)
    _ensure_bucket(client, args.thumbnail_bucket)

    keys = []
    for i in range(1, args.count + 1):
        key = f"u/{args.user_id}/local-test-{i}.png"
        client.put_object(Bucket=args.bucket, Key=key,
                          Body=_make_png(i), ContentType="image/png")
        keys.append(key)
        print(f"업로드: s3://{args.bucket}/{key}")

    print("\n--- s3Key ---")
    for key in keys:
        print(key)
    return 0


if __name__ == "__main__":
    sys.exit(main())
