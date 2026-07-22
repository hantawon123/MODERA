"""환경변수 기반 설정.

API 키·내부 토큰은 기본값을 두지 않는다. 미설정 시 즉시 실패시켜
자격증명이 코드에 남는 일을 막는다.
"""

import os
from functools import lru_cache

from dotenv import load_dotenv

# 로컬 개발 편의: .env 가 있으면 읽는다.
# 운영(Docker)은 컨테이너 환경변수를 쓰므로 파일이 없어도 그대로 동작한다.
# 이미 설정된 환경변수를 덮어쓰지 않는다(override=False 가 기본).
load_dotenv()


def _required(name: str) -> str:
    value = os.environ.get(name)
    if not value:
        raise RuntimeError(f"환경변수 {name} 가 설정되지 않았습니다.")
    return value


class Settings:
    def __init__(self) -> None:
        # 자격증명 (기본값 없음)
        self.gemini_api_key = _required("GEMINI_API_KEY")
        self.internal_token = _required("INTERNAL_TOKEN")

        # Spring 내부 API (10-4 콜백, 10-5 후보 조회)
        self.spring_base_url = os.environ.get("SPRING_BASE_URL", "http://spring-api:8080")

        # S3 (IMAGE_ANALYSIS 원본 이미지)
        self.s3_bucket = os.environ.get("S3_BUCKET", "")
        self.aws_region = os.environ.get("AWS_REGION", "ap-northeast-2")

        # 모델
        self.llm_model_name = os.environ.get("LLM_MODEL_NAME", "gemini-3.5-flash")
        self.vision_model_name = os.environ.get("VISION_MODEL_NAME", "gemini-3.5-flash")
        self.embedding_model_name = os.environ.get("EMBEDDING_MODEL_NAME", "gemini-embedding-2")

        # 카테고리 신규 생성 임계값
        self.similarity_threshold = float(os.environ.get("SIMILARITY_THRESHOLD", "0.80"))

        # AGENT 태그 최대 개수 (10-1 options.maxTags 로 요청별 덮어쓰기 가능)
        self.default_max_tags = int(os.environ.get("DEFAULT_MAX_TAGS", "10"))

        # 외부 호출 타임아웃(초)
        self.http_timeout = float(os.environ.get("HTTP_TIMEOUT", "30"))


@lru_cache(maxsize=1)
def get_settings() -> Settings:
    return Settings()
