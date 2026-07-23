"""내부 API 스키마 (명세 10-1 ~ 10-5).

파이썬 내부는 snake_case, 직렬화·역직렬화는 camelCase 별칭으로 처리한다.
Spring 과 주고받는 JSON 은 모두 camelCase 가 된다.
"""

from typing import Any, Literal

from pydantic import BaseModel, ConfigDict
from pydantic.alias_generators import to_camel


class CamelModel(BaseModel):
    model_config = ConfigDict(
        alias_generator=to_camel,
        populate_by_name=True,
        protected_namespaces=(),
    )


# ── 10-1 단계별 분석 실행 요청 ────────────────────────────────────────────
class OcrInput(CamelModel):
    raw_text: str = ""
    refined_text: str | None = None
    lang: str | None = None
    confidence: float | None = None


class ImageInput(CamelModel):
    s3_key: str
    width: int | None = None
    height: int | None = None


class ImageAnalysisInput(CamelModel):
    description: str = ""
    detected_texts: list[str] = []
    objects: list[str] = []


class AnalyzeInput(CamelModel):
    image: ImageInput | None = None
    ocr: OcrInput | None = None
    image_analysis: ImageAnalysisInput | None = None


class AnalyzeOptions(CamelModel):
    max_tags: int | None = None
    language: str | None = None


Stage = Literal["LLM", "IMAGE_ANALYSIS", "AGENT"]


class AnalyzeRequest(CamelModel):
    job_id: int
    image_id: int
    user_id: int
    stage: Stage
    input: AnalyzeInput = AnalyzeInput()
    options: AnalyzeOptions | None = None
    callback_url: str | None = None


class AnalyzeAccepted(CamelModel):
    job_id: int
    image_id: int
    stage: str
    accepted: bool
    status: str


# ── 10-2 텍스트 임베딩 생성 ───────────────────────────────────────────────
class EmbedRequest(CamelModel):
    texts: list[str]
    purpose: Literal["DOCUMENT", "QUERY"] = "DOCUMENT"


class EmbeddingItem(CamelModel):
    index: int
    vector: list[float]


class EmbedResponse(CamelModel):
    model: str
    model_version: str
    dimension: int
    embeddings: list[EmbeddingItem]


# ── 10-3 자연어 → 구조화 조건 변환 ────────────────────────────────────────
class QueryParseRequest(CamelModel):
    query: str
    now: str | None = None


class ParsedConditions(CamelModel):
    keywords: list[str] = []
    price_min: int | None = None
    price_max: int | None = None
    brand: str | None = None
    category_hints: list[str] = []
    date_from: str | None = None
    date_to: str | None = None
    expires_before: str | None = None


class QueryParseResponse(CamelModel):
    model_version: str
    parsed_conditions: ParsedConditions | None = None
    confidence: float = 0.0


# ── 10-4 분석 결과 콜백 (FastAPI → Spring) ────────────────────────────────
class CallbackError(CamelModel):
    code: str
    message: str
    retryable: bool = False


class CallbackRequest(CamelModel):
    job_id: int
    image_id: int
    stage: str
    status: Literal["COMPLETED", "FAILED", "EMPTY"]
    result: dict[str, Any] | None = None
    error: CallbackError | None = None
    model_version: str
    completed_at: str


# ── 10-5 사용자 지식 후보 (Spring → FastAPI 응답) ─────────────────────────
class TagCandidate(CamelModel):
    tag_id: int
    name: str
    usage_count: int = 0


class CategoryCandidate(CamelModel):
    # 아직 DB 에 없는 기본 카테고리 후보는 categoryId 가 없다(신규 생성 대상).
    category_id: int | None = None
    name: str
    usage_count: int = 0
    # Spring 이 카테고리 대표 벡터를 내려주면 centroid 매칭에 사용한다(선택).
    representative_vector: list[float] | None = None


class KnowledgeCandidates(CamelModel):
    user_id: int
    tags: list[TagCandidate] = []
    categories: list[CategoryCandidate] = []


# ── 검색 (OpenSearch 키워드/BM25) ─────────────────────────────────────────
class SearchRequest(CamelModel):
    user_id: int
    query: str
    category: str | None = None      # 카테고리명으로 필터 (선택)
    size: int = 10


class SearchHit(CamelModel):
    image_id: int
    title: str = ""
    summary: str = ""
    tags: list[str] = []
    category: str | None = None
    score: float = 0.0


class SearchResponse(CamelModel):
    total: int
    hits: list[SearchHit] = []


# ── 앱 API (Spring 우회 구간) ─────────────────────────────────────────────
# 팀 API 명세의 외부 API 형식을 따른다. Spring 이 복귀했을 때 앱이 응답 모델을
# 그대로 재사용할 수 있도록, 이 서비스가 채우지 못하는 값도 필드는 유지하고
# null 로 내려보낸다(예: favorite, fileName, structuredData).


class TagRef(CamelModel):
    tag_id: int
    name: str


class TagCount(CamelModel):
    tag_id: int
    name: str
    image_count: int


class CategoryRef(CamelModel):
    category_id: int
    name: str


class AppAnalyzeRequest(CamelModel):
    # 로그인 미구현 구간에서는 서버가 FIXED_USER_ID 로 덮어쓰므로 없어도 된다.
    # 필드 자체는 남겨 두어 로그인이 붙었을 때 앱을 고치지 않아도 되게 한다.
    user_id: int | None = None
    # 팀 명세 10-1 IMAGE_ANALYSIS 와 동일하게 오브젝트 키로 이미지를 지정한다.
    # presigned URL 은 만료가 있어 나중에 썸네일을 못 만들기 때문이다.
    image_id: int
    s3_key: str
    ocr: OcrInput = OcrInput()


class AppAnalyzeAccepted(CamelModel):
    image_id: int
    job_id: int
    stage: str
    status: str


class AnalysisJob(CamelModel):
    job_id: int
    image_id: int
    stage: str
    status: str
    file_name: str | None = None      # 미제공(Spring 영역)
    attempt: int = 1
    progress: int | None = None       # 미제공
    retryable: bool = False
    error_code: str | None = None
    updated_at: str | None = None


class AnalysisSummary(CamelModel):
    total: int
    stage_counts: dict[str, dict[str, int]] = {}
    overall_counts: dict[str, int] = {}


class ImageListItem(CamelModel):
    image_id: int
    file_name: str | None = None      # 미제공
    title: str = ""
    summary: str = ""
    status: str = "COMPLETED"
    favorite: bool | None = None      # 미제공
    thumbnail_url: str | None = None
    tags: list[TagRef] = []
    categories: list[CategoryRef] = []
    created_at: str | None = None


class ImageDetail(CamelModel):
    image_id: int
    file_name: str | None = None      # 미제공
    content_hash: str | None = None   # 미제공
    status: str = "COMPLETED"
    favorite: bool | None = None      # 미제공
    title: str = ""
    summary: str = ""
    ocr: OcrInput | None = None
    tags: list[TagRef] = []
    categories: list[CategoryRef] = []
    structured_data: dict[str, Any] | None = None   # MVP 제외
    analysis_confidence: float | None = None
    key_information: list[str] = []
    thumbnail_url: str | None = None
    created_at: str | None = None
    uploaded_at: str | None = None    # 미제공
    updated_at: str | None = None
    last_viewed_at: str | None = None  # 미제공


class CategoryCard(CamelModel):
    category_id: int
    name: str
    thumbnail_url: str | None = None
    image_count: int
    tags: list[TagCount] = []
    updated_at: str | None = None


class TagItem(CamelModel):
    tag_id: int
    name: str
    usage_count: int
    created_by: str = "AGENT"


class SearchResultItem(CamelModel):
    image_id: int
    title: str = ""
    summary: str = ""
    thumbnail_url: str | None = None
    matched_in: list[str] | None = None   # 미제공
    highlight: str | None = None          # 미제공
    score: float = 0.0
    tags: list[TagRef] = []
    last_viewed_at: str | None = None     # 미제공
    uploaded_at: str | None = None        # 미제공
    created_at: str | None = None


