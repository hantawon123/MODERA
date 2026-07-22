"""Gemini 호출 래퍼.

SDK 의존성을 이 파일에만 가둔다. google-genai(신 SDK)를 쓰며,
응답 JSON 은 response_schema 로 구조를 강제한다.
(프롬프트로만 JSON 을 요구하는 것보다 파싱 실패가 적다.)
"""

import json
import logging
from functools import lru_cache
from typing import Any

from google import genai
from google.genai import types

from .config import get_settings

logger = logging.getLogger(__name__)


class GeminiError(RuntimeError):
    pass


@lru_cache(maxsize=1)
def _client() -> genai.Client:
    return genai.Client(api_key=get_settings().gemini_api_key)


def parse_json_response(text: str) -> dict[str, Any]:
    """모델 응답에서 JSON 을 추출한다.

    response_schema 를 쓰면 보통 순수 JSON 이 오지만, 모델이 코드펜스나
    잡문을 섞는 경우가 남아 있어 복구 경로를 유지한다.
    """
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


def generate_json(
    model_name: str, parts: list[Any], schema: dict[str, Any] | None = None
) -> dict[str, Any]:
    """텍스트(또는 텍스트+이미지) 프롬프트를 보내고 JSON 응답을 받는다."""
    config = types.GenerateContentConfig(
        response_mime_type="application/json",
        response_schema=schema,
    )
    response = _client().models.generate_content(
        model=model_name, contents=parts, config=config
    )
    if response.text is None:
        raise GeminiError(f"{model_name} 이 빈 응답을 반환했습니다.")
    return parse_json_response(response.text)


def image_part(image_bytes: bytes, mime_type: str = "image/jpeg"):
    """이미지 바이트를 비전 입력 파트로 변환한다."""
    return types.Part.from_bytes(data=image_bytes, mime_type=mime_type)


def embed(texts: list[str], purpose: str = "DOCUMENT") -> tuple[str, list[list[float]]]:
    """텍스트 배치를 한 번의 호출로 임베딩한다. (모델명, 벡터 목록)을 돌려준다."""
    settings = get_settings()
    task_type = "RETRIEVAL_QUERY" if purpose == "QUERY" else "RETRIEVAL_DOCUMENT"
    # 빈 문자열은 API 가 거부하므로 공백으로 대체한다.
    contents = [t if t and t.strip() else " " for t in texts]
    response = _client().models.embed_content(
        model=settings.embedding_model_name,
        contents=contents,
        config=types.EmbedContentConfig(task_type=task_type),
    )
    return settings.embedding_model_name, [list(e.values) for e in response.embeddings]
