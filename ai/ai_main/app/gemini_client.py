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


def _mock_json(parts: list[Any]) -> dict[str, Any]:
    """MOCK_AI=true 일 때 쓰는 가짜 응답.

    프롬프트가 요구하는 JSON 형태를 보고 어느 호출인지 구분한다. 각 프롬프트가
    출력 스키마를 문자열로 박아 두고 있어(`'{"informative": ...}'`) 그 키를 찾으면 된다.
    """
    prompt = " ".join(p for p in parts if isinstance(p, str))
    if '"informative"' in prompt:
        return {"informative": True, "confidence": 0.9, "reason": "MOCK 정보성 판정"}
    if '"detected_texts"' in prompt:
        return {
            "description": "MOCK 이미지 분석 결과입니다.",
            "detected_texts": ["MOCK 텍스트", "32,000원", "2026-07-28"],
            "objects": ["텍스트", "버튼"],
        }
    if '"key_information"' in prompt:
        return {
            "title": "MOCK 제목",
            "summary": "MOCK 요약입니다.",
            "tags": ["mock", "테스트"],
            "categories": ["쇼핑"],
            "key_information": ["항목: MOCK 값"],
            "analysis_confidence": 0.9,
        }
    if '"price_min"' in prompt:
        return {"keywords": ["mock"], "price_min": None, "price_max": None,
                "brand": None, "category_hints": [], "date_from": None,
                "date_to": None, "expires_before": None, "confidence": 0.9}
    return {}


def _mock_vector(text: str, dim: int) -> list[float]:
    """텍스트 해시로 만드는 결정적 단위 벡터. 같은 입력이면 항상 같은 값이 나온다."""
    import hashlib
    import math

    digest = hashlib.sha256(text.encode("utf-8")).digest()
    seed = int.from_bytes(digest[:8], "big")
    # 선형 합동 생성기. random 모듈의 전역 상태를 건드리지 않으려고 직접 돌린다.
    values: list[float] = []
    state = seed or 1
    for _ in range(dim):
        state = (state * 6364136223846793005 + 1442695040888963407) % (2**64)
        values.append((state / 2**64) * 2.0 - 1.0)
    norm = math.sqrt(sum(v * v for v in values)) or 1.0
    return [v / norm for v in values]


def generate_json(model_name: str, parts: list[Any]) -> dict[str, Any]:
    """텍스트(또는 텍스트+이미지) 프롬프트를 보내고 JSON 응답을 받는다."""
    if get_settings().mock_ai:
        logger.info("MOCK_AI — generate_json 가짜 응답 (model=%s)", model_name)
        return _mock_json(parts)
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
    if settings.mock_ai:
        logger.info("MOCK_AI — embed 가짜 벡터 %s건 (dim=%s)",
                    len(texts), settings.embedding_dim)
        return settings.embedding_model_name, [
            _mock_vector(t, settings.embedding_dim) for t in texts
        ]
    if not texts:
        return settings.embedding_model_name, []
    genai = _genai()
    task_type = "retrieval_query" if purpose == "QUERY" else "retrieval_document"
    # content 에 리스트를 주면 SDK 가 batch_embed_contents 로 한 번에 보낸다
    # (100건 초과는 SDK 가 알아서 쪼갠다). 텍스트당 1회 순차 호출이던 것이 배치가
    # 되어, 카테고리 이름 17종 콜드 스타트가 17 RTT 에서 1 RTT 로 줄어든다.
    response = _call_with_retry(
        "embed_content",
        lambda: genai.embed_content(
            model=settings.embedding_model_name,
            content=texts,
            task_type=task_type,
            # 차원을 명시해 모델 기본값(3072)이 아닌 합의된 값으로 받는다.
            output_dimensionality=settings.embedding_dim,
        ),
    )
    vectors = [list(v) for v in response["embedding"]]
    # 호출자는 입력 순서와 벡터 순서가 같다고 보고 zip 한다
    # (stages.seed_default_category_vectors, 10-2 /internal/v1/embed 의 index).
    # 개수가 어긋나면 이름과 벡터가 밀려 붙으므로 여기서 끊는다.
    if len(vectors) != len(texts):
        raise GeminiError(
            f"임베딩 개수가 입력과 다릅니다: 요청 {len(texts)}, 응답 {len(vectors)}"
        )
    # 차원이 어긋난 벡터가 저장·색인까지 흘러가면 조용히 깨진다. 여기서 끊는다.
    for vector in vectors:
        if len(vector) != settings.embedding_dim:
            raise GeminiError(
                f"임베딩 차원이 설정과 다릅니다: 기대 {settings.embedding_dim}, "
                f"실제 {len(vector)} (모델={settings.embedding_model_name})"
            )
    return settings.embedding_model_name, vectors
