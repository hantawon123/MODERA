"""FULL stage 통합 테스트.

동시에 N개의 FULL 요청을 ai-server 로 보내고, 콜백이 전부 돌아오는지 본다.
세마포어 재진입 데드락(MAX_CONCURRENT_STAGES 만큼 동시에 들어오면 전부 멈추던
문제)의 회귀 테스트를 겸한다 — 데드락이면 콜백이 하나도 오지 않는다.

콜백 수신처는 두 가지다.

  --sink            이 스크립트가 임시 HTTP 서버를 띄워 콜백을 직접 받는다.
                    worker DB 에 job 행이 없어도 되고, 콜백 바디를 그대로 볼 수 있다.
                    → 데드락·파이프라인 확인용. 이걸 먼저 돌린다.

  --callback-url    analysis-worker 로 직접 보낸다. worker 는 analysis_job 에 없는
                    jobId 면 "콜백 대상 job 없음 → 무시" 로 200 을 주고 끝낸다
                    (AnalysisCallbackService.handle 첫 분기). 실제 저장까지 보려면
                    LOCAL_DEV.md 의 job 행 삽입 절차를 먼저 밟을 것.

사용 예:

    python scripts/full_stage_test.py --sink --count 4 \
        --s3-keys u/1/local-test-1.png,u/1/local-test-2.png,\
u/1/local-test-3.png,u/1/local-test-4.png

    python scripts/full_stage_test.py --count 4 \
        --callback-url http://localhost:8081/internal/v1/callback/analysis
"""

import argparse
import json
import os
import sys
import threading
import time
from http.server import BaseHTTPRequestHandler, HTTPServer

import httpx

DEFAULT_AI = "http://localhost:8000"
DEFAULT_TOKEN = "local-dev-internal-token"

_received: list[dict] = []
_received_lock = threading.Lock()


class _CallbackHandler(BaseHTTPRequestHandler):
    """analysis-worker 의 콜백 엔드포인트를 흉내낸다(경로·토큰 검사 동일)."""

    def do_POST(self):  # noqa: N802 — BaseHTTPRequestHandler 규약
        length = int(self.headers.get("Content-Length", 0))
        raw = self.rfile.read(length)
        token = self.headers.get("X-Internal-Token")
        try:
            body = json.loads(raw)
        except Exception:
            body = {"_raw": raw[:200].decode("utf-8", "replace")}
        with _received_lock:
            _received.append({"token": token, "path": self.path, "body": body})
        self.send_response(200)
        self.send_header("Content-Type", "application/json")
        self.end_headers()
        self.wfile.write(b'{"received": true}')

    def log_message(self, *_args):
        pass  # 요청 로그는 우리가 직접 찍는다


def _start_sink(port: int) -> HTTPServer:
    server = HTTPServer(("0.0.0.0", port), _CallbackHandler)
    threading.Thread(target=server.serve_forever, daemon=True).start()
    print(f"[sink] 콜백 수신 대기: http://localhost:{port}/internal/v1/callback/analysis")
    return server


def _send(ai_url: str, token: str, body: dict) -> tuple[int, dict]:
    response = httpx.post(
        f"{ai_url.rstrip('/')}/internal/v1/analyze",
        json=body,
        headers={"X-Internal-Token": token},
        timeout=10,
    )
    try:
        return response.status_code, response.json()
    except Exception:
        return response.status_code, {"_text": response.text[:200]}


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--ai-url", default=os.environ.get("AI_URL", DEFAULT_AI))
    parser.add_argument("--token",
                        default=os.environ.get("INTERNAL_TOKEN", DEFAULT_TOKEN))
    parser.add_argument("--count", type=int, default=4,
                        help="동시 요청 수. MAX_CONCURRENT_STAGES 와 같게 두면 데드락 재현 조건")
    parser.add_argument("--s3-keys", default="",
                        help="콤마 구분. 개수가 모자라면 마지막 key 를 재사용한다")
    parser.add_argument("--job-id-base", type=int, default=9000)
    parser.add_argument("--user-id", type=int, default=1)
    parser.add_argument("--sink", action="store_true",
                        help="콜백을 이 스크립트가 직접 받는다")
    parser.add_argument("--sink-port", type=int, default=8099)
    parser.add_argument("--callback-url", default="",
                        help="--sink 없이 쓸 실제 콜백 주소(analysis-worker)")
    parser.add_argument("--timeout", type=int, default=180,
                        help="콜백을 기다리는 최대 초")
    args = parser.parse_args()

    keys = [k.strip() for k in args.s3_keys.split(",") if k.strip()]
    if not keys:
        keys = [f"u/{args.user_id}/local-test-{i}.png" for i in range(1, args.count + 1)]

    if args.sink:
        _start_sink(args.sink_port)
        # ai-server 가 컨테이너 안이면 localhost 는 자기 자신이라 닿지 않는다.
        callback_url = (
            f"http://host.docker.internal:{args.sink_port}/internal/v1/callback/analysis"
            if os.environ.get("AI_IN_DOCKER") == "true"
            else f"http://localhost:{args.sink_port}/internal/v1/callback/analysis"
        )
    else:
        callback_url = args.callback_url
        if not callback_url:
            print("--sink 또는 --callback-url 중 하나는 있어야 한다.", file=sys.stderr)
            return 2

    print(f"[요청] {args.count}건 동시 전송 → {args.ai_url}/internal/v1/analyze")
    print(f"[콜백] callbackUrl = {callback_url}\n")

    job_ids = [args.job_id_base + i for i in range(args.count)]
    results: dict[int, tuple[int, dict]] = {}
    threads = []

    def _fire(index: int) -> None:
        job_id = job_ids[index]
        body = {
            "jobId": job_id,
            "imageId": job_id,
            "userId": args.user_id,
            "stage": "FULL",
            "input": {"image": {"s3Key": keys[min(index, len(keys) - 1)]}},
            "options": {"maxTags": 10, "language": "ko"},
            "callbackUrl": callback_url,
        }
        results[job_id] = _send(args.ai_url, args.token, body)

    started = time.perf_counter()
    for i in range(args.count):
        thread = threading.Thread(target=_fire, args=(i,))
        thread.start()
        threads.append(thread)
    for thread in threads:
        thread.join()

    accepted = 0
    for job_id in job_ids:
        status, body = results.get(job_id, (0, {}))
        marker = "OK " if status == 202 else "!! "
        if status == 202:
            accepted += 1
        print(f"{marker}jobId={job_id} HTTP {status} {json.dumps(body, ensure_ascii=False)}")
    print(f"\n접수 {accepted}/{args.count} (소요 {time.perf_counter() - started:.2f}s)")

    if not args.sink:
        print("\n콜백은 analysis-worker 로직에서 확인할 것:")
        print("  docker logs -f modera-analysis-worker | grep -i 콜백")
        return 0 if accepted == args.count else 1

    print(f"\n[대기] 콜백 {args.count}건 (최대 {args.timeout}s)...")
    deadline = time.time() + args.timeout
    while time.time() < deadline:
        with _received_lock:
            if len(_received) >= args.count:
                break
        time.sleep(0.5)

    with _received_lock:
        got = list(_received)
    elapsed = time.perf_counter() - started
    print(f"\n[결과] 콜백 {len(got)}/{args.count} 수신 (전체 {elapsed:.2f}s)")
    for item in got:
        body = item["body"]
        result = body.get("result") or {}
        vector = result.get("documentVector") or []
        print(
            f"  jobId={body.get('jobId')} stage={body.get('stage')} "
            f"status={body.get('status')} token={'일치' if item['token'] else '없음'} "
            f"summary={str(result.get('summary'))[:40]!r} "
            f"vectorDim={len(vector)} error={body.get('error')}"
        )

    if len(got) < args.count:
        print("\n❌ 콜백이 모자란다. 요청 수 == MAX_CONCURRENT_STAGES 인데 하나도"
              " 안 왔다면 세마포어 재진입 데드락을 의심할 것"
              " (stages.py 의 _get_semaphore 주석 참고).")
        return 1
    print("\n✅ 전부 수신 — 데드락 없음")
    return 0


if __name__ == "__main__":
    sys.exit(main())
