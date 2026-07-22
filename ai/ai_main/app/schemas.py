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
