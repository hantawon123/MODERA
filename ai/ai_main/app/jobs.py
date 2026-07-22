"""jobId + stage 멱등 처리 (명세 10-1 DUPLICATE_JOB).

프로세스 메모리 기반이라 단일 인스턴스 전제다. 다중 인스턴스나 재시작 후에도
멱등을 보장하려면 Redis 등 외부 저장소로 옮겨야 한다.
"""

import threading
from collections import OrderedDict

MAX_ENTRIES = 10_000


class JobRegistry:
    def __init__(self, max_entries: int = MAX_ENTRIES) -> None:
        self._seen: OrderedDict[tuple[int, str], str] = OrderedDict()
        self._lock = threading.Lock()
        self._max_entries = max_entries

    def try_claim(self, job_id: int, stage: str) -> bool:
        """처음 보는 작업이면 True, 이미 처리 중·완료면 False."""
        key = (job_id, stage)
        with self._lock:
            if key in self._seen:
                return False
            self._seen[key] = "PROCESSING"
            while len(self._seen) > self._max_entries:
                self._seen.popitem(last=False)
            return True

    def mark(self, job_id: int, stage: str, status: str) -> None:
        with self._lock:
            self._seen[(job_id, stage)] = status

    def status(self, job_id: int, stage: str) -> str | None:
        with self._lock:
            return self._seen.get((job_id, stage))


job_registry = JobRegistry()
