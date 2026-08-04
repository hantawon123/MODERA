# 카테고리 아이콘 (2026-08-04)

AGENT 가 새 카테고리를 만들면 그 이름으로 아이콘 1장을 생성해 MinIO 에 올린다.
앱은 카테고리 목록 카드에 이 아이콘을 띄운다.

기능 전체가 `app/category_icon.py` 한 파일에 있다. 되돌리려면 이 파일과
`main.py` 의 `include_router` 한 줄, 그리고 `schedule_icon` 호출 두 곳을 지운다.

## 1:1 매칭 — 매핑 테이블이 없다

오브젝트 key 가 곧 categoryId 다.

```
bucket: category-thumbnails
key:    {categoryId}.png          예) 1735462011.png
```

`categoryId` 는 카테고리 이름의 31비트 CRC32 해시(`search.stable_id`)다. Spring 도
Android 도 AI 도 같은 값을 계산하므로, 저장 구조만으로 카테고리 ↔ 아이콘이 1:1 이
된다. DB 행도, 동기화도, 고아 레코드도 없다.

- 이름 정규화(`normalize_name`) 기준이라 "쇼핑" 과 "쇼핑 " 은 같은 아이콘이다.
- 카테고리 이름이 바뀌면 id 가 바뀌어 새 아이콘이 생긴다(이름 변경 경로는 제품에 없다).
- 기본 13종끼리 key 충돌이 없는 것은 테스트가 지킨다(`test/test_category_icon.py`).

## 흐름

```
분석/재분석에서 resolution.created == True
  └ schedule_icon(categoryId, name)        백그라운드, 분석 응답을 붙잡지 않는다
      └ ensure_icon()                      head → 있으면 끝, 없으면 생성
          ├ OpenAI Images (gpt-image-1-mini, 1024x1024, quality=low, 투명 배경)
          ├ Pillow 로 216x216 PNG 로 축소 (알파 보존)
          └ MinIO category-thumbnails/{categoryId}.png
```

생성은 수십 초가 걸리므로 판정 경로를 막지 않는다. **실패해도 삼킨다** — 조회
엔드포인트가 없는 아이콘을 그 자리에서 만들어 채우므로 첫 조회가 재시도를 겸한다.
같은 카테고리를 동시에 두 번 만들지 않도록 진행 중 목록(`_pending`)으로 막는다
(한 배치에 같은 신규 카테고리가 여러 장 붙으면 생성 요금이 그만큼 곱해진다).

## 엔드포인트 (전송은 Spring 경유)

```
GET /api/v1/categories/{categoryId}/icon
→ 200 image/png (216x216), Cache-Control: public, max-age=604800
→ 404 CATEGORY_NOT_FOUND  이 사용자에게 그 categoryId 의 카테고리가 없다
→ 404 ICON_NOT_FOUND      생성까지 실패했다(사유는 detail·서버 로그)
```

이 응답만 공통 envelope 를 쓰지 않는다(바이너리). 썸네일 `/thumbnail/raw` 와 같은 모양.

presigned URL 로 주지 않는 이유는 두 가지다. (1) 만료가 있어 앱 이미지 캐시가 매번
깨진다. (2) 아직 만들어지지 않은 아이콘을 즉석 생성하는 경로가 이쪽에만 있다.

카테고리 목록(7-2) 응답에 주소가 함께 실린다.

```json
{ "categoryId": 1735462011, "name": "부동산",
  "iconUrl": "/api/v1/categories/1735462011/icon",
  "thumbnailUrl": "/api/v1/images/42/thumbnail/raw", "imageCount": 7 }
```

`thumbnailUrl` 은 그 카테고리에 속한 **최신 사진**이고, `iconUrl` 은 카테고리 자체를
나타내는 **생성 아이콘**이다. 서로 다른 것이라 필드를 나눠 둔다.

## 모델·엔드포인트

Gemini 경로와 완전히 분리한다. SSAFY GMS 프록시는 이미지 생성 모델을 중계하지
않으므로 OpenAI 공식 API 로 직접 붙는다. SDK 를 추가하지 않고 httpx 로 REST 를
친다 — 호출이 하나뿐이라 의존성을 늘릴 이유가 없다(requirements.txt 변경 없음).

| 환경변수 | 기본값 | 설명 |
|---|---|---|
| `OPENAI_API_KEY` | (없음) | 비우면 아이콘만 안 만들어진다. 기동·분석은 정상 |
| `OPENAI_BASE_URL` | `https://api.openai.com/v1` | |
| `IMAGE_MODEL_NAME` | `gpt-image-1-mini` | |
| `IMAGE_GEN_SIZE` | `1024x1024` | gpt-image-1 계열은 `1024x1024`/`1024x1536`/`1536x1024`/`auto` 만 받는다. 216 은 400 이라 받아서 줄인다. 정사각 유지 |
| `IMAGE_GEN_QUALITY` | `low` | 단순 픽토그램이라 올릴 이유가 없다 |
| `CATEGORY_ICON_SIZE` | `216` | 저장 규격 한 변(px) |
| `S3_CATEGORY_ICON_BUCKET` | `category-thumbnails` | |
| `OPENAI_TIMEOUT` | `120` | 생성이 수십 초 걸린다 |

`MOCK_AI=true` 면 OpenAI 를 부르지 않고 이름 해시로 만든 단색 PNG 를 쓴다 — 키 없이
배선(생성 → 축소 → 업로드 → 조회)을 끝까지 확인할 수 있다.

프롬프트는 상수 하나(`_PROMPT`)로 고정하고 카테고리 이름만 갈아 끼운다. 호출마다
문장이 흔들리면 카테고리별로 화풍이 달라져 목록 화면이 무너진다. 글자 금지를 명시한
이유는 생성 모델이 한국어를 자주 깨진 형태로 그려 넣기 때문이다.

## 비용 (2026-08-04 실측)

`gpt-image-1-mini`, `quality=low`, 1024x1024 = **$0.005/장**. 지금 OpenAI 최저가다
(dall-e-2/3 은 2026-05-12 API 에서 제거, GPT Image 2 low 는 $0.006). `quality` 아래
단계도 없고 `size` 하한도 1024 라 **장당 단가는 더 못 내린다.** 생성 시간 18.6초도
모델 고정이다.

그래서 절감 레버는 호출 횟수뿐이고, 셋 다 들어가 있다.

1. **전 사용자 공유** — key 에 `user_id` 가 없다. 사용자 100명이 각자 '부동산'을
   만들어도 아이콘은 1장이다. 총 생애 비용 = 시스템의 서로 다른 카테고리 이름 수
   × $0.005. 사용자 수와 무관하다.
2. **멱등 + 동시 중복 차단** — `ensure_icon` 은 head 부터 하고, `_pending` 이 같은
   카테고리의 동시 생성을 막는다. 한 배치에 같은 신규 카테고리가 10장 붙어도 생성 1회.
3. **기본 13종 사전 생성** — 아래 스크립트. 일회성 $0.065 로 런타임 생성과
   "첫 조회 18.6초" 노출을 대부분 없앤다.

## 로컬 / 배포 준비

```bash
python scripts/seed_minio.py                          # 버킷 생성(category-thumbnails 포함)
python scripts/seed_category_icons.py --dry-run       # 빠진 것·예상 요금만 확인
python scripts/seed_category_icons.py                 # 기본 13종 심기(멱등)
python test/test_category_icon.py
```

버킷이 없으면 업로드가 매번 실패해 신규 카테고리마다 생성 요금이 계속 나간다.
`seed_category_icons.py` 는 생성 **전에** `head_bucket` 으로 버킷에 닿아 보고,
못 닿으면 아무것도 만들지 않고 끝낸다(요금만 나가는 것을 막는다).

기동 훅이 아니라 스크립트인 이유는 장당 ~19초라 부팅이 13종 × 19초 매달리기 때문이다.

## 남은 것

- 사용자가 만든 롱테일 카테고리 중 이 기능 이전에 생긴 것은 첫 조회 때 생성된다.
  미리 채우려면 `seed_category_icons.py --name <이름>` 을 쓴다.
- 아이콘 재생성(맘에 안 들 때) 경로가 없다. 오브젝트를 지우면 다음 조회가 다시 만든다.
