import json
import math
import os
import random
import threading
import time
import urllib.request
from datetime import datetime, timezone
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

DELAY_MS = int(os.getenv("MOCK_AI_DELAY_MS", "10"))
# 동기 호출 경로의 지연 시뮬레이션. 실제 LLM은 문서 생성에 수 초가 걸리므로
# 부하 시나리오에서 스레드 점유 특성을 보려면 이 값을 늘려서 돌린다.
DOC_DELAY_MS = int(os.getenv("MOCK_AI_DOC_DELAY_MS", "200"))
SEARCH_DELAY_MS = int(os.getenv("MOCK_AI_SEARCH_DELAY_MS", "50"))
INTERNAL_TOKEN = os.getenv("INTERNAL_TOKEN", "")


def utc_now_z():
    return datetime.now(timezone.utc).isoformat(timespec="milliseconds").replace("+00:00", "Z")


def mock_document(body):
    """api-server DocumentAiClient가 호출하는 POST /internal/v1/documents 응답.

    실제 AI 스키마(DocumentResponse)와 같은 키를 내려주되 내용은 요청 이미지
    메타데이터로 결정적으로 만든다. sourceImageIds는 요청의 imageId를 그대로
    돌려줘야 api-server가 document_image를 일관되게 저장한다.
    """
    images = body.get("images", [])
    image_ids = [img["imageId"] for img in images]
    titles = [img.get("title") or f"이미지 {img['imageId']}" for img in images]
    section_lines = "\n\n".join(
        f"## {title}\n\n- OCR 요약: {(img.get('ocr') or {}).get('rawText', '')[:120] or '(내용 없음)'}\n- 태그: {', '.join(img.get('tags') or []) or '없음'}"
        for title, img in zip(titles, images)
    )
    title = body.get("title") or f"모의 문서 ({len(images)}장 구성)"
    return {
        "title": title,
        "summary": f"{len(images)}장의 이미지를 정리한 성능 테스트용 모의 문서입니다.",
        "sections": [
            {"heading": t, "body": "성능 테스트 모의 섹션", "bullets": [], "imageIds": [i]}
            for t, i in zip(titles, image_ids)
        ],
        "markdown": f"# {title}\n\n성능 테스트용으로 생성된 결정적 모의 문서입니다.\n\n{section_lines}\n",
        "sourceImageIds": image_ids,
        "skipped": [],
        "modelVersion": "mock-http-1.0",
        "generatedAt": utc_now_z(),
    }


def mock_semantic_search(body):
    """analysis-worker FastApiSemanticSearchClient가 호출하는
    POST /internal/v1/images/search/semantic 응답(이벤트 envelope).

    존재하지 않는 imageId를 지어내면 api-server 조회 모델과 어긋나므로
    항상 빈 결과를 돌려준다 — 부하 측정 대상은 스트림 왕복이지 검색 품질이 아니다.
    """
    return {
        "eventType": "IMAGE_SEARCH_COMPLETED",
        "version": 1,
        "payload": {
            "correlationId": body.get("correlationId"),
            "total": 0,
            "page": body.get("page", 0),
            "size": body.get("size", 10) or 10,
            "hits": [],
        },
    }


# ── 시딩 데이터 분포 제어 ──
# 분석 콜백 결과를 imageId 기반으로 결정적으로 만든다. 같은 imageId면 항상 같은
# 결과라 시딩을 다시 돌려도 데이터가 흔들리지 않는다.
# - category 로테이션: 사용자마다 카테고리 여러 개가 생기고 category_view.image_count가 분산된다
# - 제목·태그·OCR에 검색 키워드 포함: keyword 검색과 시맨틱 시나리오가 실제로 히트한다
#   (KEYWORDS는 kakao-user-capacity.js 07_semantic 흐름의 키워드 세트와 반드시 일치)
# - scheduleData(약 1/SCHEDULE_MOD): api의 ScheduleCreationService가 일정을 실제 생성한다
# - documentVector: analysis-db 임베딩으로 저장되어 /similar가 실제 pgvector 검색이 된다
CATEGORIES = [c.strip() for c in os.getenv(
    "MOCK_AI_CATEGORIES", "영수증,쇼핑,문서,일정,여행,음식,기타").split(",") if c.strip()]
KEYWORDS = ["영수증", "일정", "쇼핑", "문서", "여행"]
SCHEDULE_MOD = int(os.getenv("MOCK_AI_SCHEDULE_MOD", "7"))
EMBEDDING_DIM = int(os.getenv("MOCK_AI_EMBEDDING_DIM", "768"))


def _unit_vector(seed):
    rng = random.Random(seed)
    values = [rng.uniform(-1.0, 1.0) for _ in range(EMBEDDING_DIM)]
    norm = math.sqrt(sum(v * v for v in values)) or 1.0
    return [v / norm for v in values]


def _document_vector(image_id, category_index):
    """카테고리별 클러스터 벡터. 완전 난수는 768차원에서 서로 직교(코사인 ≈ 0)라
    worker의 유사도 임계값(MIN_SCORE 0.6)을 절대 못 넘는다. 카테고리 기저 벡터에
    이미지별 노이즈를 섞으면 같은 카테고리끼리 코사인 ≈ 0.97이 되어 /similar가
    같은 카테고리 이미지를 실제로 돌려준다 — 의미상으로도 그럴듯한 결과다."""
    base = _unit_vector(f"category-{category_index}")
    noise = _unit_vector(f"image-{image_id}")
    mixed = [0.85 * b + 0.15 * n for b, n in zip(base, noise)]
    norm = math.sqrt(sum(v * v for v in mixed)) or 1.0
    return [round(v / norm, 6) for v in mixed]


def mock_analysis_result(image_id):
    category_index = image_id % len(CATEGORIES)
    category = CATEGORIES[category_index]
    keyword = KEYWORDS[image_id % len(KEYWORDS)]
    result = {
        "title": f"{keyword} {category} 캡처 {image_id}",
        "summary": f"{category} 화면에서 저장한 {keyword} 관련 스크린샷입니다.",
        "ocrRefinedText": f"{keyword} {category} performance test {image_id}",
        "informative": True,
        "category": category,
        # worker의 publishInitialCategory는 categoryId와 이름이 둘 다 있어야
        # INITIAL_CATEGORY_RESOLVED를 발행한다(누락 시 카테고리 행이 안 생긴다).
        # 실제 AI처럼 이름당 고정 id를 준다 — 같은 이름이면 언제나 같은 id.
        "categoryId": 9000 + category_index,
        "tags": [category, keyword, "performance-test"],
        "keyInformation": [],
        "analysisConfidence": 1.0,
        "documentVector": _document_vector(image_id, category_index),
    }
    if SCHEDULE_MOD > 0 and image_id % SCHEDULE_MOD == 0:
        # ScheduleCreationService 계약: type은 "schedule", fields는 startYear/Month/Day/Time.
        result["scheduleData"] = {
            "type": "schedule",
            "fields": {
                "startYear": 2026,
                "startMonth": 1 + image_id % 12,
                "startDay": 1 + image_id % 28,
                "startTime": f"{9 + image_id % 9:02d}:00",
            },
        }
    return result


def callback(body):
    time.sleep(DELAY_MS / 1000)
    payload = {
        "jobId": body["jobId"],
        "imageId": body["imageId"],
        "stage": body.get("stage", "FULL"),
        "status": "COMPLETED",
        "result": mock_analysis_result(int(body["imageId"])),
        "error": None,
        "modelVersion": "mock-http-1.0",
        "completedAt": datetime.now(timezone.utc).isoformat(),
    }
    request = urllib.request.Request(
        body["callbackUrl"],
        data=json.dumps(payload).encode(),
        headers={"Content-Type": "application/json", "X-Internal-Token": INTERNAL_TOKEN},
        method="POST",
    )
    try:
        with urllib.request.urlopen(request, timeout=10):
            pass
    except Exception as error:
        print(f"callback failed jobId={body.get('jobId')}: {error}", flush=True)


class Handler(BaseHTTPRequestHandler):
    def read_body(self):
        if self.headers.get("Transfer-Encoding", "").lower() != "chunked":
            return self.rfile.read(int(self.headers.get("Content-Length", "0")))
        chunks = bytearray()
        while True:
            size = int(self.rfile.readline().strip().split(b";", 1)[0], 16)
            if size == 0:
                self.rfile.readline()
                break
            chunks.extend(self.rfile.read(size))
            self.rfile.read(2)
        return bytes(chunks)

    def do_GET(self):
        if self.path == "/health":
            self.send_response(200)
            self.end_headers()
            self.wfile.write(b"ok")
            return
        self.send_error(404)

    def send_json(self, status, payload):
        encoded = json.dumps(payload, ensure_ascii=False).encode()
        self.send_response(status)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(encoded)))
        self.end_headers()
        self.wfile.write(encoded)

    def do_POST(self):
        if self.path == "/internal/v1/analyze":
            body = json.loads(self.read_body())
            threading.Thread(target=callback, args=(body,), daemon=True).start()
            self.send_json(202, {
                "jobId": body["jobId"], "imageId": body["imageId"],
                "stage": body.get("stage", "FULL"), "accepted": True,
                "status": "ACCEPTED", "error": None,
            })
            return
        if self.path == "/internal/v1/documents":
            body = json.loads(self.read_body())
            time.sleep(DOC_DELAY_MS / 1000)
            self.send_json(200, mock_document(body))
            return
        if self.path == "/internal/v1/images/search/semantic":
            body = json.loads(self.read_body())
            time.sleep(SEARCH_DELAY_MS / 1000)
            self.send_json(200, mock_semantic_search(body))
            return
        self.send_error(404)

    def log_message(self, fmt, *args):
        return


ThreadingHTTPServer(("0.0.0.0", int(os.getenv("PORT", "8000"))), Handler).serve_forever()
