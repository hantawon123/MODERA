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
    name_dup_threshold: float,
    name_vectors: dict[str, list[float]] | None = None,
    proposed_name_vector: list[float] | None = None,
) -> CategoryResolution:
    """AGENT 가 제안한 카테고리를 기존 후보와 연결하거나 신규로 판정한다.

    신규 제안의 흡수 여부는 이름↔이름 임베딩으로 판정한다(name_vectors 는
    정규화된 후보 이름 → 이름 임베딩, 호출자가 준비). 예전의 요약↔centroid
    관문은 주제가 아니라 문장 스타일 유사도를 재는 바람에 정당한 신규 제안을
    전부 흡수했다(엣지 40장 실측 — 부동산·뷰티·자동차 10건 전멸). 이 함수는
    외부 호출을 하지 않는다 — 임베딩은 호출자가 요약 임베딩 배치에 실어 온다.
    """
    proposed = (proposed_name or "기타").strip()
    name_vectors = name_vectors or {}

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

    # 2a) 표기 변형 — 한쪽 이름이 다른 쪽을 포함하면 같은 카테고리의 변형으로
    #     본다(메이플↔메이플스토리, 쇼핑↔쇼핑몰, 일정↔일정관리). A/A′ 파편화의
    #     대부분이 이 꼴이고, 문자열 규칙이라 어휘가 늘어도 기준이 흔들리지
    #     않는다(임베딩 코사인은 어휘 92종 실측에서 경계가 무너졌다 — 아래 참조).
    if len(target) >= 2:
        for candidate in candidates:
            cn = normalize_name(candidate.name)
            if len(cn) >= 2 and (target in cn or cn in target):
                audited = (
                    cosine_similarity(summary_embedding, candidate.representative_vector)
                    if candidate.representative_vector else 1.0
                )
                return CategoryResolution(
                    name=candidate.name, category_id=candidate.category_id,
                    created=candidate.category_id is None,
                    similarity=audited, matched_by="name-variant",
                    ranking=[(round(audited, 3), candidate.name)],
                )

    # 2b) 이름↔이름 임베딩 — 표기가 다른 초근접 동의어만 잡는 극단 백스톱.
    #    임계값 이상이면 그 후보로 흡수, 미만이면 신규로 살린다.
    #    단어 임베딩은 동의어가 아니라 '관련성'을 재서, 형제 개념(야구~축구 0.853,
    #    부동산~주식 0.851)이 동의어만큼 가깝게 나온다(92종 4186쌍 실측). 그래서
    #    문턱을 0.90 까지 올려 진짜 초근접(카페~커피 0.903)만 남긴다. 의미 중복의
    #    1차 방어는 프롬프트 재사용 규칙이다(실사진 55장에서 파편화 0 실측).
    #    이름 벡터가 없는 후보는 이 판정에 못 들어간다(흡수 못 함 = 신규 쪽으로
    #    기운다). 신규를 죽이는 오류가 제품 약속("반드시 새 카테고리가 나오게")을
    #    깨므로, 불확실하면 살리는 방향이 맞다.
    missing = [c.name for c in candidates
               if normalize_name(c.name) not in name_vectors]
    if missing:
        logger.warning("이름 벡터 없는 카테고리 후보 %s건 — 중복 판정에서 제외: %s",
                       len(missing), missing[:10])

    scored = []
    if proposed_name_vector:
        for c in candidates:
            nv = name_vectors.get(normalize_name(c.name))
            if nv:
                scored.append((cosine_similarity(proposed_name_vector, nv), c))
    scored.sort(key=lambda item: item[0], reverse=True)
    ranking = [(round(score, 3), c.name) for score, c in scored[:3]]

    if scored and scored[0][0] >= name_dup_threshold:
        best_similarity, best = scored[0]
        # 보고하는 similarity 는 감사 코사인(요약↔centroid)으로 통일한다 —
        # categoryConfidence 의 의미(판정 결과에 대한 벡터 감사)를 경로마다
        # 다르게 만들지 않기 위해서다. centroid 가 없으면 이름 코사인을 쓴다.
        audited = (
            cosine_similarity(summary_embedding, best.representative_vector)
            if best.representative_vector else best_similarity
        )
        return CategoryResolution(
            name=best.name, category_id=best.category_id,
            created=best.category_id is None,
            similarity=audited, matched_by="name-dup", ranking=ranking,
        )

    # 3) 중복 아님 → 신규 카테고리. similarity 는 가장 가까운 기존 이름과의
    #    코사인(낮을수록 확실한 신규)이라 관제에서 "아슬아슬한 신규"를 찾는 데 쓴다.
    return CategoryResolution(
        name=proposed, category_id=None, created=True,
        similarity=scored[0][0] if scored else 0.0,
        matched_by="new", ranking=ranking,
    )
