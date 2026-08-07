"""검색용 로컬 임베딩 (bge-m3).

카테고리 판정용 gemini-embedding-2(768, pgvector)와는 **완전히 별개**다.
이 모듈은 OpenSearch `knn_vector` 필드에 넣을 **검색 전용** 벡터만 만든다.
질의·문서를 같은 모델로 임베딩해야 kNN 비교가 성립한다.

설계 원칙:
- **프로세스당 1회 로드(싱글톤).** 색인·검색마다 모델을 다시 올리지 않는다.
- **best-effort.** 모델 로드나 임베딩이 실패하면 None 을 돌려, 호출부가 BM25 로
  폴백하게 한다. 검색 기능 자체가 죽지 않도록 하는 발표 안전장치다.
- torch/sentence-transformers 의존성을 이 파일에만 가둔다.
"""

from __future__ import annotations

import logging
import threading

from .config import get_settings

logger = logging.getLogger(__name__)

_model = None
_load_lock = threading.Lock()
_load_failed = False


def _get_model():
    """모델을 반환한다. 로드 실패가 한 번 나면 이후로는 곧장 None(재시도 안 함)."""
    global _model, _load_failed
    if _model is not None:
        return _model
    if _load_failed:
        return None
    with _load_lock:
        if _model is not None:
            return _model
        if _load_failed:
            return None
        settings = get_settings()
        name = settings.search_embedding_model
        try:
            # 무거운 import 라 실제 로드 시점까지 미룬다.
            from sentence_transformers import SentenceTransformer

            logger.info("검색 임베딩 모델 로드 시작: %s (cpu)", name)
            model = SentenceTransformer(name, device="cpu")
            # sentence-transformers 5.x 에서 get_embedding_dimension 으로 개명됨.
            # 구버전(>=3.0) 호환 위해 폴백을 둔다.
            get_dim = getattr(model, "get_embedding_dimension", None) \
                or model.get_sentence_embedding_dimension
            dim = get_dim()
            if dim != settings.search_embedding_dim:
                # 매핑 차원과 어긋나면 색인이 조용히 깨진다. 여기서 끊는다.
                raise ValueError(
                    f"임베딩 차원 불일치: 모델 {dim} != 설정 {settings.search_embedding_dim} "
                    f"(SEARCH_EMBEDDING_DIM 또는 모델을 맞추세요)"
                )
            _model = model
            logger.info("검색 임베딩 모델 로드 완료: %s (dim=%d)", name, dim)
        except Exception as e:
            _load_failed = True
            logger.warning("검색 임베딩 모델 로드 실패 → 검색은 BM25 로 폴백: %s", e)
            return None
    return _model


def embed(text: str) -> list[float] | None:
    """텍스트 하나를 검색용 벡터로 임베딩한다.

    실패(빈 텍스트·모델 없음·추론 오류)하면 None 을 돌려 호출부가 BM25 로 폴백한다.
    bge-m3 는 질의/문서 프리픽스가 필요 없으므로 색인·검색 양쪽이 이 함수를 함께 쓴다.
    normalize 해서 cosinesimil(내적) 과 맞춘다.
    """
    text = (text or "").strip()
    if not text:
        return None
    model = _get_model()
    if model is None:
        return None
    try:
        vec = model.encode(text, normalize_embeddings=True)
        return vec.tolist()
    except Exception as e:
        logger.warning("검색 임베딩 실패(텍스트 %d자): %s", len(text), e)
        return None


def is_ready() -> bool:
    """모델이 로드 가능한 상태인지. 헬스체크·워밍업 용도."""
    return _get_model() is not None
