"""Gemini 호출 래퍼.

SDK 의존성을 이 파일에만 가둔다. 현재는 google-generativeai 를 쓰지만
지원 종료 예고가 있으므로, 이후 google-genai 로 옮길 때 이 파일만 고치면 된다.
"""

import io
import json
import logging
import random
import time
from typing import Any, Callable

from .config import get_settings

logger = logging.getLogger(__name__)


class GeminiError(RuntimeError):
    pass


def _is_rate_limit(exc: Exception) -> bool:
    """429(rate limit / quota) 성격의 예외인지 판별한다.

    SDK·전송 계층에 따라 예외 타입이 제각각이라, 상태코드·클래스명·메시지를
    폭넓게 본다. (google.api_core.exceptions.ResourceExhausted 등)
    """
    if getattr(exc, "status_code", None) == 429 or getattr(exc, "code", None) == 429:
        return True
    if type(exc).__name__ in ("ResourceExhausted", "TooManyRequests"):
        return True
    msg = str(exc).lower()
    return any(
        token in msg
        for token in ("429", "resource_exhausted", "rate limit", "rate-limit", "quota exceeded")
    )


def _call_with_retry(label: str, fn: Callable[[], Any]) -> Any:
    """Gemini 호출을 429에 한해 지수 백오프로 재시도한다.

    429가 아닌 예외(잘못된 요청·인증 오류 등)는 재시도 없이 즉시 올린다.
    동기 함수라 asyncio.to_thread 안에서 그대로 호출된다.
    """
    settings = get_settings()
    attempts = max(1, settings.gemini_max_attempts)
    for attempt in range(1, attempts + 1):
        try:
            return fn()
        except Exception as exc:
            if attempt >= attempts or not _is_rate_limit(exc):
                raise
            delay = min(
                settings.gemini_backoff_base * (2 ** (attempt - 1)),
                settings.gemini_backoff_max,
            )
            delay += random.uniform(0, settings.gemini_backoff_base)  # 지터로 동시 재시도 분산
            logger.warning(
                "Gemini rate limit(%s) 재시도 %s/%s, %.1fs 대기: %s",
                label, attempt, attempts, delay, str(exc)[:200],
            )
            time.sleep(delay)


def _genai():
    try:
        import google.generativeai as genai
    except ImportError as e:  # pragma: no cover
        raise GeminiError("google-generativeai 가 설치되지 않았습니다.") from e
    genai.configure(api_key=get_settings().gemini_api_key)
    return genai


def parse_json_response(text: str) -> dict[str, Any]:
    """모델 응답에서 JSON 을 추출한다. 코드펜스·잡문이 섞여도 복구를 시도한다."""
    cleaned = (text or "").strip()
    if cleaned.startswith("```"):
        parts = cleaned.split("```")
        cleaned = parts[1] if len(parts) > 1 else cleaned
        if cleaned.lower().startswith("json"):
            cleaned = cleaned[4:]
        cleaned = cleaned.strip()
    try:
        return json.loads(cleaned)
    except json.JSONDecodeError:
        start, end = cleaned.find("{"), cleaned.rfind("}")
        if start != -1 and end > start:
            return json.loads(cleaned[start : end + 1])
        raise GeminiError(f"모델 응답을 JSON 으로 해석하지 못했습니다: {cleaned[:200]!r}")


def generate_json(model_name: str, parts: list[Any]) -> dict[str, Any]:
    """텍스트(또는 텍스트+이미지) 프롬프트를 보내고 JSON 응답을 받는다."""
    genai = _genai()
    model = genai.GenerativeModel(model_name)
    response = _call_with_retry(
        f"generate_content:{model_name}", lambda: model.generate_content(parts)
    )
    return parse_json_response(response.text)


def image_part(image_bytes: bytes):
    """S3 에서 받은 바이트를 비전 입력으로 변환한다."""
    try:
        from PIL import Image
    except ImportError as e:  # pragma: no cover
        raise GeminiError("pillow 가 설치되지 않았습니다.") from e
    return Image.open(io.BytesIO(image_bytes))


def embed(texts: list[str], purpose: str = "DOCUMENT") -> tuple[str, list[list[float]]]:
    """텍스트 배치를 임베딩한다. (모델명, 벡터 목록)을 돌려준다."""
    settings = get_settings()
    genai = _genai()
    task_type = "retrieval_query" if purpose == "QUERY" else "retrieval_document"
    vectors: list[list[float]] = []
    for text in texts:
        response = _call_with_retry(
            "embed_content",
            lambda text=text: genai.embed_content(
                model=settings.embedding_model_name,
                content=text,
                task_type=task_type,
                # 차원을 명시해 모델 기본값(3072)이 아닌 합의된 값으로 받는다.
                output_dimensionality=settings.embedding_dim,
            ),
        )
        vector = list(response["embedding"])
        # 차원이 어긋난 벡터가 저장·색인까지 흘러가면 조용히 깨진다. 여기서 끊는다.
        if len(vector) != settings.embedding_dim:
            raise GeminiError(
                f"임베딩 차원이 설정과 다릅니다: 기대 {settings.embedding_dim}, "
                f"실제 {len(vector)} (모델={settings.embedding_model_name})"
            )
        vectors.append(vector)
    return settings.embedding_model_name, vectors
