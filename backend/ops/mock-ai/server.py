import json
import os
import threading
import time
import urllib.request
from datetime import datetime, timezone
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

DELAY_MS = int(os.getenv("MOCK_AI_DELAY_MS", "10"))
INTERNAL_TOKEN = os.getenv("INTERNAL_TOKEN", "")


def callback(body):
    time.sleep(DELAY_MS / 1000)
    payload = {
        "jobId": body["jobId"],
        "imageId": body["imageId"],
        "stage": body.get("stage", "FULL"),
        "status": "COMPLETED",
        "result": {
            "title": "Performance test image",
            "summary": "Deterministic mock result without an AI model call.",
            "ocrRefinedText": "performance test",
            "informative": True,
            "category": "기타",
            "tags": ["performance-test"],
            "keyInformation": [],
            "analysisConfidence": 1.0,
        },
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

    def do_POST(self):
        if self.path != "/internal/v1/analyze":
            self.send_error(404)
            return
        body = json.loads(self.read_body())
        threading.Thread(target=callback, args=(body,), daemon=True).start()
        response = {
            "jobId": body["jobId"], "imageId": body["imageId"],
            "stage": body.get("stage", "FULL"), "accepted": True,
            "status": "ACCEPTED", "error": None,
        }
        encoded = json.dumps(response).encode()
        self.send_response(202)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(encoded)))
        self.end_headers()
        self.wfile.write(encoded)

    def log_message(self, fmt, *args):
        return


ThreadingHTTPServer(("0.0.0.0", 8000), Handler).serve_forever()
