"""카테고리 아이콘 — 기능 전부가 이 파일에 있다.

    ① AGENT 가 새 카테고리를 만든다(`resolution.created == True`)
    ② 그 이름으로 아이콘 1장을 생성한다 (OpenAI Images, gpt-image-1-mini)
    ③ 216x216 PNG 로 줄여 MinIO `category-thumbnails` 버킷에 올린다
    ④ 앱은 `GET /api/v1/categories/{categoryId}/icon` 으로 받는다(전송은 Spring 경유)

**매핑 테이블이 없다.** 오브젝트 key 가 곧 categoryId 다 — `{categoryId}.png`.
categoryId 는 이름에서 파생된 31비트 해시(`search.stable_id`)라 어느 쪽에서 계산해도
같은 값이 나오고, 그래서 카테고리 ↔ 아이콘이 저장 구조만으로 1:1 이 된다. DB 행도,
동기화도, 고아 레코드도 생기지 않는다. 카테고리 이름이 바뀌면 id 가 바뀌므로 새
아이콘이 생긴다(이름 변경 경로는 제품에 없다).

Gemini 와 다른 엔드포인트를 쓴다. SSAFY GMS 프록시는 이미지 생성 모델을 중계하지
않으므로 OpenAI 공식 API 로 직접 붙는다(`OPENAI_BASE_URL`). SDK 를 추가하지 않고
httpx 로 REST 를 직접 친다 — 호출이 하나뿐이라 의존성을 늘릴 이유가 없다.

**분석을 막지 않는다.** 생성은 수십 초가 걸리므로 신규 판정 시점에는 백그라운드로
띄우기만 하고(`schedule_icon`), 실패하면 로그만 남긴다. 조회 경로가 없는 아이콘을
그 자리에서 만들어 채우므로(`ensure_icon`), 생성이 한 번 실패해도 영구 결손이
되지 않는다 — 첫 조회가 재시도를 겸한다.

main.py 는 `include_router` 한 줄만 닿는다. 되돌리려면 이 파일과 그 한 줄,
그리고 두 곳의 `schedule_icon` 호출만 지운다.
"""

import asyncio
import base64
import io
import logging

import httpx
from fastapi import APIRouter, Depends
from fastapi.responses import Response

from . import responses, search, storage
from .config import get_settings
from .deps import require_internal_token

logger = logging.getLogger(__name__)

router = APIRouter()


class CategoryIconError(RuntimeError):
    pass


def icon_key(category_id: int) -> str:
    """카테고리 ↔ 아이콘 1:1 매핑의 전부. key 가 곧 categoryId 다."""
    return f"{category_id}.png"


# 아이콘 한 벌이 같은 그림체로 보여야 목록 화면이 무너지지 않는다. 스타일 문장을
# 상수로 고정하고 카테고리 이름만 갈아 끼운다 — 프롬프트가 호출마다 흔들리면
# 카테고리마다 화풍이 달라진다. 글자 금지를 명시하는 이유: 생성 모델은 한국어
# 글자를 자주 깨진 형태로 그려 넣는다.
_PROMPT = (
    "A single centered 3D rendered miniature icon representing the concept '{name}' for a app category. "
    "Handcrafted felt and fabric toy texture with visible stitching details, "
    "clay-like soft volume, warm pastel tones, matte finish. "
    "Clean studio lighting, soft subtle shadows, isolated on a solid white background. "
    "No text, no letters, no words, simple isolated object, high quality."
)



def _mock_png(name: str) -> bytes:
    """MOCK_AI 용 가짜 아이콘. 이름에서 색을 뽑아 카테고리마다 다르게 보이게 한다.

    OpenAI 키 없이도 배선(생성 → 리사이즈 → 업로드 → 조회)을 끝까지 확인할 수 있다.

    일부러 목표 규격(216)이 아니라 512 로 만든다 — 216 으로 만들면 `_to_icon` 의
    축소 경로가 테스트에서 한 번도 안 돌아간다(test/test_category_icon.py).
    """
    from PIL import Image

    tint = search.stable_id(name)
    color = (tint & 0xFF, (tint >> 8) & 0xFF, (tint >> 16) & 0xFF, 255)
    buffer = io.BytesIO()
    Image.new("RGBA", (512, 512), color).save(buffer, format="PNG")
    return buffer.getvalue()


def _generate_png(name: str) -> bytes:
    """OpenAI Images 로 아이콘 원본을 만든다. 실패는 CategoryIconError 로 올린다."""
    settings = get_settings()
    if settings.mock_ai:
        logger.info("MOCK_AI — 카테고리 아이콘 가짜 생성 name=%s", name)
        return _mock_png(name)
    if not settings.openai_api_key:
        raise CategoryIconError("환경변수 OPENAI_API_KEY 가 설정되지 않았습니다.")
    try:
        response = httpx.post(
            f"{settings.openai_base_url.rstrip('/')}/images/generations",
            headers={"Authorization": f"Bearer {settings.openai_api_key}"},
            json={
                "model": settings.image_model_name,
                "prompt": _PROMPT.format(name=name),
                "size": settings.image_gen_size,
                "quality": settings.image_gen_quality,
                "background": "transparent",
                "output_format": "png",
                "n": 1,
            },
            timeout=settings.openai_timeout,
        )
        response.raise_for_status()
    except httpx.HTTPStatusError as e:
        # 본문에 거절 사유가 들어 있다(모델명 오타·미지원 파라미터·정책 거절).
        # raise_for_status 메시지만 남기면 무엇이 틀렸는지 알 수 없다.
        raise CategoryIconError(
            f"이미지 생성 HTTP {e.response.status_code}: {e.response.text[:300]}") from e
    except Exception as e:
        raise CategoryIconError(
            f"이미지 생성 실패: {e.__class__.__name__}: {str(e)[:200]}") from e

    data = response.json().get("data") or []
    encoded = data[0].get("b64_json") if data else None
    if not encoded:
        # gpt-image-* 는 항상 b64_json 으로 준다(url 응답 없음). 비었으면 계약이 바뀐 것.
        raise CategoryIconError("이미지 생성 응답에 b64_json 이 없습니다.")
    return base64.b64decode(encoded)


def _to_icon(png: bytes) -> bytes:
    """생성본을 아이콘 규격(CATEGORY_ICON_SIZE 정사각 PNG)으로 줄인다.

    RGBA 로 열어 알파를 보존한다 — 앱이 라이트/다크 테마 어디에 올려도 배경이
    사각형으로 남지 않는다. ImageOps.fit 이라 비정사각 생성본도 가운데를 채운다.
    """
    from PIL import Image, ImageOps

    side = get_settings().category_icon_size
    with Image.open(io.BytesIO(png)) as image:
        image = image.convert("RGBA")
        if image.size != (side, side):
            image = ImageOps.fit(image, (side, side), Image.LANCZOS)
        buffer = io.BytesIO()
        image.save(buffer, format="PNG")
        return buffer.getvalue()


def ensure_icon(category_id: int, name: str) -> bytes:
    """아이콘을 확보한다 — 있으면 읽고, 없으면 만들어 올린다. 멱등.

    동기 함수다(boto3·httpx 둘 다 동기). 호출자가 asyncio.to_thread 로 감싼다.
    """
    key = icon_key(category_id)
    bucket = get_settings().s3_category_icon_bucket
    if storage.object_exists(key, bucket=bucket):
        return storage.fetch_category_icon(key)

    icon = _to_icon(_generate_png(name))
    storage.put_category_icon(key, icon)
    logger.info("카테고리 아이콘 생성 categoryId=%s name=%s (%s bytes)",
                category_id, name, len(icon))
    return icon


# 진행 중인 생성. 같은 카테고리를 동시에 두 번 만들지 않기 위한 것 —
# 한 배치에서 같은 신규 카테고리가 여러 장에 붙으면 그만큼 생성 요금이 곱해진다.
# 이벤트 루프 하나 안에서만 만지므로 락이 필요 없다.
_pending: dict[int, asyncio.Task] = {}


def schedule_icon(category_id: int, name: str) -> None:
    """신규 카테고리 아이콘을 백그라운드로 만들어 둔다(best-effort).

    분석 응답을 붙잡지 않는다 — 생성은 수십 초라 콜백이 그만큼 늦어지면 앱이
    분석 완료를 못 받는다. 실패해도 삼킨다: 첫 조회(`GET .../icon`)가 없는
    아이콘을 그 자리에서 만들므로 재시도 경로가 이미 있다.
    """
    if category_id in _pending:
        return

    async def _run() -> None:
        try:
            await asyncio.to_thread(ensure_icon, category_id, name)
        except Exception as e:
            logger.warning("카테고리 아이콘 사전 생성 실패 categoryId=%s name=%s: %s "
                           "(조회 시 다시 시도한다)", category_id, name, e)

    task = asyncio.create_task(_run())
    # 참조를 들고 있어야 한다 — asyncio 는 태스크를 약한 참조로만 잡아서
    # 로컬 변수만 두면 실행 도중에 GC 될 수 있다.
    _pending[category_id] = task
    task.add_done_callback(lambda _: _pending.pop(category_id, None))


async def wait_pending(category_id: int | None, timeout: float) -> None:
    """이 카테고리 아이콘이 올라갈 때까지만 기다린다(상한 timeout 초).

    분석 완료 콜백(10-4)이 앱에게는 '카테고리 생겼다' 신호다. 앱은 그 신호를 받고
    Spring 이 준 presigned URL 로 MinIO 를 **직접** 친다 — 아직 객체가 없으면
    MinIO 가 404 를 주고 끝이다(이 파일의 조회 엔드포인트를 안 탄다. 즉석 생성
    경로가 회선에 없다). 그래서 콜백 자체를 아이콘만큼 늦춘다.

    실측(2026-08-08 23:32): 콜백 23:32:36 → 앱이 23:32:39/48/52 세 번 404 →
    아이콘 완성 23:32:55. 20초 차이로 빈 자리가 남았다.

    기존 카테고리는 `_pending` 에 없어 즉시 통과한다(대부분의 분석). 생성이
    실패했거나 이미 끝났어도 done 콜백이 항목을 지웠으므로 역시 즉시 통과.
    상한을 넘겨도 예외 없이 그냥 돌아온다 — 콜백을 막을 만큼 중요하지 않다.

    ponytail: 호출자가 세마포어 슬롯을 쥔 채로 기다린다(MAX_CONCURRENT_STAGES=4).
    신규 카테고리가 한꺼번에 몰리면 그만큼 큐가 밀린다. 실제로 밀리면 대기를
    세마포어 밖으로 빼거나 슬롯 수를 올린다.
    """
    task = _pending.get(category_id) if category_id is not None else None
    if task is None or timeout <= 0:
        return
    await asyncio.wait([task], timeout=timeout)


@router.get("/api/v1/categories/{category_id}/icon",
            dependencies=[Depends(require_internal_token)],
            response_class=Response,
            responses={200: {"content": {"image/png": {}},
                             "description": "카테고리 아이콘 PNG (216x216)"},
                       404: {"description": "CATEGORY_NOT_FOUND / ICON_NOT_FOUND"}})
async def category_icon(category_id: int):
    """카테고리 아이콘 바이너리. Spring 이 이 주소를 앱에 중계한다.
    presigned URL 로 주지 않는다. (1) 만료가 있어 앱 캐시가 매번 깨지고,
    (2) 아직 만들어지지 않은 아이콘을 즉석 생성하는 경로가 이쪽에만 있다
    (썸네일 `/thumbnail/raw` 와 같은 이유·같은 모양).

    이 응답만 공통 envelope 를 쓰지 않는다(바이너리).
    """
    try:
        return Response(
            content=await asyncio.to_thread(storage.fetch_category_icon,
                                            icon_key(category_id)),
            media_type="image/png",
            headers={"Cache-Control": "public, max-age=604800"},
        )
    except Exception as e:
        logger.info("카테고리 아이콘 없음 categoryId=%s: %s — 즉석 생성", category_id, e)

    # 생성하려면 이름이 있어야 한다. id 는 이름 해시라 역산이 안 되므로 색인의
    # 카테고리 이름들을 훑어 같은 해시를 찾는다(사진이 1장이라도 있으면 잡힌다).
    # 사용자 필터를 걸지 않는다 — 아이콘은 전 사용자 공유라 '누구의 카테고리인지'
    # 가 의미가 없고, 여기서 필터를 걸면 userId 를 다시 받아야 한다.
    try:
        name = await asyncio.to_thread(
            search.resolve_name_by_id, None, "category_name", category_id)
    except Exception as e:
        logger.exception("카테고리 조회 실패 categoryId=%s", category_id)
        return responses.failure("INTERNAL_ERROR", "카테고리를 조회하지 못했습니다.",
                                 str(e)[:300], http_status=500)

    if name is None:
        return responses.failure("CATEGORY_NOT_FOUND", "카테고리를 찾을 수 없습니다.",
                                 f"categoryId: {category_id}", http_status=404)

    try:
        icon = await asyncio.to_thread(ensure_icon, category_id, name)
    except Exception as e:
        logger.warning("카테고리 아이콘 생성 실패 categoryId=%s name=%s: %s",
                       category_id, name, e)
        return responses.failure("ICON_NOT_FOUND", "아이콘을 가져오지 못했습니다.",
                                 str(e)[:200], http_status=404)

    return Response(content=icon, media_type="image/png",
                    headers={"Cache-Control": "public, max-age=604800"})
