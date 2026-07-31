# main.py 라우터 분리

> 동작 변경 0. `openapi.json` 이 분리 전과 byte 단위로 같다.
> 새 기능도, 삭제도, 로직 수정도 없다. 파일만 옮겼다.

## 왜

`app/main.py` 가 1321줄이었다. 라우트 20개, 공유 헬퍼 13개, 예외 핸들러 4개,
OpenAPI 후처리가 한 파일에 있어서 서로 다른 기능을 건드리는 브랜치가 전부
같은 파일에서 충돌했다. `related.py`·`doc_selection.py`·`reanalyze.py` 는 이미
기능별 파일로 나가 있었는데(`include_router` 한 줄만 main 에 닿는다) 나머지가
남아 있던 상태다.

## 결과

| 파일 | 줄 | 내용 |
| --- | --- | --- |
| `main.py` | 258 | lifespan, `FastAPI()` 생성, OpenAPI 422 후처리, 예외 핸들러 4개, `/health`, `include_router` 6줄 |
| `deps.py` | 198 | 라우터 공용 의존성·헬퍼. **라우트 없음** |
| `internal_api.py` | 221 | `/internal/v1/*` — 명세 10 장 5개 |
| `app_images.py` | 483 | `/api/v1/images/*` — 명세 4·6 장 9개 |
| `app_library.py` | 341 | `/api/v1/{analysis,tags,categories,home,search}` — 명세 5·7·8 장 6개 |

기존 3개(`related` 286 / `doc_selection` 109 / `reanalyze` 223)는 손대지 않았다.

### deps.py 에 있는 것

| 이름 | 역할 |
| --- | --- |
| `require_internal_token` | `X-Internal-Token` 검사. `APP_API_AUTH=false` 동안 `/api/v1/*` 우회 포함 |
| `CurrentUserId` / `resolve_user_id` / `_MissingUserId` | 사용자 식별. `FIXED_USER_ID` 고정, 0 이하 거부 |
| `_owned_image` | imageId 소유권 확인. 없는 것과 남의 것을 구분해 주지 않는다(둘 다 404) |
| `_thumbnail_url` / `_image_url` | 조회 URL. `PRESIGNED_READ_URLS` 로 서버 경유 ↔ presigned 선택 |
| `_to_list_item` / `_tag_refs` / `_category_refs` / `_resolve_filters` | 응답 조립 |
| `_now_iso` / `_error` / `_is_app_api` | 시간 형식, 내부 API 에러 형식, 앱 API 판별 |

**왜 별 파일인가**: 라우트 모듈이 `main.py` 의 헬퍼를 import 하면 순환이 된다
(main 이 그 모듈을 `include_router` 한다). 그래서 헬퍼 추출이 분리의 선행 조건이었다.

### main.py 에 남긴 것

예외 핸들러는 `@app.exception_handler` 데코레이터가 `app` 객체를 잡아야 해서 못
옮긴다. `_MissingUserId` 핸들러도 여기 남고 클래스만 `deps` 에서 가져온다.

`include_router` 순서는 **원래 라우트가 선언돼 있던 자리**를 지킨다. 위로 모으면
`openapi.json` 의 path 순서가 바뀐다.

## 이 리팩터링이 하지 않은 것

- 이름 개명 없음. `_owned_image` 처럼 밑줄 붙은 이름도 그대로 import 한다
  (밑줄은 `import *` 에만 영향). 개명하면 함수 본문에 diff 가 생긴다.
- 라우트별 `dependencies=[Depends(require_internal_token)]` 23개를
  `include_router(..., dependencies=[...])` 로 접지 않았다. 순수 이동이 아니고
  OpenAPI security 블록 위치가 흔들린다.
- 죽은 코드 정리 없음. 지역 `import asyncio` 14개, `except InvalidSortError: raise`
  2곳, 중복 `bool()` 1곳 — 원본 그대로 따라갔다.
- 이동으로 참조가 끊긴 `main.py` 의 import(schemas 대부분, `job_store`,
  `execute_stage`, `BackgroundTasks` 등)도 남겨 뒀다.

즉 새 파일의 모든 줄은 원본과 같고, 편집은 **`@app.` → `@router.` 20줄 +
import 목록 줄바꿈 2줄**이 전부다.

## 알려진 변경 1가지

라우터 2개로 묶으면서 앱 API 등록 순서가 바뀐다.

```
전: 4-1 4-2 4-5 4-3 | 5-1 5-6 | 6-1 6-2 6-6 raw source | 7-1 7-2 7-3 8-1
후: 4-1 4-2 4-5 4-3   6-1 6-2 6-6 raw source | 5-1 5-6   7-1 7-2 7-3 8-1
    └────────── app_images ──────────────┘   └───── app_library ─────┘
```

같은 URL 에 동시 매칭되는 (메서드, 경로) 짝이 24개 중 **0개**라 매칭 결과는
동일하다(아래 검증 ③). 바뀌는 건 Swagger 나열 순서뿐 — 장 번호 순이 아니게 된다.
장 번호 순을 유지하려면 라우터를 4개(4장/5장/6장/7·8장)로 쪼개야 한다.

## 검증

분리 전 `HEAD` 에서 기준 스냅샷을 떠 두고 커밋마다 대조했다.

```bash
cd ai/ai_main

# ① openapi.json 이 같은가 — 이게 주 검증이다
python3 -c "
import json
from app.main import app
print(json.dumps(app.openapi(), ensure_ascii=False, sort_keys=True, indent=1))
" > /tmp/openapi_after.json
diff /tmp/openapi_before.json /tmp/openapi_after.json     # 24 paths, 4660줄, 차이 0

# ② 사라진 줄이 있는가
git show <분리전>:ai/ai_main/app/main.py | grep -v '^[[:space:]]*$' | sort -u > /tmp/before_lines
cat app/main.py app/deps.py app/internal_api.py app/app_images.py app/app_library.py \
  | grep -v '^[[:space:]]*$' | sort -u > /tmp/after_lines
comm -23 /tmp/before_lines /tmp/after_lines   # @app. 20줄 + import 줄바꿈 2줄만
```

**`app.routes` 로는 검증하지 마라.** FastAPI 0.139 는 `include_router` 를
`_IncludedRouter` 로 지연 등록해서 평면 목록에 안 잡힌다 — 분리 전에도
`related`·`doc_selection`·`reanalyze` 는 `app.routes` 에 안 보였다.
`app.openapi()` 는 전부 펼친다.

- ③ 같은 URL 에 동시 매칭되는 경로 짝 0개 — 등록 순서 변화 무해
- ④ 옮긴 엔드포인트 13개 실제 호출 스모크 — 500·`NameError` 없음.
  지역 `import asyncio` 에 기대는 함수를 옮기면 import 시점엔 안 터지고
  요청이 들어올 때만 터진다. openapi diff 로는 안 잡히는 구멍이라 필요했다.
- ⑤ 기존 테스트 4파일(`test_related`·`test_doc_selection`·`test_reanalyze`·
  `test_document`) 전부 통과

## 커밋

| 커밋 | 내용 |
| --- | --- |
| `4526fd7` | 공유 헬퍼를 `deps.py` 로 분리 (순환 차단, 라우트는 아직 main) |
| `42ad6b1` | 내부 API 라우터를 `internal_api.py` 로 분리 |
| `8f8ae00` | 앱 API 라우터를 `app_images.py`·`app_library.py` 로 분리 |

## 다음에 할 수 있는 것 (안 했음)

1. `main.py` 의 끊긴 import 정리 (~50줄)
2. 지역 `import asyncio` 14개 제거 — `main.py` 7번줄에 top-level 이 이미 있었다
3. `except InvalidSortError: raise` 2곳 제거 — 잡아서 그대로 던진다
4. `IMAGE_NOT_FOUND` 응답 7곳 복붙 → 헬퍼 1개
5. 라우트별 `dependencies` 23개 → `include_router` 인자로 접기

1~3 은 순수 삭제라 위험이 없고, 4~5 는 로직에 손이 닿는다.
