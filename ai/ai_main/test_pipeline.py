"""파이프라인 자체 점검 (Gemini·Spring·S3 호출 없이 동작).

    GEMINI_API_KEY=x INTERNAL_TOKEN=t python test_pipeline.py
"""

import asyncio
import os

os.environ.setdefault("GEMINI_API_KEY", "test-key")
os.environ.setdefault("INTERNAL_TOKEN", "test-token")

from fastapi.testclient import TestClient  # noqa: E402

from app import spring_client, stages  # noqa: E402
from app.category import cosine_similarity, resolve_category  # noqa: E402
from app.main import app  # noqa: E402
from app.schemas import CategoryCandidate, KnowledgeCandidates  # noqa: E402

HEADERS = {"X-Internal-Token": os.environ["INTERNAL_TOKEN"]}

# 이름 길이로 결정되는 2차원 벡터. 네트워크 없이 유사도 판정을 재현한다.
fake_embed = lambda texts: [[float(len(t)), 0.0] for t in texts]


def test_cosine():
    assert cosine_similarity([1, 0], [1, 0]) == 1.0
    assert cosine_similarity([1, 0], [0, 1]) == 0.0
    assert cosine_similarity([0, 0], [1, 0]) == 0.0


def test_resolve_category():
    candidates = [
        CategoryCandidate(category_id=1, name="쇼핑", representative_vector=[1.0, 0.0]),
        CategoryCandidate(category_id=2, name="여행", representative_vector=[0.0, 1.0]),
    ]

    # 1) AGENT 가 기존 후보 이름을 고르면 이름 매칭 (대소문자·전각 정규화 포함)
    r = resolve_category([0.0, 1.0], "쇼핑", candidates, 0.8, fake_embed)
    assert r.matched_by == "name" and r.category_id == 1 and not r.created

    # 2) 이름 불일치 + 유사도 충분 → 기존 카테고리 연결
    r = resolve_category([1.0, 0.0], "온라인쇼핑몰", candidates, 0.8, fake_embed)
    assert r.matched_by == "embedding" and r.name == "쇼핑" and not r.created

    # 3) 임계값 미달 → 신규 카테고리 (categoryId 없음 = Spring 이 생성)
    r = resolve_category([1.0, -1.0], "우주항공", candidates, 0.99, fake_embed)
    assert r.created and r.category_id is None and r.name == "우주항공"

    # 4) DB 에 없는 기본 카테고리를 골랐으면 created=True (Spring 이 새로 만들어야 함)
    seed = [CategoryCandidate(category_id=None, name="기타")]
    r = resolve_category([1.0, 0.0], "기타", seed, 0.8, fake_embed)
    assert r.matched_by == "name" and r.created and r.category_id is None

    # 5) 후보가 아예 없으면 신규
    r = resolve_category([1.0, 0.0], "쇼핑", [], 0.8, fake_embed)
    assert r.created and r.matched_by.startswith("new")


def test_empty_ocr_skips_llm():
    """OCR 텍스트가 비면 모델을 호출하지 않고 EMPTY 로 끝난다."""
    status, result = stages.run_llm("   ")
    assert status == "EMPTY" and result["informative"] is False


def test_stage_input_validation():
    client = TestClient(app)
    # 토큰 없음
    assert client.post("/internal/v1/analyze", json={
        "jobId": 1, "imageId": 1, "userId": 1, "stage": "LLM", "input": {}
    }).status_code == 401
    # stage 별 필수 input 누락
    r = client.post("/internal/v1/analyze", headers=HEADERS, json={
        "jobId": 1, "imageId": 1, "userId": 1, "stage": "AGENT", "input": {}
    })
    assert r.status_code == 400 and r.json()["detail"]["missing"] == ["ocr", "imageAnalysis"]
    # 빈 texts
    assert client.post("/internal/v1/embed", headers=HEADERS,
                       json={"texts": []}).status_code == 400


def test_job_idempotency():
    from app.jobs import JobRegistry
    registry = JobRegistry()
    assert registry.try_claim(1, "LLM") is True
    assert registry.try_claim(1, "LLM") is False          # 같은 job+stage 는 중복
    assert registry.try_claim(1, "AGENT") is True         # stage 가 다르면 별개
    registry.mark(1, "LLM", "COMPLETED")
    assert registry.status(1, "LLM") == "COMPLETED"


def test_upload_endpoint():
    """멀티파트 업로드 → 정보성 분기 / 전체 실행 둘 다."""
    calls: list[str] = []

    stages.run_server_ocr = lambda b, m="image/jpeg": (
        calls.append("ocr"), "교보문고 C++ 입문 32,000원")[1]
    stages.analyze_image_bytes = lambda b, m="image/jpeg": (
        calls.append("vision"),
        {"description": "서점 상품 화면", "detectedTexts": [], "objects": []})[1]
    stages.run_agent_generation = lambda *a, **kw: (
        calls.append("agent"),
        {"title": "C++ 입문", "summary": "교보문고 C++ 입문서", "tags": ["C++"],
         "categories": ["쇼핑"], "key_information": ["가격: 32,000원"],
         "analysis_confidence": 0.9})[1]
    stages.gemini_client.embed = lambda texts, purpose="DOCUMENT": ("fake", fake_embed(texts))
    # Spring 이 없으므로 10-5 는 빈 후보 → DEFAULT_CATEGORIES 로 진행
    async def no_spring(user_id, **kw):
        return KnowledgeCandidates(user_id=user_id)
    spring_client.fetch_knowledge_candidates = no_spring
    stages.spring_client.fetch_knowledge_candidates = no_spring

    client = TestClient(app)

    # 비정보성 → '기타', 비전·AGENT 미실행
    stages.run_llm = lambda text: ("COMPLETED", {
        "informative": False, "confidence": 0.1, "reason": "UI 요소뿐"})
    r = client.post("/internal/v1/analyze/upload", headers=HEADERS,
                    files={"image": ("shot.png", b"fakebytes", "image/png")})
    assert r.status_code == 200, r.text
    body = r.json()
    assert body["categories"] == ["기타"] and calls == ["ocr"]
    assert body["structuredData"] == {"type": None, "fields": {}}

    # 정보성 → 전체 실행. ocrText 를 주면 서버 OCR 은 건너뛴다.
    calls.clear()
    stages.run_llm = lambda text: ("COMPLETED", {
        "informative": True, "confidence": 0.95, "reason": "상품 정보"})
    r = client.post("/internal/v1/analyze/upload", headers=HEADERS,
                    files={"image": ("shot.png", b"fakebytes", "image/png")},
                    data={"ocrText": "교보문고 C++ 입문 32,000원", "userId": 7})
    assert r.status_code == 200, r.text
    body = r.json()
    assert calls == ["vision", "agent"]                    # 서버 OCR 미호출
    assert body["categories"] == ["쇼핑"]
    assert body["keyInformation"] == ["가격: 32,000원"]
    assert body["categoryCreated"] is True                 # 기본 후보라 DB 에 없음
    assert body["structuredData"] == {"type": None, "fields": {}}

    # 토큰 없음 401 / 빈 파일 400
    assert client.post("/internal/v1/analyze/upload",
                       files={"image": ("a.png", b"x", "image/png")}).status_code == 401
    assert client.post("/internal/v1/analyze/upload", headers=HEADERS,
                       files={"image": ("a.png", b"", "image/png")}).status_code == 400


if __name__ == "__main__":
    test_cosine()
    test_resolve_category()
    test_empty_ocr_skips_llm()
    test_stage_input_validation()
    test_job_idempotency()
    test_upload_endpoint()   # 여기서 Gemini/Spring 호출을 스텁으로 교체한다
    print("OK: 카테고리 판정 / stage 검증 / 멱등 / 업로드 파이프라인 정상")
