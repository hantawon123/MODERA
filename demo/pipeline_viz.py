"""실시간 AI 파이프라인 시각화 사이드카 (읽기 전용 관찰자).

기존 서버 코드는 전혀 건드리지 않는다. 서버가 이미 노출하는 API
(/api/v1/analysis/jobs, /api/v1/home, /api/v1/images/{id})를 폴링해
분석 파이프라인(정보성 판정 → 이미지 분석 → 메타데이터 생성 → 색인)의
진행 상태를 브라우저에 실시간으로 그려 준다.

하는 일은 딱 둘:
  1. GET /            → index.html 서빙
  2. 그 외 모든 경로   → UPSTREAM 으로 그대로 중계(reverse proxy)

중계가 필요한 이유: FastAPI 에 CORS 헤더가 없어서 브라우저가 다른 오리진의
API 를 직접 fetch 하면 막힌다. 이 사이드카가 같은 오리진(localhost)에서
HTML 과 API 를 함께 내주면 CORS 문제가 사라진다. 서버↔서버 호출(여기)은
CORS 대상이 아니라 그대로 통과한다.

실행:
    python demo/pipeline_viz.py                 # 기본: 공개 서버로 중계
    python demo/pipeline_viz.py http://127.0.0.1:8001   # 서버 호스트에서 로컬 중계
    #   ↑ 포트는 관찰 대상 스택의 호스트 포트다. 배포본 8000, full-stage 8001.
    VIZ_PORT=9000 python demo/pipeline_viz.py

그리고 브라우저에서 http://localhost:8090 을 연다.
"""

import os
import ssl
import sys
import urllib.error
import urllib.request
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path

UPSTREAM = (sys.argv[1] if len(sys.argv) > 1
            else os.environ.get("VIZ_UPSTREAM", "https://i15d207.p.ssafy.io")).rstrip("/")
PORT = int(os.environ.get("VIZ_PORT", "8090"))
INDEX = Path(__file__).with_name("index.html")
_SSL = ssl.create_default_context()


class Handler(BaseHTTPRequestHandler):
    def log_message(self, *_):  # 요청마다 찍히는 기본 로그를 끈다
        pass

    def do_GET(self):
        if self.path in ("/", "/index.html"):
            body = INDEX.read_bytes()
            self.send_response(200)
            self.send_header("Content-Type", "text/html; charset=utf-8")
            self.send_header("Content-Length", str(len(body)))
            self.end_headers()
            self.wfile.write(body)
            return
        self._proxy()

    def _proxy(self):
        url = UPSTREAM + self.path
        req = urllib.request.Request(url, headers={"Accept": "*/*"})
        try:
            with urllib.request.urlopen(req, timeout=30, context=_SSL) as up:
                body = up.read()
                status = up.status
                ctype = up.headers.get("Content-Type", "application/octet-stream")
        except urllib.error.HTTPError as e:  # 4xx/5xx 도 그대로 전달
            body, status = e.read(), e.code
            ctype = e.headers.get("Content-Type", "application/json")
        except Exception as e:  # 연결 실패 → 502 로 알려 준다
            body = f'{{"error":"upstream unreachable","detail":{str(e)!r}}}'.encode()
            status, ctype = 502, "application/json"
        self.send_response(status)
        self.send_header("Content-Type", ctype)
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        try:
            self.wfile.write(body)
        except (BrokenPipeError, ConnectionResetError):
            pass  # 브라우저가 이미지를 취소하면 흔히 난다. 무시.


def main():
    if not INDEX.exists():
        sys.exit(f"index.html 이 없습니다: {INDEX}")
    srv = ThreadingHTTPServer(("0.0.0.0", PORT), Handler)
    print(f"▶ 파이프라인 시각화: http://localhost:{PORT}")
    print(f"  중계 대상(UPSTREAM): {UPSTREAM}")
    print("  Ctrl+C 로 종료")
    try:
        srv.serve_forever()
    except KeyboardInterrupt:
        srv.shutdown()


if __name__ == "__main__":
    main()
