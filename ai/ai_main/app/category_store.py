"""카테고리 대표 벡터 저장소 (OpenSearch).

카테고리 판정에 쓰는 벡터를 서버 재기동 뒤에도 유지한다. AGENT 가 새로 만든
카테고리도 여기 남으므로, 다음 분석 때 후보로 다시 제시된다.

문서는 두 종류다.

  사용자 문서  id="{user_id}:{정규화이름}"  count>=1
    그 카테고리에 붙은 **이미지 요약 임베딩들의 평균**(centroid). 이미지가
    쌓일수록 정확해진다. 사용자별로 격리된다 — 다른 사용자의 카테고리는
    후보에 들어가지 않는다(centroid 는 그 사람 이미지 내용의 평균이다).

  전역 시드   id="0:{정규화이름}"           count=0
    기본 카테고리 **이름 문자열**의 임베딩. 이미지가 0장인 카테고리는 평균낼
    것이 없어 centroid 를 만들 수 없으므로, 콜드 스타트에서만 대신 쓴다.
    사용자와 무관하게 같은 값이라 사용자별로 복제하지 않는다(문서 17개로 끝).
    사용자 문서와 id 가 겹치지 않으므로 centroid 에 누적되지 않는다 — 단어
    임베딩이 문장 centroid 를 오염시키면 이미지가 쌓여도 씻겨나가지 않는다.

본문 인덱스에 섞지 않고 인덱스를 따로 두는 이유는 벡터 공간이 다르기 때문이다.
본문 embedding 은 로컬 bge-m3(1024), 이쪽은 Gemini(768).

OpenSearch 연결·인덱스 생성 원시 함수(`_client`·`_create_index_with_template`)와
`stable_id` 는 검색 본체(`search.py`)와 공유한다. 여기서 import 만 한다 —
search 는 이 모듈을 되부르지 않으므로 순환이 없다.
"""

import logging
import time
from typing import Any

from .config import get_settings
from .category import normalize_name
from .search import _client, _create_index_with_template, _INDEX_CHECK_TTL, stable_id
from .timeutil import now_iso

logger = logging.getLogger(__name__)

_category_index_checked_at = 0.0

# 전역 시드 문서의 user_id. 실제 사용자 id 는 1 부터라 겹치지 않는다.
SEED_USER_ID = 0

# 한 번에 읽어오는 카테고리 수 상한. 시드 17종에 사용자 카테고리가 더해지는
# 정도라 넉넉하다. 넘으면 경고를 남긴다(조용히 잘리면 판정이 이유 없이 나빠진다).
_CATEGORY_LOAD_LIMIT = 500

# 다른 사용자가 만든 카테고리 이름을 한 번에 몇 개까지 빌려올지(서로 다른 이름 기준).
# 0 이면 이름 공유를 끈다.
#
# 이 값은 "얼마나 공유할까" 의 예산이 아니라 폭주 감지선이다. 카테고리가 우후죽순
# 늘어나는 것을 막으려고 만든 것이 프롬프트 재사용 규칙과 흡수 관문(표기 변형 +
# 이름 임베딩 0.90)이고, 그게 제대로 돌면 서로 다른 이름은 기본 12종에 주제 몇십
# 개가 더해지는 선에서 멈춘다(실사진 55장·엣지 40장 실측 모두 파편화 0건).
# 이름을 전역으로 공유하는 것 자체가 그 수렴 장치를 사용자 사이로 확장하는 것이라
# 이름 수는 오히려 줄어드는 방향이다.
#
# 그래서 여기 걸린다는 것은 "상한을 올려야 한다" 가 아니라 "관문이 새고 있다" 는
# 신호다. 경고를 남기는 이유가 그것이다 — 조용히 잘리면 파편화가 진행 중인데도
# 후보만 잘려 나가 원인을 못 찾는다. `_CATEGORY_LOAD_LIMIT` 과 같은 성격·같은 크기.
_SHARED_NAME_LIMIT = _CATEGORY_LOAD_LIMIT


def _category_index() -> str:
    return f"{get_settings().opensearch_index}_categories"


def _status_code(e: Exception) -> int | None:
    return getattr(e, "status_code", None)


def ensure_category_index(force: bool = False) -> None:
    """카테고리 벡터 인덱스를 만든다(멱등). TTL 전략은 ensure_index 와 같다."""
    global _category_index_checked_at
    now = time.monotonic()
    if not force and now - _category_index_checked_at < _INDEX_CHECK_TTL:
        return
    client = _client()
    index = _category_index()
    if not client.indices.exists(index=index):
        _create_index_with_template(index, {
            "settings": {"index": {"number_of_shards": 1, "number_of_replicas": 0}},
            "mappings": {"properties": {
                "user_id": {"type": "long"},
                "name": {"type": "keyword"},
                # name 과 같은 것을 가리키는 숫자 키(= stable_id(name)). Spring 의
                # 카테고리 테이블은 전역이고 user_id 도 auto-increment 도 없어서
                # 이 값이 곧 그쪽 PK 가 된다. 이름에서 파생되므로 굳이 저장하지
                # 않아도 계산되지만, 후보에 실어 보내려면 조회 결과에 함께 와야 한다.
                "category_id": {"type": "long"},
                # kNN 을 쓰지 않는다. 사용자당 수십 개라 전량 읽어 파이썬에서
                # 코사인을 도는 편이 싸고, 차원을 바꿔도 매핑을 안 건드린다.
                "vector": {"type": "float", "index": False},
                "count": {"type": "integer"},      # centroid 에 반영된 이미지 수
                # 모델·차원이 지금 설정과 다른 행은 다른 벡터 공간이라 무효다.
                # 프로세스 메모리 캐시는 재기동이 무효화를 대신해 줬지만, 영속
                # 저장은 그 공짜가 없으므로 무효화 키를 문서에 함께 적어 둔다.
                "model": {"type": "keyword"},
                "dim": {"type": "integer"},
                "updated_at": {"type": "date"},
            }},
        })
        logger.warning("OpenSearch 카테고리 벡터 인덱스 생성: %s", index)
    _category_index_checked_at = now


def load_category_vectors(user_id: int) -> dict[str, dict[str, Any]]:
    """판정에 쓸 카테고리 벡터. 정규화된 이름 → {name, vector, count}.

    사용자 문서(centroid)와 전역 시드(이름 임베딩)를 함께 읽고, 이름이 겹치면
    사용자 centroid 를 쓴다 — 시드는 그 카테고리에 이미지가 아직 없을 때만 쓰인다.

    임베딩 모델·차원이 지금 설정과 다른 행은 버린다(코사인이 의미 없다).
    조회가 실패하면 빈 dict 를 돌려준다 — 판정은 이름 완전일치로 degrade 한다.
    """
    settings = get_settings()
    try:
        ensure_category_index()
        resp = _client().search(
            index=_category_index(),
            body={
                "size": _CATEGORY_LOAD_LIMIT,
                "track_total_hits": True,
                "query": {"bool": {"filter": [
                    {"terms": {"user_id": sorted({SEED_USER_ID, user_id})}},
                    {"term": {"model": settings.embedding_model_name}},
                    {"term": {"dim": settings.embedding_dim}},
                ]}},
                # 시드(user_id=0)를 먼저 깔고 사용자 문서로 덮으려면 정렬이 필요하다.
                "sort": [{"user_id": {"order": "asc"}}],
                "_source": ["name", "category_id", "vector", "count"],
            },
        )
    except Exception as e:
        logger.warning("카테고리 벡터 조회 실패 userId=%s: %s — 이름 일치로만 진행",
                       user_id, e)
        return {}

    hits = resp.get("hits", {})
    total = (hits.get("total") or {}).get("value", 0)
    if total > _CATEGORY_LOAD_LIMIT:
        logger.warning("카테고리 벡터 %s건 중 %s건만 읽었다 — 상한을 올려야 한다",
                       total, _CATEGORY_LOAD_LIMIT)
    stored: dict[str, dict[str, Any]] = {}
    for hit in hits.get("hits", []):
        source = hit.get("_source", {})
        name, vector = source.get("name"), source.get("vector")
        if name and vector:
            stored[normalize_name(name)] = {
                "name": name,
                # 이 필드가 생기기 전에 쓰인 문서는 값이 없다. 이름에서 다시
                # 계산하면 같은 값이라 마이그레이션 없이 섞여 있어도 된다.
                "category_id": int(source.get("category_id") or stable_id(name)),
                "vector": [float(v) for v in vector],
                "count": int(source.get("count") or 0),
            }
    _add_shared_names(stored, user_id, settings)
    return stored


def _add_shared_names(
    stored: dict[str, dict[str, Any]], user_id: int, settings: Any
) -> None:
    """다른 사용자가 만든 카테고리 **이름**을 후보에 채운다. 벡터는 빌리지 않는다.

    아이콘과 Spring taxonomy 는 이미 이름 단위 전역이다 — 같은 이름이면 stable_id
    가 같아 같은 행·같은 아이콘 파일을 쓴다. 그런데 판정 후보가 사용자별로 갇혀
    있으면 같은 개념이 사람마다 다른 이름으로 갈라져("부동산" vs "집구하기") 그
    전제가 깨진다. 개념당 아이콘 한 장이라는 설계가 개념당 N 장이 되고, 생성 요금이
    그만큼 나간다(같은 이름은 `ensure_icon` 이 이미 존재 확인으로 막아 준다).

    centroid 는 공유하지 않는다 — 그 사람 이미지 요약의 평균이라 개인 데이터다.
    이름만 count=0 으로 넣으면 `stages.build_candidates` 가 전역 시드와 똑같이
    다룬다: 이름은 프롬프트 후보에 들어가고 벡터 판정에서는 빠진다(이름 임베딩의
    분리력이 노이즈라는 실측 결과를 그대로 따른다).

    사용자가 하나뿐이면 빌려올 문서가 없어 아무 것도 하지 않는다 — 지금까지의
    단일 사용자 실측(골드셋 93.6%)과 후보 목록이 완전히 같다.
    """
    if _SHARED_NAME_LIMIT <= 0:
        return
    try:
        resp = _client().search(
            index=_category_index(),
            body={
                # 문서가 아니라 이름 집계만 필요하다. 같은 이름을 쓰는 사용자가
                # 많을수록 문서도 그만큼 많으므로(1:쇼핑, 2:쇼핑 …) 문서를 그대로
                # 읽으면 흔한 이름의 사본이 상한을 다 잡아먹는다. terms 집계는
                # 이름당 한 칸만 쓰므로 상한이 곧 "서로 다른 이름 수" 가 된다.
                "size": 0,
                "query": {"bool": {
                    "filter": [
                        {"term": {"model": settings.embedding_model_name}},
                        {"term": {"dim": settings.embedding_dim}},
                    ],
                    # 시드와 본인 문서는 위에서 이미 벡터까지 읽었다.
                    "must_not": [
                        {"terms": {"user_id": sorted({SEED_USER_ID, user_id})}}],
                }},
                # 기본 정렬이 doc_count 내림차순 — 쓰는 사람이 많은 이름부터다.
                # 상한에 걸려 잘려도 흔한 어휘가 남아 수렴에 가장 도움이 된다.
                "aggs": {"names": {"terms": {"field": "name",
                                             "size": _SHARED_NAME_LIMIT}}},
            },
        )
    except Exception as e:
        # 조회 실패는 판정을 막지 않는다 — 이 사용자 후보만으로 진행한다.
        logger.warning("공유 카테고리 이름 조회 실패 userId=%s: %s", user_id, e)
        return

    aggregations = resp.get("aggregations") or {}
    names_agg = aggregations.get("names") or {}
    if names_agg.get("sum_other_doc_count"):
        # 상한을 넘었다 = 서로 다른 카테고리 이름이 수백 개라는 뜻이다. 프롬프트
        # 재사용 규칙과 흡수 관문이 새고 있는 것이므로 상한을 올릴 게 아니라
        # 파편화 원인을 봐야 한다(어떤 이름들이 갈렸는지 저장소를 직접 조회).
        logger.warning(
            "공유 카테고리 이름이 상한 %s개를 넘었다 — 카테고리 파편화를 의심할 것"
            "(흡수 관문·프롬프트 재사용 규칙 점검). userId=%s",
            _SHARED_NAME_LIMIT, user_id)
    for bucket in names_agg.get("buckets") or []:
        name = bucket.get("key")
        if not name:
            continue
        key = normalize_name(name)
        # 본인 centroid·시드가 이미 있으면 그쪽이 이긴다(벡터를 갖고 있다).
        if key not in stored:
            stored[key] = {"name": name, "category_id": stable_id(name),
                           "vector": None, "count": 0}


def put_seed_category_vectors(name_to_vector: dict[str, list[float]]) -> int:
    """기본 카테고리 이름 벡터를 전역 시드로 심는다(멱등). 심은 개수를 돌려준다.

    count=0 으로 넣어 "이미지 0장" 을 명시한다. 사용자 centroid 문서와 id 가
    다르므로(0:이름 vs {userId}:이름) 이 값이 centroid 에 누적되는 경로는 없다.

    같은 이름이면 항상 같은 벡터라 동시 쓰기를 막을 필요가 없다(덮어써도 같은 값).
    실패해도 예외를 올리지 않는다 — 시드가 없으면 콜드 스타트 판정이 이름 완전일치
    로만 동작하고, 다음 기동에서 다시 시도한다.
    """
    if not name_to_vector:
        return 0
    settings = get_settings()
    now = now_iso()
    written = 0
    try:
        ensure_category_index()
        for name, vector in name_to_vector.items():
            _client().index(
                index=_category_index(),
                id=f"{SEED_USER_ID}:{normalize_name(name)}",
                body={
                    "user_id": SEED_USER_ID,
                    "name": name,
                    "category_id": stable_id(name),
                    "vector": list(vector),
                    "count": 0,
                    "model": settings.embedding_model_name,
                    "dim": len(vector),
                    "updated_at": now,
                },
            )
            written += 1
        _client().indices.refresh(index=_category_index())
    except Exception as e:
        logger.warning("카테고리 시드 저장 실패(%s/%s건): %s",
                       written, len(name_to_vector), e)
    return written


def _get_category_doc(index: str, doc_id: str) -> dict[str, Any] | None:
    """문서와 낙관적 동시성 토큰을 함께 읽는다. 없으면 None."""
    try:
        resp = _client().get(index=index, id=doc_id)
    except Exception as e:
        if _status_code(e) == 404:
            return None
        raise
    return {
        "source": resp.get("_source", {}),
        "seq_no": resp.get("_seq_no"),
        "primary_term": resp.get("_primary_term"),
    }


def upsert_category_vector(
    user_id: int, name: str, vector: list[float], attempts: int = 3
) -> None:
    """판정된 카테고리의 대표 벡터를 새 요약 임베딩으로 갱신한다(없으면 생성).

    누적 평균이라 과거 벡터를 다시 임베딩하지 않는다 — Gemini 추가 호출 0회.

    같은 사용자·카테고리를 동시에 갱신하면 409 가 난다. 그때는 읽기부터 다시 한다.
    덮어쓰기로 넘어가면 갱신 하나를 잃고 centroid 가 조용히 틀어진다.

    분석을 실패시키지 않는다(best-effort). 저장이 안 되면 다음 분석은 이름
    임베딩으로 판정하며, 정확도만 떨어지고 동작은 유지된다.
    """
    if not vector:
        return
    settings = get_settings()
    index = _category_index()
    doc_id = f"{user_id}:{normalize_name(name)}"
    now = now_iso()
    for attempt in range(1, attempts + 1):
        try:
            ensure_category_index()
            current = _get_category_doc(index, doc_id)
            merged, count = list(vector), 1
            previous = (current or {}).get("source", {})
            # 모델·차원이 다른 기존 값은 이어붙이지 않고 새 벡터로 다시 시작한다.
            if (previous.get("model") == settings.embedding_model_name
                    and len(previous.get("vector") or []) == len(vector)):
                n = int(previous.get("count") or 1)
                merged = [(float(p) * n + v) / (n + 1)
                          for p, v in zip(previous["vector"], vector)]
                count = n + 1
            body = {
                "user_id": user_id,
                "name": name,
                "category_id": stable_id(name),
                "vector": merged,
                "count": count,
                "model": settings.embedding_model_name,
                "dim": len(merged),
                "updated_at": now,
            }
            # 새 문서는 create 로 넣어 동시 생성 하나가 다른 하나를 덮지 않게 한다.
            guard = (
                {"if_seq_no": current["seq_no"], "if_primary_term": current["primary_term"]}
                if current else {"op_type": "create"}
            )
            # refresh 를 기다린다. 다음 분석은 load_category_vectors(=search 쿼리)로
            # 후보를 읽는데, OpenSearch 는 기본 1초 주기로만 refresh 하므로 방금 만든
            # 카테고리가 바로 뒤 이미지의 후보 목록에서 빠진다(같은 카테고리를 또
            # 신규로 판정). 문서 수십 개짜리 인덱스라 refresh 비용은 무시할 수 있다.
            _client().index(index=index, id=doc_id, body=body, refresh=True, **guard)
            logger.info("카테고리 벡터 갱신 userId=%s name=%s count=%s",
                        user_id, name, count)
            return
        except Exception as e:
            if _status_code(e) == 409 and attempt < attempts:
                continue
            logger.warning("카테고리 벡터 저장 실패 userId=%s name=%s: %s "
                           "(분석은 정상 진행)", user_id, name, e)
            return
