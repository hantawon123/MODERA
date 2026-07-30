"""카테고리 분류.

FastAPI 는 판정 상태를 갖지 않는다. 요청마다 후보와 그 대표 벡터를 받아 판정한다.

대표 벡터 우선순위 (채우는 쪽은 stages._build_candidates)
  1. Spring 이 내려준 representativeVector
  2. 이 서버의 사용자 centroid (search 카테고리 인덱스, 그 카테고리 이미지 요약 평균)
  3. 전역 시드 (카테고리 이름 임베딩 — 이미지가 0장일 때만)

세 개 다 없는 후보는 코사인이 0.0 이 되어 벡터로는 못 뽑힌다(이름 완전일치로만
잡힌다). 벡터 확보는 저장소 몫이고 여기서 임베딩을 부르지 않는다 — 판정 함수가
외부 호출을 하면 Gemini 장애가 곧 분류 실패가 된다.
"""

import logging
import math
import unicodedata
from dataclasses import dataclass, field

from .schemas import CategoryCandidate

logger = logging.getLogger(__name__)


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
) -> CategoryResolution:
    """AGENT 가 제안한 카테고리를 기존 후보와 연결하거나 신규로 판정한다."""
    proposed = (proposed_name or "기타").strip()

    # 1) AGENT 가 기존 후보 이름을 그대로 골랐으면 바로 연결
    target = normalize_name(proposed)
    for candidate in candidates:
        if target and normalize_name(candidate.name) == target:
            # 벡터 감사: 판정(연결)은 이름이 하되, 대표 벡터가 있으면 실제
            # 코사인을 재서 similarity 로 보고한다. AGENT 가 확신 있게 틀리는
            # 이름 일치(동의서→금융 실측)가 유일한 무검증 경로였는데, 실값을
            # 내보내면 "AGENT 와 벡터가 불일치한 판정"을 밖에서 관측할 수 있고
            # 오염 가드(stages)의 판단 근거가 된다. 벡터가 없으면(첫 이미지 등
            # 콜드 스타트) 잴 수 없으므로 기존대로 1.0 이다.
            audited = (
                cosine_similarity(summary_embedding, candidate.representative_vector)
                if candidate.representative_vector else 1.0
            )
            return CategoryResolution(
                name=candidate.name,
                category_id=candidate.category_id,
                # 아직 DB 에 없는 기본 카테고리라면 Spring 이 새로 만들어야 한다.
                created=candidate.category_id is None,
                similarity=audited,
                matched_by="name",
                ranking=[(round(audited, 3), candidate.name)],
            )

    if not candidates:
        return CategoryResolution(
            name=proposed, category_id=None, created=True,
            similarity=0.0, matched_by="new(후보 없음)",
        )

    # 2) 대표 벡터와 요약 임베딩의 코사인 유사도로 판정
    #    벡터 없는 후보는 0.0 이 되어 사실상 탈락한다. 조용히 넘기면 "왜 매번
    #    새 카테고리가 생기는가" 를 추적할 수 없으므로 이름을 남긴다.
    missing = [c.name for c in candidates if not c.representative_vector]
    if missing:
        logger.warning("대표 벡터 없는 카테고리 후보 %s건 — 벡터 판정에서 제외: %s",
                       len(missing), missing[:10])

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
