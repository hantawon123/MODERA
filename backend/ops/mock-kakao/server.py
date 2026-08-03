import json
import re
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

APP_ID = 1525155


class Handler(BaseHTTPRequestHandler):
    def do_GET(self):
        token = self.headers.get("Authorization", "").removeprefix("Bearer ")
        match = re.fullmatch(r"modera-perf-kakao-(\d+)", token)
        if not match:
            self.respond(401, {"msg": "invalid mock token"})
            return
        user_number = int(match.group(1))
        if self.path == "/v1/user/access_token_info":
            self.respond(200, {"app_id": APP_ID})
        elif self.path == "/v2/user/me":
            self.respond(200, {
                "id": 9_000_000_000 + user_number,
                "kakao_account": {
                    "email": f"modera-perf-kakao-{user_number}@example.com",
                    "is_email_valid": True,
                    "is_email_verified": True,
                },
            })
        else:
            self.respond(404, {"msg": "not found"})

    def respond(self, status, body):
        encoded = json.dumps(body).encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(encoded)))
        self.end_headers()
        self.wfile.write(encoded)

    def log_message(self, *_):
        pass


ThreadingHTTPServer(("0.0.0.0", 8080), Handler).serve_forever()
