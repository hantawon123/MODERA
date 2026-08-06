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

from app import search, stages                       # noqa: E402
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
        return {"_source": source, "_seq_no": seq_no, "_primary_term": primary_term}

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


def use(client: FakeClient) -> None:
    search._client = lambda: client
    search._category_index_checked_at = 0.0


def stored_doc(client: FakeClient, doc_id: str) -> dict:
    return client.docs[doc_id][0]


# 1) 신규 카테고리는 요약 임베딩 그대로 저장되고 count=1 이다.
client = FakeClient()
use(client)
search.upsert_category_vector(1, "쇼핑", [1.0] * DIM)
doc = stored_doc(client, "1:쇼핑")
assert doc["count"] == 1, doc["count"]
assert doc["vector"] == [1.0] * DIM
assert doc["dim"] == DIM and doc["model"] == MODEL
assert search._category_index() in client.indices.created

# 2) 두 번째 이미지는 누적 평균으로 합쳐진다(마지막 값 덮어쓰기가 아니다).
search.upsert_category_vector(1, "쇼핑", [0.0] * DIM)
doc = stored_doc(client, "1:쇼핑")
assert doc["count"] == 2, doc["count"]
assert doc["vector"] == [0.5] * DIM, doc["vector"][:3]

# 세 번째도 개수 가중이 맞아야 한다 — (0.5*2 + 2.0)/3 = 1.0
search.upsert_category_vector(1, "쇼핑", [2.0] * DIM)
doc = stored_doc(client, "1:쇼핑")
assert doc["count"] == 3 and doc["vector"] == [1.0] * DIM, doc["vector"][:3]

# 3) 이름 정규화가 같으면 같은 문서다(대소문자·공백으로 갈라지지 않는다).
search.upsert_category_vector(1, "  쇼핑 ", [1.0] * DIM)
assert stored_doc(client, "1:쇼핑")["count"] == 4
assert len(client.docs) == 1, list(client.docs)

# 4) 사용자별로 격리된다.
search.upsert_category_vector(2, "쇼핑", [7.0] * DIM)
assert stored_doc(client, "2:쇼핑")["count"] == 1
assert stored_doc(client, "1:쇼핑")["count"] == 4

# 5) 임베딩 모델이 바뀐 기존 행은 이어붙이지 않고 새로 시작한다.
client.docs["1:여행"] = ({"user_id": 1, "name": "여행", "vector": [9.0] * DIM,
                          "count": 50, "model": "old-model", "dim": DIM}, 1, 1)
search.upsert_category_vector(1, "여행", [1.0] * DIM)
doc = stored_doc(client, "1:여행")
assert doc["count"] == 1 and doc["vector"] == [1.0] * DIM, doc["count"]
assert doc["model"] == MODEL

# 6) 차원이 다른 기존 행도 마찬가지다(zip 이 조용히 잘라먹으면 안 된다).
client.docs["1:금융"] = ({"user_id": 1, "name": "금융", "vector": [9.0] * (DIM - 1),
                          "count": 5, "model": MODEL, "dim": DIM - 1}, 1, 1)
search.upsert_category_vector(1, "금융", [1.0] * DIM)
doc = stored_doc(client, "1:금융")
assert doc["count"] == 1 and len(doc["vector"]) == DIM

# 7) 409(동시 갱신)는 읽기부터 재시도한다 — 갱신을 잃지 않는다.
client.force_conflicts = 2
search.upsert_category_vector(1, "쇼핑", [1.0] * DIM)
assert stored_doc(client, "1:쇼핑")["count"] == 5, stored_doc(client, "1:쇼핑")["count"]

# 8) 재시도 한도를 넘으면 예외를 삼킨다(분석을 실패시키지 않는다).
client.force_conflicts = 99
search.upsert_category_vector(1, "쇼핑", [1.0] * DIM)
assert stored_doc(client, "1:쇼핑")["count"] == 5, "실패한 갱신이 반영됐다"

# 9) 빈 벡터는 아무것도 쓰지 않는다.
before = len(client.docs)
search.upsert_category_vector(1, "없음", [])
assert len(client.docs) == before

# 10) 조회는 현재 모델·차원으로 필터하고, 시드(user_id=0)를 함께 읽는다.
client = FakeClient()
use(client)
client.hits = [{"name": "쇼핑", "vector": [0.5] * DIM, "count": 3}]
loaded = search.load_category_vectors(7)
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
loaded = search.load_category_vectors(7)
assert loaded["쇼핑"]["vector"] == [0.9] * DIM, "시드가 centroid 를 덮었다"
assert loaded["쇼핑"]["count"] == 4

# 11) 조회가 터지면 빈 dict — 판정은 이름 임베딩으로 degrade 한다.
class BoomClient(FakeClient):
    def search(self, index, body):
        raise FakeError(500)


use(BoomClient())
assert search.load_category_vectors(7) == {}

# 12) 저장된 카테고리는 후보로 되살아나고, 대표 벡터가 채워진다.
#     (= 재기동 뒤에도 AGENT 가 만든 카테고리가 프롬프트 후보에 남는다)
stages.search.load_category_vectors = lambda user_id: {
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
stages.search.load_category_vectors = lambda user_id: {
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
stages.search.load_category_vectors = lambda user_id: {}
assert [c.name for c in stages.build_candidates(1, [])] == stages.DEFAULT_CATEGORIES

# 15) 시드는 user_id=0, count=0 으로 들어간다 — 사용자 centroid 와 id 가 겹치지 않아
#     단어 임베딩이 문장 centroid 에 누적되는 경로가 없다.
client = FakeClient()
use(client)
written = search.put_seed_category_vectors({"쇼핑": [0.2] * DIM, "여행": [0.3] * DIM})
assert written == 2
seed = stored_doc(client, "0:쇼핑")
assert seed["user_id"] == search.SEED_USER_ID == 0
assert seed["count"] == 0 and seed["vector"] == [0.2] * DIM
assert seed["model"] == MODEL and seed["dim"] == DIM

# 같은 이름을 사용자 문서로 쓰면 시드와 별개 문서다(누적 안 됨).
search.upsert_category_vector(1, "쇼핑", [1.0] * DIM)
assert stored_doc(client, "1:쇼핑")["vector"] == [1.0] * DIM, "시드가 centroid 에 섞였다"
assert stored_doc(client, "1:쇼핑")["count"] == 1
assert stored_doc(client, "0:쇼핑")["count"] == 0, "시드가 갱신됐다"

# 16) 시드 준비는 멱등 — 이미 있는 이름은 다시 임베딩하지 않는다.
embed_calls: list[list[str]] = []


def fake_embed(texts, purpose="DOCUMENT"):
    embed_calls.append(list(texts))
    return MODEL, [[0.5] * DIM for _ in texts]


stages.gemini_client.embed = fake_embed
stages.search.load_category_vectors = lambda user_id: {
    normalize_name(name): {"name": name, "vector": [0.5] * DIM, "count": 0}
    for name in stages.DEFAULT_CATEGORIES
}
assert stages.seed_default_category_vectors() == 0
assert embed_calls == [], "이미 있는 시드를 다시 임베딩했다"

# 빠진 이름만 골라 배치 1회로 임베딩한다.
written_names: list[str] = []
stages.search.load_category_vectors = lambda user_id: {
    normalize_name(name): {"name": name, "vector": [0.5] * DIM, "count": 0}
    for name in stages.DEFAULT_CATEGORIES[:-2]
}
stages.search.put_seed_category_vectors = lambda mapping: (
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

print("OK — centroid 누적·무효화·409 재시도·후보 병합·전역 시드 전부 통과")
