"""카테고리 벡터 저장소 자체 점검. OpenSearch 없이 돈다.

가짜 OpenSearch 클라이언트를 끼워 저장·조회·병합 규칙을 검증한다. 검증 대상은
전부 조용히 틀어질 수 있는 것들이다 — centroid 누적 평균, 모델·차원이 바뀐 행의
무효화, 동시 갱신(409) 재시도, 저장된 카테고리가 후보로 되살아나는지.

    python scripts/category_store_check.py
"""

import os
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
os.environ.setdefault("INTERNAL_TOKEN", "check")
os.environ.setdefault("GMS_KEY", "check")
os.environ.setdefault("GEMINI_API_KEY", "check")
os.environ.setdefault("GCP_PROJECT", "check")

from app import category_store, search, stages       # noqa: E402
from app.category import normalize_name               # noqa: E402
from app.schemas import CategoryCandidate            # noqa: E402

MODEL = search.get_settings().embedding_model_name
DIM = search.get_settings().embedding_dim


class FakeError(Exception):
    def __init__(self, status_code):
        super().__init__(f"fake {status_code}")
        self.status_code = status_code


class FakeIndices:
    def __init__(self):
        self.created: list[str] = []
        self.bodies: dict[str, dict] = {}

    def exists(self, index):
        return index in self.created

    def create(self, index, body):
        self.created.append(index)
        self.bodies[index] = body

    def put_index_template(self, name, body):
        pass

    def refresh(self, index):
        pass


class FakeClient:
    """필요한 만큼만 흉내내는 OpenSearch. docs: id -> (source, seq_no, primary_term)."""

    def __init__(self):
        self.indices = FakeIndices()
        self.docs: dict[str, tuple[dict, int, int]] = {}
        self.queries: list[dict] = []
        self.hits: list[dict] = []
        self.total = 0
        self.force_conflicts = 0      # 다음 N 번의 index 호출을 409 로 만든다

    def search(self, index, body):
        self.queries.append(body)
        return {"hits": {"total": {"value": self.total or len(self.hits)},
                         "hits": [{"_source": h} for h in self.hits]}}

    def get(self, index, id):
        if id not in self.docs:
            raise FakeError(404)
        source, seq_no, primary_term = self.docs[id]
        return {"found": True, "_source": source,
                "_seq_no": seq_no, "_primary_term": primary_term}

    def index(self, index, id, body, **guard):
        if self.force_conflicts > 0:
            self.force_conflicts -= 1
            raise FakeError(409)
        if guard.get("op_type") == "create" and id in self.docs:
            raise FakeError(409)
        if "if_seq_no" in guard and self.docs[id][1] != guard["if_seq_no"]:
            raise FakeError(409)
        seq_no = self.docs[id][1] + 1 if id in self.docs else 1
        self.docs[id] = (body, seq_no, 1)

    def update(self, index, id, body):
        """부분 병합(doc_as_upsert). 이미지 인덱스의 _merge_doc 이 쓴다."""
        source, seq_no, _ = self.docs.get(id, ({}, 0, 1))
        self.docs[id] = ({**source, **body["doc"]}, seq_no + 1, 1)


def use(client: FakeClient) -> None:
    # category_store 는 `from .search import _client` 로 이름을 바인딩해 갔다.
    # search 쪽만 갈아끼우면 가짜가 안 먹는다 — 둘 다 갈아야 한다.
    search._client = lambda: client
    category_store._client = lambda: client
    category_store._category_index_checked_at = 0.0


def stored_doc(client: FakeClient, doc_id: str) -> dict:
    return client.docs[doc_id][0]


# 뒤쪽 케이스가 모듈 함수를 몽키패치한 채로 끝나므로 원본을 붙잡아 둔다.
REAL_LOAD_CATEGORY_VECTORS = category_store.load_category_vectors
REAL_PUT_SEED_CATEGORY_VECTORS = category_store.put_seed_category_vectors


# 1) 신규 카테고리는 요약 임베딩 그대로 저장되고 count=1 이다.
client = FakeClient()
use(client)
category_store.upsert_category_vector(1, "쇼핑", [1.0] * DIM)
doc = stored_doc(client, "1:쇼핑")
assert doc["count"] == 1, doc["count"]
assert doc["vector"] == [1.0] * DIM
assert doc["dim"] == DIM and doc["model"] == MODEL
assert category_store._category_index() in client.indices.created

# 2) 두 번째 이미지는 누적 평균으로 합쳐진다(마지막 값 덮어쓰기가 아니다).
category_store.upsert_category_vector(1, "쇼핑", [0.0] * DIM)
doc = stored_doc(client, "1:쇼핑")
assert doc["count"] == 2, doc["count"]
assert doc["vector"] == [0.5] * DIM, doc["vector"][:3]

# 세 번째도 개수 가중이 맞아야 한다 — (0.5*2 + 2.0)/3 = 1.0
category_store.upsert_category_vector(1, "쇼핑", [2.0] * DIM)
doc = stored_doc(client, "1:쇼핑")
assert doc["count"] == 3 and doc["vector"] == [1.0] * DIM, doc["vector"][:3]

# 3) 이름 정규화가 같으면 같은 문서다(대소문자·공백으로 갈라지지 않는다).
category_store.upsert_category_vector(1, "  쇼핑 ", [1.0] * DIM)
assert stored_doc(client, "1:쇼핑")["count"] == 4
assert len(client.docs) == 1, list(client.docs)

# 4) 사용자별로 격리된다.
category_store.upsert_category_vector(2, "쇼핑", [7.0] * DIM)
assert stored_doc(client, "2:쇼핑")["count"] == 1
assert stored_doc(client, "1:쇼핑")["count"] == 4

# 5) 임베딩 모델이 바뀐 기존 행은 이어붙이지 않고 새로 시작한다.
client.docs["1:여행"] = ({"user_id": 1, "name": "여행", "vector": [9.0] * DIM,
                          "count": 50, "model": "old-model", "dim": DIM}, 1, 1)
category_store.upsert_category_vector(1, "여행", [1.0] * DIM)
doc = stored_doc(client, "1:여행")
assert doc["count"] == 1 and doc["vector"] == [1.0] * DIM, doc["count"]
assert doc["model"] == MODEL

# 6) 차원이 다른 기존 행도 마찬가지다(zip 이 조용히 잘라먹으면 안 된다).
client.docs["1:금융"] = ({"user_id": 1, "name": "금융", "vector": [9.0] * (DIM - 1),
                          "count": 5, "model": MODEL, "dim": DIM - 1}, 1, 1)
category_store.upsert_category_vector(1, "금융", [1.0] * DIM)
doc = stored_doc(client, "1:금융")
assert doc["count"] == 1 and len(doc["vector"]) == DIM

# 7) 409(동시 갱신)는 읽기부터 재시도한다 — 갱신을 잃지 않는다.
client.force_conflicts = 2
category_store.upsert_category_vector(1, "쇼핑", [1.0] * DIM)
assert stored_doc(client, "1:쇼핑")["count"] == 5, stored_doc(client, "1:쇼핑")["count"]

# 8) 재시도 한도를 넘으면 예외를 삼킨다(분석을 실패시키지 않는다).
client.force_conflicts = 99
category_store.upsert_category_vector(1, "쇼핑", [1.0] * DIM)
assert stored_doc(client, "1:쇼핑")["count"] == 5, "실패한 갱신이 반영됐다"

# 9) 빈 벡터는 아무것도 쓰지 않는다.
before = len(client.docs)
category_store.upsert_category_vector(1, "없음", [])
assert len(client.docs) == before

# 10) 조회는 현재 모델·차원으로 필터하고, 시드(user_id=0)를 함께 읽는다.
client = FakeClient()
use(client)
client.hits = [{"name": "쇼핑", "vector": [0.5] * DIM, "count": 3}]
loaded = category_store.load_category_vectors(7)
filters = client.queries[0]["query"]["bool"]["filter"]
assert {"term": {"model": MODEL}} in filters, filters
assert {"term": {"dim": DIM}} in filters, filters
assert {"terms": {"user_id": [0, 7]}} in filters, filters
# 시드가 먼저 오도록 정렬해야 사용자 centroid 가 시드를 덮는다.
assert client.queries[0]["sort"] == [{"user_id": {"order": "asc"}}]
assert loaded == {"쇼핑": {"name": "쇼핑", "category_id": search.stable_id("쇼핑"),
                           "vector": [0.5] * DIM, "count": 3}}

# 10-b) 같은 이름이면 사용자 centroid 가 시드를 덮는다(정렬 순서대로 나중 것이 이김).
client = FakeClient()
use(client)
client.hits = [
    {"name": "쇼핑", "vector": [0.1] * DIM, "count": 0},   # 시드 (user_id=0)
    {"name": "쇼핑", "vector": [0.9] * DIM, "count": 4},   # 사용자 centroid
]
loaded = category_store.load_category_vectors(7)
assert loaded["쇼핑"]["vector"] == [0.9] * DIM, "시드가 centroid 를 덮었다"
assert loaded["쇼핑"]["count"] == 4

# 11) 조회가 터지면 빈 dict — 판정은 이름 임베딩으로 degrade 한다.
class BoomClient(FakeClient):
    def search(self, index, body):
        raise FakeError(500)


use(BoomClient())
assert category_store.load_category_vectors(7) == {}

# 12) 저장된 카테고리는 후보로 되살아나고, 대표 벡터가 채워진다.
#     (= 재기동 뒤에도 AGENT 가 만든 카테고리가 프롬프트 후보에 남는다)
stages.category_store.load_category_vectors = lambda user_id: {
    "부동산": {"name": "부동산", "category_id": search.stable_id("부동산"),
               "vector": [0.3] * DIM, "count": 2},
}
candidates = stages.build_candidates(1, [])
names = [c.name for c in candidates]
assert names == ["부동산"], names
assert candidates[0].representative_vector == [0.3] * DIM
# 사용자에게 쌓인(count>0) 저장 카테고리는 id 를 가져야 기존 카테고리로 판정된다.
assert candidates[0].category_id == search.stable_id("부동산")

# 13) Spring 후보와 합쳐지고, representativeVector 가 있으면 그쪽이 우선이다.
stages.category_store.load_category_vectors = lambda user_id: {
    "쇼핑": {"name": "쇼핑", "category_id": search.stable_id("쇼핑"),
             "vector": [0.1] * DIM, "count": 9},
    "부동산": {"name": "부동산", "category_id": search.stable_id("부동산"),
               "vector": [0.3] * DIM, "count": 2},
}
spring = [
    CategoryCandidate(category_id=11, name="쇼핑"),
    CategoryCandidate(category_id=12, name="음식",
                      representative_vector=[0.9] * DIM),
]
candidates = stages.build_candidates(1, spring)
by_name = {c.name: c for c in candidates}
assert set(by_name) == {"쇼핑", "음식", "부동산"}, set(by_name)
assert by_name["쇼핑"].category_id == 11, "Spring 의 categoryId 가 사라졌다"
assert by_name["쇼핑"].representative_vector == [0.1] * DIM, "저장된 centroid 미적용"
assert by_name["음식"].representative_vector == [0.9] * DIM, "Spring 벡터가 덮였다"
assert by_name["부동산"].category_id == search.stable_id("부동산")

# 14) 저장소도 Spring 도 비면 기본 후보로 콜드 스타트한다.
stages.category_store.load_category_vectors = lambda user_id: {}
assert [c.name for c in stages.build_candidates(1, [])] == stages.DEFAULT_CATEGORIES

# 15) 시드는 user_id=0, count=0 으로 들어간다 — 사용자 centroid 와 id 가 겹치지 않아
#     단어 임베딩이 문장 centroid 에 누적되는 경로가 없다.
client = FakeClient()
use(client)
written = category_store.put_seed_category_vectors({"쇼핑": [0.2] * DIM, "여행": [0.3] * DIM})
assert written == 2
seed = stored_doc(client, "0:쇼핑")
assert seed["user_id"] == category_store.SEED_USER_ID == 0
assert seed["count"] == 0 and seed["vector"] == [0.2] * DIM
assert seed["model"] == MODEL and seed["dim"] == DIM

# 같은 이름을 사용자 문서로 쓰면 시드와 별개 문서다(누적 안 됨).
category_store.upsert_category_vector(1, "쇼핑", [1.0] * DIM)
assert stored_doc(client, "1:쇼핑")["vector"] == [1.0] * DIM, "시드가 centroid 에 섞였다"
assert stored_doc(client, "1:쇼핑")["count"] == 1
assert stored_doc(client, "0:쇼핑")["count"] == 0, "시드가 갱신됐다"

# 16) 시드 준비는 멱등 — 이미 있는 이름은 다시 임베딩하지 않는다.
embed_calls: list[list[str]] = []


def fake_embed(texts, purpose="DOCUMENT"):
    embed_calls.append(list(texts))
    return MODEL, [[0.5] * DIM for _ in texts]


stages.gemini_client.embed = fake_embed
stages.category_store.load_category_vectors = lambda user_id: {
    normalize_name(name): {"name": name, "vector": [0.5] * DIM, "count": 0}
    for name in stages.DEFAULT_CATEGORIES
}
assert stages.seed_default_category_vectors() == 0
assert embed_calls == [], "이미 있는 시드를 다시 임베딩했다"

# 빠진 이름만 골라 배치 1회로 임베딩한다.
written_names: list[str] = []
stages.category_store.load_category_vectors = lambda user_id: {
    normalize_name(name): {"name": name, "vector": [0.5] * DIM, "count": 0}
    for name in stages.DEFAULT_CATEGORIES[:-2]
}
stages.category_store.put_seed_category_vectors = lambda mapping: (
    written_names.extend(mapping) or len(mapping)
)
assert stages.seed_default_category_vectors() == 2
assert len(embed_calls) == 1, f"임베딩 호출 {len(embed_calls)}회 (배치 아님)"
assert embed_calls[0] == stages.DEFAULT_CATEGORIES[-2:] == written_names

# 17) 시드 준비가 터져도 기동을 막지 않는다.
def boom(*a, **kw):
    raise RuntimeError("gemini down")


stages.gemini_client.embed = boom
assert stages.seed_default_category_vectors() == 0


# 18) 다른 사용자가 만든 카테고리는 "이름만" 후보로 빌려 온다.
#     아이콘·Spring taxonomy 가 이름 단위 전역이라(같은 이름 = 같은 stable_id =
#     같은 아이콘 파일) 판정 후보만 사용자별로 갇혀 있으면 같은 개념이 사람마다
#     다른 이름으로 갈라진다. 이름은 빌리되 centroid(개인 데이터)는 안 빌린다.
category_store.load_category_vectors = REAL_LOAD_CATEGORY_VECTORS
category_store.put_seed_category_vectors = REAL_PUT_SEED_CATEGORY_VECTORS


class TwoQueryClient(FakeClient):
    """1회차는 문서(시드+본인), 2회차는 이름 집계(타 사용자)를 주는 가짜.

    foreign 은 {이름: 그 이름을 쓰는 사용자 수} — terms 집계 버킷으로 돌려준다.
    """

    def __init__(self, own, foreign, other_doc_count=0):
        super().__init__()
        self.own, self.foreign, self.calls = own, foreign, 0
        self.other_doc_count = other_doc_count   # 상한에 걸려 잘린 몫

    def search(self, index, body):
        self.queries.append(body)
        self.calls += 1
        if self.calls == 1:
            return {"hits": {"total": {"value": len(self.own)},
                             "hits": [{"_source": r} for r in self.own]}}
        buckets = [{"key": name, "doc_count": n}
                   for name, n in sorted(self.foreign.items(),
                                         key=lambda kv: -kv[1])]
        return {"hits": {"total": {"value": 0}, "hits": []},
                "aggregations": {"names": {
                    "buckets": buckets,
                    "sum_other_doc_count": self.other_doc_count}}}


client = TwoQueryClient(
    own=[{"name": "쇼핑", "vector": [0.3] * DIM, "count": 4, "category_id": 11}],
    foreign={"부동산": 3, "쇼핑": 7},
)
use(client)
loaded = category_store.load_category_vectors(2)

# 타 사용자 이름이 후보로 들어왔다 — 벡터 없이, count=0 으로.
assert "부동산" in loaded, loaded.keys()
assert loaded["부동산"]["vector"] is None, "남의 centroid 를 빌려 왔다"
assert loaded["부동산"]["count"] == 0
assert loaded["부동산"]["category_id"] == search.stable_id("부동산"), "아이콘 키와 어긋난다"
# 본인 centroid 가 있는 이름은 남의 것에 덮이지 않는다.
assert loaded["쇼핑"]["vector"] == [0.3] * DIM and loaded["쇼핑"]["count"] == 4

# 2회차 질의는 시드·본인을 제외하고 "이름" 만 집계한다 — 문서 본문을 읽지 않으므로
# centroid 가 애초에 넘어오지 않고, 상한이 서로 다른 이름 수가 된다(사본이 자리를
# 먹지 않는다). 위 foreign 에서 "쇼핑" 을 7명이 쓰지만 한 칸만 차지한 것이 그 증거다.
shared_query = client.queries[1]
assert shared_query["query"]["bool"]["must_not"] == [{"terms": {"user_id": [0, 2]}}]
assert shared_query["size"] == 0, "집계만 필요한데 문서를 읽어 왔다"
assert shared_query["aggs"]["names"]["terms"]["field"] == "name"
assert shared_query["aggs"]["names"]["terms"]["size"] == category_store._SHARED_NAME_LIMIT
assert "_source" not in shared_query, "문서 본문(벡터 포함)을 읽을 여지를 남겼다"

# 빌려 온 이름은 프롬프트 후보로 들어가되 벡터 판정에서는 빠진다(시드와 같은 취급).
stages.category_store.load_category_vectors = lambda user_id: loaded
merged = stages.build_candidates(2, [])
borrowed = next(c for c in merged if c.name == "부동산")
assert borrowed.representative_vector is None, "남의 이름이 벡터 판정에 들어갔다"
assert borrowed.category_id is None, "빌린 이름에 id 가 붙었다(신규로 남겨야 한다)"
own = next(c for c in merged if c.name == "쇼핑")
assert own.representative_vector == [0.3] * DIM and own.category_id == 11
category_store.load_category_vectors = REAL_LOAD_CATEGORY_VECTORS

# 19) 사용자가 하나뿐이면 완전한 무동작 — 단일 사용자 실측과 후보가 같다.
client = TwoQueryClient(
    own=[{"name": "쇼핑", "vector": [0.3] * DIM, "count": 4, "category_id": 11}],
    foreign={},
)
use(client)
assert set(category_store.load_category_vectors(1)) == {"쇼핑"}

# 20) 공유 이름 조회가 터져도 판정은 본인 후보로 진행한다.
class SharedBoomClient(TwoQueryClient):
    def search(self, index, body):
        self.calls += 1
        if self.calls == 1:
            return {"hits": {"total": {"value": len(self.own)},
                             "hits": [{"_source": r} for r in self.own]}}
        raise FakeError(500)


client = SharedBoomClient(
    own=[{"name": "쇼핑", "vector": [0.3] * DIM, "count": 4, "category_id": 11}],
    foreign={},
)
use(client)
assert set(category_store.load_category_vectors(3)) == {"쇼핑"}


# 21) OCR 교정본이 색인에 남아 6-2 상세까지 닿는다. 카테고리 벡터는 아니지만 같은
#     가짜 OpenSearch 로 검증되고, 끊기면 조용하다 — 장당 교정 토큰을 내고 결과를
#     100% 버리는데 아무 에러도 안 난다. 그래서 같은 관문에서 잡는다.
client = FakeClient()
use(client)
search._index_checked_at = 0.0
search.embedder.embed = lambda text: []      # 로컬 bge-m3 로딩 회피


def indexed(image_id=1, **kw):
    args = {"image_id": image_id, "user_id": 1, "title": "t", "summary": "s",
            "tags": [], "category_name": "쇼핑", "raw_text": "동으1서",
            "created_at": "2026-08-07T00:00:00+09:00"}
    search.index_document(**{**args, **kw})
    return client.docs[str(image_id)][0]


assert indexed(refined_text="동의서")["refined_text"] == "동의서"
# 교정본 없이 다시 색인해도(재분석·비융합 AGENT) 이미 있는 교정본을 지우지 않는다.
assert indexed()["refined_text"] == "동의서", "교정본이 None 으로 덮였다"
assert "refined_text" not in indexed(image_id=2), "빈 교정본이 키로 들어갔다"
# 원문은 그대로 남아야 한다 — 교정본은 추가 필드이고 검색은 raw_text 로 한다.
assert client.docs["1"][0]["raw_text"] == "동으1서"

# 6-2 가 읽는 상세 조회에 실려 나간다(없으면 None → 앱이 rawText 로 폴백).
assert search.get_image(1)["refined_text"] == "동의서"
assert search.get_image(2)["refined_text"] is None

# 매핑에도 있어야 한다(dynamic mapping 에 맡기면 text 로 굳어 저장 전용 의도가 깨진다).
props = search._index_body()["mappings"]["properties"]
assert props["refined_text"] == {"type": "text", "index": False}, props["refined_text"]

# 20-b) 상한을 넘으면 경고를 남긴다 — 조용히 자르지 않는다.
#       상한에 걸린다는 건 서로 다른 이름이 수백 개라는 뜻이고, 그건 파편화를 막으려고
#       만든 흡수 관문·프롬프트 재사용 규칙이 새고 있다는 신호다(상한을 올릴 일이 아니다).
import logging as _logging   # noqa: E402

captured: list[str] = []


class _Capture(_logging.Handler):
    def emit(self, record):
        captured.append(record.getMessage())


_handler = _Capture()
_logging.getLogger("app.category_store").addHandler(_handler)
client = TwoQueryClient(own=[], foreign={"부동산": 1}, other_doc_count=17)
use(client)
category_store.load_category_vectors(2)
_logging.getLogger("app.category_store").removeHandler(_handler)
assert any("파편화" in m for m in captured), captured

# 21) 상한 0 이면 아예 끈다(질의 1회).
client = TwoQueryClient(own=[], foreign={"부동산": 1})
use(client)
original_limit = category_store._SHARED_NAME_LIMIT
category_store._SHARED_NAME_LIMIT = 0
try:
    assert category_store.load_category_vectors(2) == {}
    assert client.calls == 1, "끈 상태로 공유 이름을 조회했다"
finally:
    category_store._SHARED_NAME_LIMIT = original_limit

print("OK — centroid 누적·무효화·409 재시도·후보 병합·전역 시드·이름 공유"
      "·OCR 교정본 배선 전부 통과")
