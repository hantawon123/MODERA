# 문서화 API 명세

> 명세 번호는 **제안**이다. 10장 계열에 편입할지 팀 확정이 필요하다.

## 10-6 복수 이미지 문서화

| 항목 | 내용 |
| --- | --- |
| API | `POST /internal/v1/documents` |
| 방향 | Spring → FastAPI |
| 처리 | 동기 |
| 설명 | 분석이 끝난 이미지 **여러 장의 결과를 종합해 문서 1개**를 만든다. 이미지 한 장당 한 번 호출하는 API가 아니다 — 전부 한 프롬프트에 들어가야 비교·병합·시간순 서술이 된다. 새로 분석하지 않으며, FastAPI는 자체 색인·저장소를 **조회하지 않는다**(재료를 전부 요청 본문으로 받는다). 소유자·분석 완료 필터링은 Spring 책임이다. |

### Request Header

| 이름 | 설명 |
| --- | --- |
| X-Internal-Token | 필수. 서비스 간 공유 토큰 |
| Content-Type | 필수. `application/json` |

### Request Body

```json
{
  "userId": 1,
  "images": [
    {
      "imageId": 101,
      "title": "교보문고 C++ 프로그래밍 입문",
      "summary": "C++ 입문서 32,000원, 10% 할인 중",
      "tags": ["도서", "프로그래밍"],
      "category": "쇼핑",
      "keyInformation": ["가격: 32,000원", "할인: 10%", "판매처: 교보문고"],
      "ocr": {
        "rawText": "교보문고 C++ 프로그래밍 입문 32,000원 10% 할인",
        "refinedText": null
      },
      "createdAt": "2026-07-16T06:00:00.000Z"
    },
    {
      "imageId": 102,
      "title": "예스24 C++ 프로그래밍 입문",
      "summary": "같은 책 28,800원, 무료배송",
      "tags": ["도서", "프로그래밍"],
      "category": "쇼핑",
      "keyInformation": ["가격: 28,800원", "배송: 무료", "판매처: 예스24"],
      "ocr": { "rawText": "예스24 C++ 프로그래밍 입문 28,800원 무료배송" },
      "createdAt": "2026-07-16T06:02:00.000Z"
    }
  ],
  "title": null,
  "instruction": "구매 후보를 가격순으로 비교해 줘",
  "language": null
}
```

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| userId | Number | 필수. **로그·추적 전용.** 이 값으로 필터링하지 않는다 |
| images | Array | 필수. 문서로 묶을 분석 결과. **1 ~ 30건** |
| title | String | 선택. 문서 제목 고정. 없으면 모델이 정한다 |
| instruction | String | 선택. 추가 지시. 예: `"여행 일정표로 정리해 줘"` |
| language | String | 선택. 출력 언어. 예: `"ko"` |

**images[]**

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| imageId | Number | 필수. 섹션 출처·출처 표에 쓰인다 |
| title | String | 선택. 기본 `""`. 섹션 구성, 출처 표 |
| summary | String | 선택. 기본 `""`. 섹션 구성 |
| tags | String[] | 선택. 기본 `[]`. **이름 배열**이다(`{tagId, name}` 객체 아님) |
| category | String | 선택. 카테고리 **이름**. 섹션 분리, 출처 표 |
| keyInformation | String[] | 선택. 기본 `[]`. 가격·날짜 등 구체 사실. 10-4로 보낸 그 필드 |
| ocr | Object | 선택. 기본 `{"rawText": ""}`. 문서의 사실 근거 |
| createdAt | String | 선택. ISO-8601. 시간순 서술, 출처 표 |

**images[].ocr**

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| rawText | String | 선택. 기본 `""`. OCR 원문 |
| refinedText | String | 선택. 있으면 `rawText` 대신 쓴다(분석 단계와 같은 규칙) |
| confidence | Number | 선택. **미사용.** 스키마 재사용으로 받기만 한다 |

> `lang` 은 계약에서 삭제됐다(2026-07-29 백엔드 합의). 보내도 무시되며 요청은 거부되지 않는다.

> 필수는 `userId` / `images` / `images[].imageId` **셋뿐**이다. 나머지는 기본값이 있어
> 빠져도 요청이 거부되지 않지만, 그만큼 문서가 부실해진다.
> 정의되지 않은 필드는 무시되므로 Spring DTO를 그대로 실어 보내도 깨지지 않는다.

### Response

```json
{
  "title": "C++ 입문서 구매 후보 비교",
  "summary": "같은 책을 두 곳에서 확인했다. 최저가는 예스24 28,800원이다.",
  "sections": [
    {
      "heading": "가격 비교",
      "body": "두 판매처 모두 동일한 도서이며 가격 차이는 3,200원이다.",
      "bullets": ["예스24 28,800원 (무료배송)", "교보문고 32,000원 (10% 할인)"],
      "imageIds": [102, 101]
    }
  ],
  "markdown": "### 가격 비교\n\n두 판매처 모두 동일한 도서이며 …",
  "sourceImageIds": [101, 102],
  "skipped": [],
  "modelVersion": "gemini-2.5-flash-lite",
  "generatedAt": "2026-07-16T06:10:00.000Z"
}
```

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| title | String | 문서 제목. 요청의 `title`이 있으면 그 값 |
| summary | String | 문서 전체 요약 |
| sections | Array | 문서 구조 원자료. 앱·Spring이 직접 재조립할 때 쓴다 |
| markdown | String | **최종 산출물.** 위 구조를 렌더링한 마크다운 **본문**. `title`·`summary`는 들어 있지 않다 |
| sourceImageIds | Number[] | 문서 재료로 쓴 imageId (= 요청 − `skipped`). 모델이 어떤 이미지를 어느 섹션에도 인용하지 않아도 여기에는 남는다 |
| skipped | Array | 재료에서 빠진 항목 |
| modelVersion | String | 사용한 LLM 모델명. `LLM_MODEL_NAME` 값(기본 `gemini-2.5-flash-lite`)을 그대로 실어 보낸다 |
| generatedAt | String | 생성 시각. ISO-8601 UTC, 밀리초 3자리 |

**sections[]**

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| heading | String | 섹션 제목 (마크다운 `###`) |
| body | String | 서술 본문. 빈 문자열일 수 있다 |
| bullets | String[] | 나열 항목 (마크다운 `-`). 빈 배열일 수 있다 |
| imageIds | Number[] | 이 섹션의 근거 이미지. **요청에 실제로 있던 id만 남는다** |

**skipped[]**

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| imageId | Number | 문서에서 빠진 이미지 |
| reason | String | `NO_CONTENT` — 제목·요약·주요정보·OCR이 전부 비어 있음 |

> 응답에 `imageId` 단수 필드는 **없다.** N장이 문서 1개로 합쳐지므로 출처는
> `sourceImageIds`와 섹션별 `imageIds`로만 표시한다.

`markdown` 필드의 출력 형태:

```markdown
### {heading}

{body}

- {bullet}

### {heading}

{body}
```

**제목·요약은 들어 있지 않다.** 앱이 `title`을 화면 헤더에, `summary`를 '요약' 블록에
각각 따로 그린 뒤 이 본문을 그 아래에 렌더하므로, 본문에 또 담으면 제목·요약이 한 화면에
두 번 나온다. 두 값은 응답 JSON의 `title`·`summary`로 이미 나가니 그대로 쓰면 된다.

이미지 id와 출처 표도 넣지 않는다. 내부 id는 사용자에게 의미가 없고 본문 흐름을 끊는다.
근거 매핑은 `sections[].imageIds`로만 나간다.

섹션이 하나도 없으면 본문 대신 `summary`(그것도 비면 `title`)를 한 줄 담는다 —
빈 문자열을 내보내면 Spring이 '완료인데 내용 없음'으로 보고 실패 처리한다.

### 에러

```json
{ "error": "CODE", "message": "...", "detail": {} }
```

| error | HTTP | 설명 |
| --- | --- | --- |
| UNAUTHORIZED | 401 | 내부 토큰 불일치·누락 |
| INVALID_REQUEST | 400 | `images`가 빈 배열 |
| INVALID_REQUEST | 400 | `images` 31건 이상. `detail: {requested, limit}` |
| INVALID_REQUEST | 400 | 스키마 위반(예: `imageId` 누락). `detail.errors` 최대 5건 |
| NO_DOCUMENT_SOURCE | 400 | 쓸 수 있는 이미지가 하나도 없음 |
| DOCUMENT_GENERATION_FAILED | 502 | Gemini 호출·JSON 파싱 실패 |

> **422는 나오지 않는다.** FastAPI의 검증 오류를 400 `INVALID_REQUEST`로 변환하는
> 핸들러가 있다. Spring은 422 분기를 만들 필요가 없다.

### 처리 규칙

| 조건 | 동작 |
| --- | --- |
| 같은 `imageId` 중복 전달 | 첫 번째만 사용, 나머지는 무시 |
| 항목의 `title`·`summary`·`keyInformation`·`ocr`이 **전부** 빔 | 그 항목만 `skipped: NO_CONTENT`, 나머지로 계속 진행 |
| 쓸 수 있는 항목이 하나도 없음 | 400 `NO_DOCUMENT_SOURCE` |
| `ocr` 텍스트가 1500자 초과 | 앞 1500자만 사용. **Spring이 미리 자르지 말 것** |
| 모델이 요청에 없는 `imageId`를 출처로 반환 | 해당 id 제거. 거짓 근거가 문서에 남지 않게 한다 |
| 모델 응답의 제목·항목에 줄바꿈 포함 | 공백으로 치환. 마크다운 구조가 깨지지 않게 한다 |
| `summary`·`body` 줄 앞에 `#`·`>`·`\|` | `\` 로 escape. `#` 이 그대로 나가면 섹션이 문서 최상위 제목으로 승격된다 |
| `summary`·`body` 에 코드펜스(```` ``` ````) | 그 줄을 버린다. 펜스가 열리면 뒤따르는 출처 표까지 코드 블록에 먹힌다 |
| 모델이 `sections` 를 하나도 주지 않음 | **현재는 200.** 제목 + 출처 표만 있는 문서가 나간다(실패로 끊지 않는다) |

상한값 — `MAX_IMAGES = 30`(요청당 이미지 수), `OCR_CHARS = 1500`(이미지당 OCR 길이).
둘 다 `app/document.py` 상수이며 프롬프트 크기 = 지연·비용과 직결된다. 실측 후 조정한다.

### 연동 유의사항

**요청 전에 Spring이 끝내야 하는 것.** FastAPI는 받은 것을 그대로 쓴다.

| 항목 | 내용 |
| --- | --- |
| 소유자 필터 | 해당 `userId` 소유 이미지만 담는다. 남의 것이 섞이면 그대로 문서에 실린다 |
| 분석 완료 필터 | `status = COMPLETED`만 담는다. `QUEUED`·`PROCESSING`·`EMPTY`는 내용이 비어 `NO_CONTENT`로 버려지거나 빈약한 섹션이 된다 |
| tags 이름 변환 | `{tagId, name}` 객체가 아닌 이름 문자열 배열로 보낸다 |
| 30건 분할 | 초과 시 Spring이 나눠 호출하고 합치거나, 사용자에게 장수를 줄이도록 안내한다 |

**보관 여부 확인이 필요한 필드.**

| 필드 | 확인 사항 |
| --- | --- |
| ocr.rawText | **10-4 콜백에 OCR 원문은 들어 있지 않다.** 앱이 4-1 등록 요청에 실어 보낸 것을 Spring이 저장하고 있어야 한다. 없으면 문서 품질이 "요약문 이어붙이기" 수준까지 떨어진다 |
| keyInformation | FastAPI가 10-4로 보내고 있으나 명세 밖 합의 필드라 Spring이 버리고 있을 수 있다 |

**Spring 쪽에 필요한 구현.** FastAPI가 제공하는 것은 이 API 하나뿐이다.

| 구분 | 내용 |
| --- | --- |
| 앱용 API | `POST /api/v1/documents` 등. 앱에서 `imageIds`를 받는다 |
| DB 조회 | 소유자·분석 완료 필터 후 Request Body 형태로 변환 |
| FastAPI 호출 | 이 API를 **1회** 호출(장당 호출 아님) |
| 저장 | `document`(documentId PK, userId, title, markdown, modelVersion, generatedAt)<br>`document_image`(documentId, imageId, sortOrder) — N:M. `documentId` 채번은 Spring 몫 |
| 조회 API | 문서 목록·상세 |

**응답 시간.** Gemini 호출 1회라 이미지 수에 비례해 늘지는 않지만 프롬프트가 커지면
함께 길어진다. Spring의 HTTP 클라이언트 타임아웃을 **60초 이상**으로 잡는다.

단, 429가 오면 재시도가 붙는다 — `GEMINI_MAX_ATTEMPTS`(기본 5회) × `GEMINI_TIMEOUT`
(기본 90초, 호출 1회당 상한) + 백오프 대기(1·2·4·8초)라 최악의 경우 훨씬 길어진다.
정상 경로는 한 번에 끝나지만, Spring은 **자기 타임아웃을 먼저 끊고** `DOCUMENT_GENERATION_FAILED`
와 같이 처리하는 편이 안전하다. 앞단 nginx의 `proxy_read_timeout` 은 120초다.

### 열려 있는 사항

| 항목 | 내용 |
| --- | --- |
| 명세 번호 | `10-6` 편입 여부 팀 확정 필요 |
| 비동기 전환 | 현재 동기. 30장 응답이 실측으로 느리면 10-1처럼 `202` + 콜백으로 전환(FastAPI 추가 작업) |
| 멱등성 | 같은 imageIds로 다시 호출하면 매번 새로 생성된다. 같은 결과를 원하면 Spring이 저장본을 돌려준다 |
| instruction 신뢰 경계 | 사용자 입력이 그대로 프롬프트에 들어간다. 내부 API라 Spring이 앞단에서 거르는 것을 전제로 한다 |

---

실제 출력 샘플: [`samples/`](samples/) — 이 API 를 호출해 받은 마크다운·응답 JSON

구현: [`app/document.py`](../app/document.py) · 엔드포인트 [`app/main.py`](../app/main.py) ·
자체 점검 `python test/test_document.py` (네트워크·Gemini 키 불필요) ·
Swagger `/docs` 의 Example Value 에 실행 가능한 예시 포함

> `ai/ai_main/test/` 는 `.gitignore` 대상이라 저장소에 올라가지 않는다. clone 만 한
> 상태에서는 이 파일이 없다 — 로컬에서 직접 두고 쓴다.
