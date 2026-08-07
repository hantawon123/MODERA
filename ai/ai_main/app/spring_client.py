"""Spring 내부 API 클라이언트.

- 10-4 : 분석 결과 콜백 (FastAPI → Spring)
- 10-5 : 사용자 지식 후보 조회 (FastAPI → Spring)
"""

import asyncio
import logging

import httpx

from .config import get_settings
from .schemas import CallbackRequest, KnowledgeCandidates

logger = logging.getLogger(__name__)

CALLBACK_PATH = "/internal/v1/callback/analysis"
CALLBACK_MAX_ATTEMPTS = 3


def _headers() -> dict[str, str]:
    return {
        "X-Internal-Token": get_settings().internal_token,
        "Content-Type": "application/json; charset=utf-8",
    }


async def post_callback(payload: CallbackRequest, callback_url: str | None = None) -> bool:
    """10-4 콜백 전송. 실패해도 예외를 올리지 않고 로그만 남긴다."""
    settings = get_settings()
    if not settings.spring_enabled and not callback_url:
        # Spring 미연동 모드. 보낼 곳이 없으므로 3회 재시도(백오프 포함)를 낭비하지 않는다.
        # 요청이 callbackUrl 을 명시했다면 그쪽으로는 그대로 보낸다.
        logger.info("Spring 미연동(SPRING_ENABLED=false) — 콜백 생략 jobId=%s stage=%s",
                    payload.job_id, payload.stage)
        return False
    url = callback_url or f"{settings.spring_base_url.rstrip('/')}{CALLBACK_PATH}"
    body = payload.model_dump(by_alias=True, exclude_none=True)

    for attempt in range(1, CALLBACK_MAX_ATTEMPTS + 1):
        try:
            async with httpx.AsyncClient(timeout=settings.spring_timeout) as client:
                response = await client.post(url, json=body, headers=_headers())
            if response.status_code < 300:
                logger.info(
                    "콜백 성공 jobId=%s stage=%s status=%s",
                    payload.job_id, payload.stage, payload.status,
                )
                return True
            logger.warning(
                "콜백 실패(HTTP %s) jobId=%s stage=%s attempt=%s",
                response.status_code, payload.job_id, payload.stage, attempt,
            )
        except Exception as e:
            logger.warning(
                "콜백 예외 jobId=%s stage=%s attempt=%s: %s",
                payload.job_id, payload.stage, attempt, e,
            )
        if attempt < CALLBACK_MAX_ATTEMPTS:
            await asyncio.sleep(2 ** (attempt - 1))

    logger.error("콜백 최종 실패 jobId=%s stage=%s", payload.job_id, payload.stage)
    return False


async def fetch_knowledge_candidates(
    user_id: int, tag_limit: int = 100, category_limit: int = 50
) -> KnowledgeCandidates:
    """10-5 기존 태그·카테고리 후보 조회.

    조회에 실패하면 빈 후보로 degrade 한다(분석 자체를 실패시키지 않는다).
    Spring 미연동 모드에서는 아예 시도하지 않는다(건당 약 4초 절약).
    """
    settings = get_settings()
    if not settings.spring_enabled:
        return KnowledgeCandidates(user_id=user_id)
    url = (
        f"{settings.spring_base_url.rstrip('/')}"
        f"/internal/v1/user/{user_id}/knowledge-candidates"
    )
    params = {"tagLimit": tag_limit, "categoryLimit": category_limit}
    try:
        async with httpx.AsyncClient(timeout=settings.spring_timeout) as client:
            response = await client.get(url, params=params, headers=_headers())
        response.raise_for_status()
        return KnowledgeCandidates.model_validate(response.json())
    except Exception as e:
        logger.warning("지식 후보 조회 실패 userId=%s: %s — 빈 후보로 진행", user_id, e)
        return KnowledgeCandidates(user_id=user_id)
