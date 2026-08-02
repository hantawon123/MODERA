"""공용 시간 형식 헬퍼 — 단일 출처.

명세 1.2 시간 형식(ISO-8601 UTC, 밀리초 3자리)을 한 곳에서만 정의한다.
예전에는 같은 4줄이 deps·stages·document·responses·jobs 에 흩어져 있었다.
이 모듈은 아무것도 import 하지 않는 leaf 라 어디서든 순환 없이 가져다 쓴다.
"""

from datetime import datetime, timezone


def now_iso() -> str:
    """명세 1.2 시간 형식: ISO-8601 UTC, 밀리초 3자리."""
    return datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%S.%f")[:-3] + "Z"
