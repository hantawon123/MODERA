"""환경변수 기반 설정.

API 키·내부 토큰은 기본값을 두지 않는다. 미설정 시 즉시 실패시켜
자격증명이 코드에 남는 일을 막는다.
"""

import os
from functools import lru_cache


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
        # Spring 연동 스위치. MVP 는 앱↔AI 직결이라 Spring 이 떠 있지 않은데,
        # 그 상태로 10-5 를 매 건 호출하면 연결 실패까지 약 4초를 그냥 버린다.
        # false 로 두면 조회를 시도하지 않고 기본 후보로 바로 진행한다.
        self.spring_enabled = os.environ.get("SPRING_ENABLED", "true").lower() == "true"
        # ponytail: Spring 완성 전까지의 임시 스위치. 지금은 Android 가 /api/v1 을
        # 직접 부르는데 앱에는 X-Internal-Token 을 넣을 수 없어 인증을 끈다.
        # Spring 이 앞단에 서면 APP_API_AUTH=true 로 되돌리고, 실제 로그인이
        # 붙으면 이 스위치와 require_internal_token 의 분기를 함께 지운다.
        # /internal/v1/* 은 이 값과 무관하게 항상 토큰을 요구한다.
        self.app_api_auth = os.environ.get("APP_API_AUTH", "false").lower() == "true"
        # Spring 내부 호출 타임아웃(초). Gemini 용 http_timeout(30) 과 분리한다.
        # 같은 네트워크 안의 내부 호출이라 길게 잡을 이유가 없다.
        self.spring_timeout = float(os.environ.get("SPRING_TIMEOUT", "3"))

        # 오브젝트 스토리지 (원본 이미지). MinIO 는 S3 호환이라 같은 클라이언트를 쓴다.
        # endpoint 를 지정하면 MinIO, 비우면 AWS S3 로 붙는다.
        self.s3_bucket = os.environ.get("S3_BUCKET", "")
        self.s3_endpoint = os.environ.get("S3_ENDPOINT", "")
        self.s3_access_key = os.environ.get("S3_ACCESS_KEY", "")
        self.s3_secret_key = os.environ.get("S3_SECRET_KEY", "")
        # MinIO 는 path-style 접근이 필요하다(버킷을 호스트명이 아닌 경로로 붙임).
        self.s3_path_style = os.environ.get("S3_PATH_STYLE", "true").lower() == "true"
        self.aws_region = os.environ.get("S3_REGION", os.environ.get("AWS_REGION", "us-east-1"))

        # 썸네일 최대 변(px). 0 이면 축소하지 않고 해상도를 원본 그대로 둔다
        # (정사각으로 모양만 맞추는 용도).
        self.thumbnail_max_size = int(os.environ.get("THUMBNAIL_MAX_SIZE", "0"))
        self.thumbnail_quality = int(os.environ.get("THUMBNAIL_QUALITY", "82"))
        # 썸네일 전용 버킷. 분석할 때 리사이즈본을 만들어 **원본과 같은 key** 로 올려 둔다.
        # (원본 pictures/u/1/a.png → 썸네일 thumbnail/u/1/a.png, 내용은 JPEG)
        # 비워 두면 저장하지 않고 요청 때마다 원본을 읽어 즉석 생성한다(기존 동작).
        self.s3_thumbnail_bucket = os.environ.get("S3_THUMBNAIL_BUCKET", "thumbnail")
        # 스토리지 호출 타임아웃(초)과 재시도 횟수.
        # 기본값(연결 60초 × 재시도 5회)이면 MinIO 가 잠깐 내려갔을 때 목록·썸네일
        # 요청이 몇 분씩 매달린다. 짧게 끊고 에러를 돌려주는 편이 낫다.
        self.s3_connect_timeout = float(os.environ.get("S3_CONNECT_TIMEOUT", "3"))
        self.s3_read_timeout = float(os.environ.get("S3_READ_TIMEOUT", "10"))
        self.s3_max_attempts = int(os.environ.get("S3_MAX_ATTEMPTS", "2"))

        # 업로드 허용 최대 크기(MB). 명세 4-1 fileSize 는 최대 5MB 다.
        self.max_upload_mb = int(os.environ.get("MAX_UPLOAD_MB", "5"))
        # presigned 업로드 URL 유효시간(초). 명세 4-1 uploadExpiresIn 예시가 600 이다.
        self.upload_url_expires_in = int(os.environ.get("UPLOAD_URL_EXPIRES_IN", "600"))
        # 앱에 건네줄 presigned URL 을 만들 때 쓰는 공개 주소.
        #
        # presigned URL 은 S3_ENDPOINT 값을 host 로 그대로 박아서 만들어진다. 그런데
        # 서버 내부용 주소(http://minio:9000)는 휴대폰에서 접근할 수 없다. 그래서
        # 읽기·쓰기는 내부 주소로 하고, 앱에 주는 URL 만 이 공개 주소로 만든다.
        # 비워 두면 S3_ENDPOINT 를 그대로 쓴다(로컬 개발·터널 환경).
        self.s3_public_endpoint = os.environ.get("S3_PUBLIC_ENDPOINT", "")

        # 조회용 이미지 주소를 presigned URL 로 줄지 여부.
        #
        # 업로드는 presigned 가 필수다(앱이 스토리지에 직접 올려야 하므로).
        # 하지만 조회까지 presigned 로 하면 만료 때문에 손해가 크다:
        #   - 서명이 매번 바뀌어 앱 이미지 캐시가 URL 기준이면 같은 사진을 계속 다시 받는다
        #   - 목록 응답을 캐시해 두고 나중에 열면 이미지가 깨진다
        # 그래서 기본은 만료 없는 서버 경유 경로(/thumbnail/raw, /source)다.
        # 이미지 트래픽을 서버에서 덜어내고 싶어지면 true 로 바꾼다.
        self.presigned_read_urls = (
            os.environ.get("PRESIGNED_READ_URLS", "false").lower() == "true"
        )

        # 썸네일을 정사각으로 만들지 여부. 카테고리 카드·격자 목록이 모두 정사각이라
        # 서버에서 잘라 두면 앱이 자를 필요가 없다. false 면 비율을 유지한다.
        self.thumbnail_square = (
            os.environ.get("THUMBNAIL_SQUARE", "true").lower() == "true"
        )

        # OpenSearch (검색 색인/조회 — FastAPI 전담, 한글 nori analyzer)
        self.opensearch_host = os.environ.get("OPENSEARCH_HOST", "localhost")
        self.opensearch_port = int(os.environ.get("OPENSEARCH_PORT", "9200"))
        self.opensearch_use_ssl = os.environ.get("OPENSEARCH_USE_SSL", "false").lower() == "true"
        self.opensearch_user = os.environ.get("OPENSEARCH_USER", "")
        self.opensearch_password = os.environ.get("OPENSEARCH_PASSWORD", "")
        self.opensearch_index = os.environ.get("OPENSEARCH_INDEX", "screenshot_kb")

        # 모델
        self.llm_model_name = os.environ.get("LLM_MODEL_NAME", "gemini-3.5-flash")
        self.vision_model_name = os.environ.get("VISION_MODEL_NAME", "gemini-3.5-flash")
        self.embedding_model_name = os.environ.get("EMBEDDING_MODEL_NAME", "gemini-embedding-2")
        # 임베딩 차원. 팀 합의로 768 고정이며 pgvector 컬럼(vector(768))과 일치해야 한다.
        # gemini-embedding-2 의 기본 출력은 3072 라 호출 시 명시적으로 줄여서 받는다.
        # pgvector 는 2000 초과 차원에 인덱스를 만들 수 없어 3072 는 쓰지 않는다.
        self.embedding_dim = int(os.environ.get("EMBEDDING_DIM", "768"))

        # 카테고리 신규 생성 임계값
        self.similarity_threshold = float(os.environ.get("SIMILARITY_THRESHOLD", "0.80"))

        # AGENT 태그 최대 개수 (10-1 options.maxTags 로 요청별 덮어쓰기 가능)
        self.default_max_tags = int(os.environ.get("DEFAULT_MAX_TAGS", "5"))

        # 로그인 미구현 구간용 고정 userId.
        # MVP 는 회원가입·로그인이 없어서 요청이 보내는 userId 를 신뢰할 수 없다.
        # 값이 있으면 요청 값을 무시하고 항상 이 값으로 처리한다. API 계약(userId 파라미터)은
        # 그대로 두므로, 로그인이 붙으면 0 으로 바꾸는 것만으로 실제 값 사용으로 돌아간다.
        self.fixed_user_id = int(os.environ.get("FIXED_USER_ID", "1"))

        # 외부 호출 타임아웃(초)
        self.http_timeout = float(os.environ.get("HTTP_TIMEOUT", "30"))

        # 동시 실행 단계 수 제한.
        # 수백 장이 한꺼번에 들어와도 Gemini 호출이 폭주(429)하지 않게 막는다.
        self.max_concurrent_stages = int(os.environ.get("MAX_CONCURRENT_STAGES", "4"))

        # Gemini 429(rate limit) 재시도. 지수 백오프 + 지터로 재시도한다.
        self.gemini_max_attempts = int(os.environ.get("GEMINI_MAX_ATTEMPTS", "5"))
        self.gemini_backoff_base = float(os.environ.get("GEMINI_BACKOFF_BASE", "1.0"))
        # Gemini 가 40초 이상 대기를 요구하는 경우가 있어 상한을 넉넉히 둔다.
        self.gemini_backoff_max = float(os.environ.get("GEMINI_BACKOFF_MAX", "60"))


@lru_cache(maxsize=1)
def get_settings() -> Settings:
    return Settings()
