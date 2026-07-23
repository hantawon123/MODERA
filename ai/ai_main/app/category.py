"""카테고리 분류.

프로토타입의 전역 CategoryStore 를 대체한다. FastAPI 는 상태를 갖지 않고,
요청마다 Spring 10-5 로 받은 후보를 기준으로 판정한다.

대표 벡터 우선순위
  1. Spring 이 내려준 representativeVector (카테고리 소속 이미지들의 centroid)
  2. 없으면 카테고리 이름 임베딩 (콜드 스타트 대체)

2번은 이름만 비교하므로 1번보다 약하다. pgvector 에 카테고리 centroid 를
유지하고 10-5 응답에 실어주면 정확도가 올라간다(백엔드와 협의 필요).
"""

import logging
import math
import threading
import unicodedata
from dataclasses import dataclass, field
from typing import Callable

from .schemas import CategoryCandidate

logger = logging.getLogger(__name__)

# 카테고리 이름 임베딩 캐시.
#
# 대표 벡터가 없는 후보는 이름을 임베딩해서 대신 쓰는데, 이름 목록은 사용자당
# 사실상 고정이다(기본 17종). 캐시가 없으면 이미지 한 장마다 17회를 순차로
# 호출해 건당 Gemini 호출이 2회에서 19회로 뛴다. 이름→벡터는 모델이 같으면
# 항상 같은 값이므로 프로세스 수명 동안 재사용해도 안전하다.
# (임베딩 모델·차원을 바꾸면 컨테이너를 재기동해야 한다 — 설정이 프로세스
#  기동 시 고정되므로 실제로도 재기동이 강제된다.)
_name_vector_cache: dict[str, list[float]] = {}
_cache_lock = threading.Lock()


def embed_names_cached(
    names: list[str], embed_fn: Callable[[list[str]], list[list[float]]]
) -> dict[str, list[float]]:
    """이름 목록의 임베딩을 돌려준다. 캐시에 없는 것만 한 번에 임베딩한다."""
    keys = {name: normalize_name(name) for name in names}
    with _cache_lock:
        missing = [n for n in names if keys[n] not in _name_vector_cache]

    if missing:
        logger.info("카테고리 이름 임베딩 %s건 신규 계산 (캐시 %s건)",
                    len(missing), len(_name_vector_cache))
        vectors = embed_fn(missing)
        with _cache_lock:
            for name, vector in zip(missing, vectors):
                _name_vector_cache[keys[name]] = vector

    with _cache_lock:
        return {n: _name_vector_cache[keys[n]] for n in names if keys[n] in _name_vector_cache}


def clear_name_vector_cache() -> None:
    """테스트·운영 점검용. 캐시를 비운다."""
    with _cache_lock:
        _name_vector_cache.clear()


def cosine_similarity(a: list[float], b: list[float]) -> float:
    na = math.sqrt(sum(x * x for x in a))
    nb = math.sqrt(sum(y * y for y in b))
    if na == 0 or nb == 0:
        return 0.0
    return sum(x * y for x, y in zip(a, b)) / (na * nb)


def normalize_name(name: str) -> str:
    return unicodedata.normalize("NFKC", (name or "")).strip().lower()


@dataclass
class CategoryResolution:
    name: str
    category_id: int | None
    created: bool
    similarity: float
    matched_by: str
    ranking: list[tuple[float, str]] = field(default_factory=list)


def resolve_category(
    summary_embedding: list[float],
    proposed_name: str,
    candidates: list[CategoryCandidate],
    threshold: float,
    embed_fn: Callable[[list[str]], list[list[float]]],
) -> CategoryResolution:
    """AGENT 가 제안한 카테고리를 기존 후보와 연결하거나 신규로 판정한다."""
    proposed = (proposed_name or "기타").strip()

    # 1) AGENT 가 기존 후보 이름을 그대로 골랐으면 바로 연결
    target = normalize_name(proposed)
    for candidate in candidates:
        if target and normalize_name(candidate.name) == target:
            return CategoryResolution(
                name=candidate.name,
                category_id=candidate.category_id,
                # 아직 DB 에 없는 기본 카테고리라면 Spring 이 새로 만들어야 한다.
                created=candidate.category_id is None,
                similarity=1.0,
                matched_by="name",
                ranking=[(1.0, candidate.name)],
            )

    if not candidates:
        return CategoryResolution(
            name=proposed, category_id=None, created=True,
            similarity=0.0, matched_by="new(후보 없음)",
        )

    # 2) 대표 벡터와 요약 임베딩의 코사인 유사도로 판정
    missing = [c for c in candidates if not c.representative_vector]
    if missing:
        name_vectors = embed_names_cached([c.name for c in missing], embed_fn)
        for candidate in missing:
            vector = name_vectors.get(candidate.name)
            if vector:
                candidate.representative_vector = vector

    scored = [
        (cosine_similarity(summary_embedding, c.representative_vector or []), c)
        for c in candidates
    ]
    scored.sort(key=lambda item: item[0], reverse=True)
    best_similarity, best = scored[0]
    ranking = [(round(score, 3), c.name) for score, c in scored[:3]]

    if best_similarity >= threshold:
        return CategoryResolution(
            name=best.name, category_id=best.category_id,
            created=best.category_id is None,
            similarity=best_similarity, matched_by="embedding", ranking=ranking,
        )

    # 3) 임계값 미달 → 신규 카테고리 후보
    return CategoryResolution(
        name=proposed, category_id=None, created=True,
        similarity=best_similarity, matched_by="new", ranking=ranking,
    )
