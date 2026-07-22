"""OpenSearch 키워드 검색 (FastAPI 전담).

- 색인: AGENT 단계가 끝나면 요약·태그·카테고리·OCR 원문을 색인한다.
- 검색: BM25(multi_match) 키워드 검색. 한글은 nori analyzer 로 형태소 분석한다.

의미(벡터) 검색은 MVP 범위에서 제외한다. 확장 시 이 인덱스에 knn_vector 필드를
추가하고 AGENT 콜백의 documentVector 를 함께 색인하면 하이브리드로 넓힐 수 있다.

SDK(opensearch-py) 의존성을 이 파일에만 가둔다.
"""

import logging
from functools import lru_cache
from typing import Any

from .config import get_settings

logger = logging.getLogger(__name__)

# 한글 형태소 분석기. nori 플러그인이 설치돼 있어야 한다(도커 이미지에 포함).
_KOREAN_ANALYZER = "korean"

_index_ready = False


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
                "tags": {"type": "text", "analyzer": _KOREAN_ANALYZER},
                "category_name": {"type": "keyword"},  # 정확 일치 필터
                "raw_text": {"type": "text", "analyzer": _KOREAN_ANALYZER},
                "created_at": {"type": "date"},
            }
        },
    }


def ensure_index() -> None:
    """인덱스가 없으면 생성한다(멱등). 최초 색인·검색 직전에 한 번만 수행."""
    global _index_ready
    if _index_ready:
        return
    client = _client()
    index = get_settings().opensearch_index
    if not client.indices.exists(index=index):
        client.indices.create(index=index, body=_index_body())
        logger.info("OpenSearch 인덱스 생성: %s", index)
    _index_ready = True


def index_document(
    image_id: int,
    user_id: int,
    title: str,
    summary: str,
    tags: list[str],
    category_name: str | None,
    raw_text: str,
    created_at: str,
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
    }
    # refresh 는 기본값(비동기)로 둔다. 대량 색인 시 건당 강제 refresh 는 느리다.
    _client().index(index=settings.opensearch_index, id=str(image_id), body=doc)


def keyword_search(
    user_id: int, query: str, category: str | None, size: int
) -> tuple[list[dict[str, Any]], int]:
    """BM25 키워드 검색. 항상 user_id 로 격리하고, category 가 있으면 추가 필터."""
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

    resp = _client().search(
        index=settings.opensearch_index,
        body={"size": size, "query": {"bool": bool_query}},
    )

    hits = [
        {
            "image_id": h["_source"].get("image_id"),
            "title": h["_source"].get("title", ""),
            "summary": h["_source"].get("summary", ""),
            "tags": h["_source"].get("tags", []),
            "category": h["_source"].get("category_name"),
            "score": h.get("_score", 0.0),
        }
        for h in resp["hits"]["hits"]
    ]
    total = resp["hits"]["total"]["value"]
    return hits, total
