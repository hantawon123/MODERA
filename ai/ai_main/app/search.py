"""OpenSearch 키워드 검색 (FastAPI 전담).

- 색인: AGENT 단계가 끝나면 요약·태그·카테고리·OCR 원문을 색인한다.
- 검색: BM25(multi_match) 키워드 검색. 한글은 nori analyzer 로 형태소 분석한다.

의미(벡터) 검색은 MVP 범위에서 제외한다. 확장 시 이 인덱스에 knn_vector 필드를
추가하고 AGENT 콜백의 documentVector 를 함께 색인하면 하이브리드로 넓힐 수 있다.

SDK(opensearch-py) 의존성을 이 파일에만 가둔다.
"""

import logging
import time
import zlib
from functools import lru_cache
from typing import Any

from .config import get_settings

logger = logging.getLogger(__name__)

# 한글 형태소 분석기. nori 플러그인이 설치돼 있어야 한다(도커 이미지에 포함).
# 이 이름의 analyzer 를 title·summary·tags·raw_text 가 쓴다. nori 가 없는
# OpenSearch 로 바꾸면 인덱스 생성 자체가 실패한다.
_KOREAN_ANALYZER = "korean"

# 인덱스 존재 확인 캐시. 0 이면 아직 확인 전.
_index_checked_at = 0.0
_INDEX_CHECK_TTL = 60.0


class SearchError(RuntimeError):
    pass


@lru_cache(maxsize=1)
def _client():
    try:
        from opensearchpy import OpenSearch
    except ImportError as e:  # pragma: no cover
        raise SearchError("opensearch-py 가 설치되지 않았습니다.") from e

    settings = get_settings()
    http_auth = (
        (settings.opensearch_user, settings.opensearch_password)
        if settings.opensearch_user
        else None
    )
    return OpenSearch(
        hosts=[{"host": settings.opensearch_host, "port": settings.opensearch_port}],
        http_auth=http_auth,
        use_ssl=settings.opensearch_use_ssl,
        verify_certs=False,      # 내부망/자체서명 인증서 허용
        ssl_show_warn=False,
    )


def _index_body() -> dict[str, Any]:
    return {
        "settings": {
            "index": {"number_of_shards": 1, "number_of_replicas": 0},
            "analysis": {
                "analyzer": {
                    # nori_tokenizer 로 복합명사까지 분해해 한글 키워드 검색 품질을 높인다.
                    _KOREAN_ANALYZER: {
                        "type": "custom",
                        "tokenizer": "nori_tokenizer",
                        "filter": ["lowercase"],
                    }
                }
            },
        },
        "mappings": {
            "properties": {
                "image_id": {"type": "long"},
                "user_id": {"type": "long"},          # 사용자별 검색 격리용
                "title": {"type": "text", "analyzer": _KOREAN_ANALYZER},
                "summary": {"type": "text", "analyzer": _KOREAN_ANALYZER},
                # 검색은 형태소 분석된 text 로, 태그 목록 집계는 keyword 서브필드로 한다.
                "tags": {
                    "type": "text",
                    "analyzer": _KOREAN_ANALYZER,
                    "fields": {"keyword": {"type": "keyword"}},
                },
                "category_name": {"type": "keyword"},  # 정확 일치 필터
                "raw_text": {"type": "text", "analyzer": _KOREAN_ANALYZER},
                "created_at": {"type": "date"},
                # 아래 둘은 화면에 돌려주기만 하고 검색 대상은 아니라 색인하지 않는다.
                "s3_key": {"type": "keyword", "index": False},
                "key_information": {"type": "keyword", "index": False},
            }
        },
    }


def ensure_index(force: bool = False) -> None:
    """인덱스가 없으면 **올바른 매핑으로** 만든다(멱등).

    왜 매번 확인하지 않고, 왜 캐시를 영원히 두지도 않는가:

    OpenSearch 는 `action.auto_create_index` 가 기본 true 라, 없는 인덱스에 문서를
    쓰면 알아서 만들어 준다. 문제는 그때 만들어지는 매핑에는 **nori analyzer 가 없다**는
    것이다. 한글 검색이 조용히 망가지고, 에러는 아무 데도 안 뜬다.

    이 상황은 OpenSearch 컨테이너를 볼륨까지 지우고 다시 올렸는데 FastAPI 는 계속
    떠 있을 때 실제로 생긴다. 캐시를 영구히 두면 ensure_index 가 그냥 통과해 버려서
    다음 색인이 잘못된 매핑을 만든다.

    그래서 캐시에 유효기간을 둔다. exists 는 로컬 HEAD 요청이라 싸고,
    최악의 경우에도 TTL 안에 스스로 복구된다.
    """
    global _index_checked_at
    now = time.monotonic()
    if not force and now - _index_checked_at < _INDEX_CHECK_TTL:
        return
    client = _client()
    index = get_settings().opensearch_index
    if not client.indices.exists(index=index):
        client.indices.create(index=index, body=_index_body())
        logger.warning("OpenSearch 인덱스 생성: %s (nori 매핑 적용)", index)
    _index_checked_at = now


def index_document(
    image_id: int,
    user_id: int,
    title: str,
    summary: str,
    tags: list[str],
    category_name: str | None,
    raw_text: str,
    created_at: str,
    s3_key: str | None = None,
    key_information: list[str] | None = None,
) -> None:
    """AGENT 결과를 검색 인덱스에 저장한다. image_id 를 문서 id 로 써서 재색인은 덮어쓴다."""
    ensure_index()
    settings = get_settings()
    doc = {
        "image_id": image_id,
        "user_id": user_id,
        "title": title,
        "summary": summary,
        "tags": tags,
        "category_name": category_name,
        "raw_text": raw_text,
        "created_at": created_at,
        "s3_key": s3_key,
        "key_information": key_information or [],
    }
    # refresh 는 기본값(비동기)로 둔다. 대량 색인 시 건당 강제 refresh 는 느리다.
    _client().index(index=settings.opensearch_index, id=str(image_id), body=doc)


def keyword_search(
    user_id: int,
    query: str,
    category: str | None,
    size: int,
    page: int = 0,
    tag: str | None = None,
) -> tuple[list[dict[str, Any]], int]:
    """BM25 키워드 검색. 항상 user_id 로 격리하고, category·tag 가 있으면 추가 필터."""
    ensure_index()
    settings = get_settings()

    bool_query: dict[str, Any] = {
        "must": [
            {
                "multi_match": {
                    "query": query,
                    # 제목 가중치를 높이고, 요약·태그·원문까지 함께 매칭한다.
                    "fields": ["title^2", "summary", "tags", "raw_text"],
                }
            }
        ],
        "filter": [{"term": {"user_id": user_id}}],
    }
    if category:
        bool_query["filter"].append({"term": {"category_name": category}})
    if tag:
        bool_query["filter"].append({"term": {"tags.keyword": tag}})

    resp = _client().search(
        index=settings.opensearch_index,
        body={
            "from": max(0, page) * size,
            "size": size,
            "query": {"bool": bool_query},
        },
    )

    hits = [
        {
            "image_id": h["_source"].get("image_id"),
            "title": h["_source"].get("title", ""),
            "summary": h["_source"].get("summary", ""),
            "tags": h["_source"].get("tags", []),
            "category": h["_source"].get("category_name"),
            "s3_key": h["_source"].get("s3_key"),
            "created_at": h["_source"].get("created_at"),
            "score": h.get("_score", 0.0),
        }
        for h in resp["hits"]["hits"]
    ]
    total = resp["hits"]["total"]["value"]
    return hits, total


def _to_image(source: dict[str, Any]) -> dict[str, Any]:
    return {
        "image_id": source.get("image_id"),
        "title": source.get("title", ""),
        "summary": source.get("summary", ""),
        "tags": source.get("tags", []),
        "category": source.get("category_name"),
        "s3_key": source.get("s3_key"),
        "created_at": source.get("created_at"),
    }


def list_images(
    user_id: int,
    page: int,
    size: int,
    category: str | None = None,
    tag: str | None = None,
) -> tuple[list[dict[str, Any]], int]:
    """홈 화면용 목록. 최근 분석순으로 정렬하며 카테고리·태그로 좁힐 수 있다."""
    ensure_index()
    settings = get_settings()

    filters: list[dict[str, Any]] = [{"term": {"user_id": user_id}}]
    if category:
        filters.append({"term": {"category_name": category}})
    if tag:
        # 집계·필터는 분석되지 않은 keyword 서브필드를 쓴다.
        filters.append({"term": {"tags.keyword": tag}})

    resp = _client().search(
        index=settings.opensearch_index,
        body={
            "from": max(0, page) * size,
            "size": size,
            "query": {"bool": {"filter": filters}},
            "sort": [{"created_at": {"order": "desc"}}],
        },
    )
    images = [_to_image(h["_source"]) for h in resp["hits"]["hits"]]
    return images, resp["hits"]["total"]["value"]


def get_image(image_id: int) -> dict[str, Any] | None:
    """이미지 상세. 문서 id 가 image_id 라 바로 조회한다."""
    ensure_index()
    settings = get_settings()
    try:
        resp = _client().get(index=settings.opensearch_index, id=str(image_id))
    except Exception:
        return None
    if not resp.get("found"):
        return None
    source = resp["_source"]
    detail = _to_image(source)
    detail["user_id"] = source.get("user_id")
    detail["key_information"] = source.get("key_information", [])
    detail["raw_text"] = source.get("raw_text", "")
    return detail


def _aggregate(user_id: int, field: str, limit: int) -> list[dict[str, Any]]:
    """사용자 문서를 대상으로 term 집계를 돌려 (이름, 개수) 목록을 만든다."""
    ensure_index()
    settings = get_settings()
    resp = _client().search(
        index=settings.opensearch_index,
        body={
            "size": 0,
            "query": {"bool": {"filter": [{"term": {"user_id": user_id}}]}},
            "aggs": {"grouped": {"terms": {"field": field, "size": limit}}},
        },
    )
    buckets = resp.get("aggregations", {}).get("grouped", {}).get("buckets", [])
    return [{"name": b["key"], "count": b["doc_count"]} for b in buckets]


def aggregate_categories(user_id: int, limit: int = 50) -> list[dict[str, Any]]:
    """카테고리 목록. 카드 UI 에 필요한 썸네일·대표 태그까지 한 번에 모아 준다.

    카테고리별로 최신 이미지 1장(top_hits)과 상위 태그(terms)를 하위 집계로 붙인다.
    정렬(이름순·최신순·개수순)은 앱이 이 데이터로 직접 처리할 수 있게 필드를 다 담는다.
    """
    ensure_index()
    settings = get_settings()
    resp = _client().search(
        index=settings.opensearch_index,
        body={
            "size": 0,
            "query": {"bool": {"filter": [{"term": {"user_id": user_id}}]}},
            "aggs": {
                "grouped": {
                    "terms": {"field": "category_name", "size": limit},
                    "aggs": {
                        "recent": {
                            "top_hits": {
                                "size": 1,
                                "sort": [{"created_at": {"order": "desc"}}],
                                "_source": {
                                    "includes": ["image_id", "s3_key", "created_at"]
                                },
                            }
                        },
                        # 카테고리명과 겹치는 태그를 걸러내고 4개를 채우기 위해 넉넉히 받는다.
                        "top_tags": {"terms": {"field": "tags.keyword", "size": 8}},
                    },
                }
            },
        },
    )

    categories: list[dict[str, Any]] = []
    for bucket in resp.get("aggregations", {}).get("grouped", {}).get("buckets", []):
        name = bucket["key"]
        hits = bucket.get("recent", {}).get("hits", {}).get("hits", [])
        recent = hits[0]["_source"] if hits else {}

        tags = [
            {"name": t["key"], "count": t["doc_count"]}
            for t in bucket.get("top_tags", {}).get("buckets", [])
            # 카테고리명과 같은 태그는 카드에서 중복이라 뺀다.
            if t["key"] != name
        ][:4]

        categories.append({
            "name": name,
            "count": bucket["doc_count"],
            "tags": tags,
            "thumbnail_image_id": recent.get("image_id"),
            "thumbnail_s3_key": recent.get("s3_key"),
            "last_updated_at": recent.get("created_at"),
        })
    return categories


def aggregate_tags(user_id: int, limit: int = 30) -> list[dict[str, Any]]:
    return _aggregate(user_id, "tags.keyword", limit)


# ── 카테고리·태그 ID ──────────────────────────────────────────────────────
# 이 서비스는 이름만 색인하고 별도 ID 테이블을 두지 않는다. 앱 명세가 숫자 ID 를
# 요구하므로 이름에서 결정적으로 파생시킨다. 해시라서 서버를 재시작해도 값이
# 유지되고, 별도 저장소가 필요 없다. Spring 복귀 시에는 DB 의 실제 ID 로 바뀐다.


def max_image_id() -> int:
    """색인된 문서 중 가장 큰 image_id. 문서가 없으면 0.

    image_id 는 곧 문서 id 라, 서버가 재시작한 뒤에도 이 값 다음부터 채번해야
    기존 문서를 덮어쓰지 않는다.
    """
    ensure_index()
    settings = get_settings()
    resp = _client().search(
        index=settings.opensearch_index,
        body={"size": 0, "aggs": {"max_id": {"max": {"field": "image_id"}}}},
    )
    value = resp.get("aggregations", {}).get("max_id", {}).get("value")
    return int(value) if value else 0


def stable_id(name: str) -> int:
    """이름 → 31비트 양의 정수 ID. 같은 이름이면 항상 같은 값이 나온다."""
    return zlib.crc32((name or "").encode("utf-8")) & 0x7FFFFFFF


def to_tag_refs(names: list[str]) -> list[dict[str, Any]]:
    return [{"tag_id": stable_id(n), "name": n} for n in names or []]


def resolve_name_by_id(user_id: int, field: str, target_id: int) -> str | None:
    """ID 로 필터가 들어왔을 때, 집계 결과를 훑어 원래 이름을 되찾는다.

    해시는 되돌릴 수 없으므로 후보 이름들을 모아 같은 해시를 찾는 방식이다.
    카테고리·태그 개수가 많지 않아 비용은 크지 않다.
    """
    for item in _aggregate(user_id, field, 500):
        if stable_id(item["name"]) == target_id:
            return item["name"]
    return None
