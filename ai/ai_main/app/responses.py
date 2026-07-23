"""외부(앱) API 공통 응답 형식.

팀 API 명세 1.1 의 envelope 를 그대로 따른다.

    { "code": "SUCCESS", "message": "...", "data": {...}, "timestamp": "..." }

내부 API(`/internal/v1/*`)는 서비스 간 통신 전용이라 이 형식을 쓰지 않는다(raw JSON).
"""

from datetime import datetime, timezone
from typing import Any

from fastapi.responses import JSONResponse


def _now() -> str:
    return datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%S.%f")[:-3] + "Z"


def success(
    data: Any, message: str = "요청이 성공했습니다.", http_status: int = 200
) -> JSONResponse:
    return JSONResponse(
        status_code=http_status,
        content={"code": "SUCCESS", "message": message, "data": data, "timestamp": _now()},
    )


def failure(
    code: str, message: str, data: Any = None, http_status: int = 400
) -> JSONResponse:
    """에러 응답. data 에는 상세 문자열 또는 필드 오류 배열을 담는다."""
    return JSONResponse(
        status_code=http_status,
        content={"code": code, "message": message, "data": data, "timestamp": _now()},
    )


def page_data(
    items: list[Any], page: int, size: int, total_elements: int
) -> dict[str, Any]:
    """페이지 응답의 data 부분을 명세 형식으로 만든다."""
    total_pages = (total_elements + size - 1) // size if size > 0 else 0
    return {
        "list": items,
        "page": page,
        "size": size,
        "totalElements": total_elements,
        "totalPages": total_pages,
        "first": page <= 0,
        "last": page >= total_pages - 1,
        "hasNext": page < total_pages - 1,
        "hasPrevious": page > 0,
    }
