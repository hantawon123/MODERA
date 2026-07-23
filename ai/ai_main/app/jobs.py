"""작업 상태 관리.

- JobRegistry : jobId + stage 멱등 처리 (명세 10-1 DUPLICATE_JOB). Spring 연동용.
- JobStore    : 앱 직결 흐름에서 분석 결과를 담아두고 폴링으로 돌려주는 저장소.

둘 다 프로세스 메모리 기반이라 단일 인스턴스 전제다. 재시작하면 진행 중이던 작업과
저장된 결과가 사라지므로, 다중 인스턴스나 영속성이 필요해지면 Redis 로 옮겨야 한다.
"""

import logging
import threading
import time
from collections import OrderedDict
from datetime import datetime, timezone
from typing import Any

logger = logging.getLogger(__name__)

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


class JobStore:
    """앱 직결 분석 작업의 상태·결과를 보관한다.

    jobId 와 imageId 는 서버가 채번한다(앱은 값을 만들지 않는다).
    오래된 항목은 개수 상한에 걸리면 먼저 들어온 순서대로 밀려난다.
    """

    def __init__(self, max_entries: int = MAX_ENTRIES) -> None:
        self._jobs: OrderedDict[int, dict[str, Any]] = OrderedDict()
        self._lock = threading.Lock()
        self._max_entries = max_entries
        self._next_job_id = 1
        self._next_image_id = 1
        self._seeded = False

    @staticmethod
    def _now() -> str:
        return datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%S.%f")[:-3] + "Z"

    def _seed(self) -> None:
        """채번 시작점을 정한다(최초 1회). 반드시 락 안에서 호출한다.

        imageId 는 검색 인덱스의 문서 id 로도 쓰이므로, 서버가 재시작했을 때
        1번부터 다시 발급하면 기존 문서를 덮어써 데이터가 소리 없이 사라진다.
        그래서 이미 색인된 최대 imageId 다음부터 이어서 발급한다.

        색인을 확인할 수 없으면(OpenSearch 미기동 등) 현재 epoch 초를 시작점으로
        삼는다. 값은 커지지만 단조 증가라 최소한 덮어쓰기는 일어나지 않는다.
        """
        if self._seeded:
            return
        try:
            from . import search
            highest = search.max_image_id()
            logger.info("채번 시작점: 색인 최대 imageId=%s", highest)
        except Exception as e:
            highest = int(time.time())
            logger.warning(
                "색인 최대 imageId 조회 실패(%s) — 덮어쓰기 방지를 위해 %s 부터 채번",
                e, highest + 1,
            )
        self._next_image_id = highest + 1
        self._next_job_id = highest + 1
        self._seeded = True

    def create(
        self, user_id: int, s3_key: str, image_id: int | None = None
    ) -> dict[str, Any]:
        """새 작업을 만든다.

        imageId 는 Spring 이 발급한 값을 그대로 쓰는 것이 원칙이라 인자로 받는다.
        전달되지 않으면(Spring 없이 단독 테스트 등) 서버가 채번한다.
        """
        with self._lock:
            self._seed()
            if image_id is None:
                image_id = self._next_image_id
                self._next_image_id += 1
            job = {
                "job_id": self._next_job_id,
                "image_id": image_id,
                "user_id": user_id,
                "s3_key": s3_key,
                # 내부적으로는 LLM → IMAGE_ANALYSIS → AGENT → INDEXING 순으로 진행하며,
                # 현재 어느 단계인지 담아 분석 현황 화면에 그대로 노출한다.
                "stage": "LLM",
                "status": "QUEUED",
                "result": None,
                "error": None,
                "created_at": self._now(),
                "updated_at": self._now(),
            }
            self._next_job_id += 1
            self._jobs[job["job_id"]] = job
            while len(self._jobs) > self._max_entries:
                self._jobs.popitem(last=False)
            return dict(job)

    def next_image_id(self) -> int:
        """imageId 만 하나 발급한다(업로드 시점. 분석 작업은 아직 만들지 않는다).

        업로드와 분석 요청이 분리돼 있어서(앱이 온디바이스 OCR 을 끝낸 뒤 분석을 부른다)
        업로드 단계에서는 번호만 떼어 주고, 작업은 analyze 에서 만든다.
        """
        with self._lock:
            self._seed()
            image_id = self._next_image_id
            self._next_image_id += 1
            return image_id

    def update(
        self,
        job_id: int,
        status: str,
        result: dict[str, Any] | None = None,
        error: dict[str, Any] | None = None,
        stage: str | None = None,
    ) -> None:
        with self._lock:
            job = self._jobs.get(job_id)
            if job is None:
                return
            job["status"] = status
            job["updated_at"] = self._now()
            if stage is not None:
                job["stage"] = stage
            if result is not None:
                job["result"] = result
            if error is not None:
                job["error"] = error

    def get(self, job_id: int) -> dict[str, Any] | None:
        with self._lock:
            job = self._jobs.get(job_id)
            return dict(job) if job else None

    def list_by_user(self, user_id: int) -> list[dict[str, Any]]:
        """해당 사용자의 작업을 생성 순서대로 돌려준다."""
        with self._lock:
            return [dict(j) for j in self._jobs.values() if j["user_id"] == user_id]


job_store = JobStore()
