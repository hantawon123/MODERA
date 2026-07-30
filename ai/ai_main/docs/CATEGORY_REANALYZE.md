# 카테고리 재분석 API 명세

> 명세 번호는 **제안**이다. 10장 계열에 편입할지 팀 확정이 필요하다.

## 10-7 카테고리 재분석

| 항목 | 내용 |
| --- | --- |
| API | `POST /internal/v1/categories/reanalyze` |
| 방향 | Spring → FastAPI |
| 처리 | 동기 |
| 설명 | 이미지 1장의 카테고리 분석 결과가 맘에 들지 않을 때, **거부한 카테고리를 배제한 상태로 다시 판정**한다. 바뀌는 것은 카테고리 하나뿐이다 — 제목·요약·태그·주요정보·OCR 은 그대로 둔다. 원본 이미지를 다시 읽지 않는다(비전 호출 0회). |
| 비용 | LLM 1회 + 임베딩 1회. 전체 재분석(FULL, 비전 포함)의 일부다 |

### Request Header

| 이름 | 설명 |
| --- | --- |
| X-Internal-Token | 필수. 서비스 간 공유 토큰 |
| Content-Type | 필수. `application/json` |

### Request Body

```json
{
  "imageId": 18,
  "excludedCategoryIds": [3, 5, 7, 8, 9]
}
```

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| imageId | Number | 필수. 1 이상. 재분석할 이미지 |
| excludedCategoryIds | Array&lt;Number&gt; | 필수. 사용자가 거부한 카테고리 id **누적 목록**. 1 ~ 5건 |

**`userId` 는 받지 않는다.** 이미지 문서에 소유자가 이미 박혀 있어 서버가 읽는다.
요청에서 받으면 값이 어긋날 때 어느 쪽을 믿을지 정해야 하고, 그건 격리 사고의 입구다.

### Response Body

```json
{
  "imageId": 18,
  "categoryId": 11,
  "categoryName": "새 카테고리"
}
```

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| imageId | Number | 요청 값 그대로 |
| categoryId | Number | 기존 카테고리로 판정되면 Spring 이 준 `categoryId`, **신규면 이름 해시**(`search.stable_id`) |
| categoryName | String | 최종 카테고리명. 요청의 `excludedCategoryIds` 에 해당하는 이름은 절대 나오지 않는다 |

> 응답에 `categoryCreated` 는 없다(요청받은 계약이 3필드다). 신규 여부는 서버 로그에
> 남는다 — Spring 은 `categoryId` 가 자기 DB 에 없으면 신규로 보고 생성하면 된다.

**`categoryId` 값 체계는 두 갈래다.** 위 예시의 `11` 처럼 Spring DB id 가 나오는 것은
그 카테고리가 10-5 후보에 `categoryId` 를 달고 왔을 때뿐이다. 판정 결과가 진짜 신규면
줄 id 가 없어 `search.stable_id(이름)`(31비트 이름 해시, 예: `2130483261`)를 넣는다 —
**응답 양식은 같고 값의 출처만 다르다.** Spring 이 그 카테고리를 만들고 다음 10-5
응답에 자기 id 를 실어 주면 그 뒤 재분석부터는 Spring id 로 통일된다.

### 계약 실측

위 요청·응답 예시 그대로 호출해 확인했다(후보에 `categoryId: 11` 인 `새 카테고리` 를
둔 상태 — 그래서 예시의 `11` 이 그대로 나온다).

```
POST /internal/v1/categories/reanalyze     X-Internal-Token: <토큰>
Content-Type: application/json
{"imageId": 18, "excludedCategoryIds": [3, 5, 7, 8, 9]}

→ HTTP 200  Content-Type: application/json
{"imageId":18,"categoryId":11,"categoryName":"새 카테고리"}
```

키 이름·순서·타입 일치, 여분 필드 없음. 토큰을 빼면 401. camelCase 는
`schemas.CamelModel` 의 alias 로 나온다(내부 API 공통).

### 반복 제한 — 최대 5회

거부 목록은 **서버가 들고 있지 않다.** 매 호출에 누적 배열 전체를 받는다.

| 시도 | 요청 |
| --- | --- |
| 1회 | `[3]` |
| 2회 | `[3, 5]` |
| … | … |
| 5회 | `[3, 5, 7, 8, 9]` |
| 6회 | `[3, 5, 7, 8, 9, 11]` → **400** (`INVALID_REQUEST`) |

배열 길이가 곧 시도 횟수라 상한 하나(`reanalyze.MAX_EXCLUDED = 5`)가 그대로 회수
제한이 된다. 서버 세션으로 만들면 `jobs.py` 처럼 프로세스 메모리가 되어 재기동·다중
인스턴스에서 거부 이력이 날아간다 — 정수 5개를 다시 보내는 비용으로 무상태를 산다.
뒤로 가기·앱 재시작·중간 실패가 전부 "그 시점 목록으로 다시 호출" 하나로 복구된다.

## 처리 순서

1. **이미지 조회** — 색인에서 소유자·요약·제목·주요정보·OCR 을 읽는다. 없으면 404.
2. **재료 확인** — 요약(없으면 제목)이 비어 있으면 400 `NO_ANALYSIS_SOURCE`.
   분석 전이거나 EMPTY(비정보성) 이미지는 재분류할 근거가 없다.
3. **후보 수집** — `stages.build_candidates`(분석 경로와 **같은 함수**):
   Spring 10-5 후보 + 이 서버 카테고리 벡터 저장소 + (둘 다 비면) `DEFAULT_CATEGORIES`.
4. **배제** — 아래 3중 관문. 자세한 이유는 다음 절.
5. **재제안** — 남은 후보와 금지 목록을 넣어 LLM 에 카테고리 하나를 받는다
   (`reanalyze._pick_category`). 분석 프롬프트의 카테고리 규칙만 떼어 온 것이다.
6. **판정** — `category.resolve_category`(분석 경로와 **같은 코어**, 같은 임계값).
   이름 일치 → 연결, 표기 변형 → 흡수, 이름 임베딩 ≥ `CATEGORY_NAME_DUP_THRESHOLD`
   → 흡수, 아니면 신규.
7. **폴백** — 판정 결과가 거부된 이름(또는 그 변형)이면 이름이 가장 가까운 남은
   후보로 떨어진다. 남은 후보가 없으면 502.
8. **centroid 갱신** — 분석 경로와 같은 오염 가드(`CATEGORY_GUARD_MIN_COSINE`).
   폴백으로 정해진 경우는 누적하지 않는다(근거가 약하다).
9. **색인 반영** — `search.set_category` 로 **카테고리 필드만** 부분 병합 + refresh.
   실패해도 판정 결과는 돌려준다(분석 경로와 같은 best-effort). Spring DB 가 최종
   보관이고 이쪽 값은 조회 사본이다.

## 배제 관문 3중

| 관문 | 잡는 것 |
| --- | --- |
| `categoryId` 일치 | Spring DB id 로 거부가 들어온 경우 |
| 이름 해시(`stable_id`) 일치 | 앱 직결 id 로 거부가 들어온 경우(현재 안드는 AI 스펙 직결) |
| **현재 붙어 있는 카테고리** 무조건 배제 | 위 두 관문이 아무것도 못 걸러도 동작한다 |

id 체계가 두 개인 것이 이 API 의 유일한 회색지대다. 어느 쪽 id 가 올지 서버가 알
방법이 없어 둘 다 본다. 그래도 못 걸렀을 때를 대비해 **지금 붙어 있는 카테고리는
언제나 뺀다** — 안 빼면 사용자가 거부한 바로 그 카테고리를 그대로 돌려주는,
"재분석했는데 아무것도 안 바뀐" 응답이 된다. 어느 id 도 안 걸리면 WARNING 을 남긴다.

### 표기 변형도 거부로 본다

`'쇼핑'` 거부 후 모델이 `'쇼핑몰'` 을 제안하면 사실상 같은 카테고리다. 프롬프트로
금지하고(변형·축약 금지), 응답에서도 한 번 더 막는다 — 한쪽 이름이 다른 쪽을
포함하면 같은 것으로 본다(`resolve_category` 의 name-variant 규칙과 같은 기준).

## 에러

| HTTP | error | 상황 |
| --- | --- | --- |
| 400 | `INVALID_REQUEST` | `imageId` ≤ 0, `excludedCategoryIds` 가 비었거나 6건 이상 |
| 400 | `NO_ANALYSIS_SOURCE` | 분석 전·EMPTY 이미지(요약·제목 둘 다 없음) |
| 401 | `UNAUTHORIZED` | 토큰 불일치 |
| 404 | `IMAGE_NOT_FOUND` | 색인에 없는 `imageId` |
| 502 | `REANALYZE_FAILED` | LLM·임베딩 실패, 또는 배제 후 남은 후보 0건 |

형식은 내부 API 공통이다: `{"error": CODE, "message": ..., "detail": {}}`.

## 설정

새로 추가한 환경변수는 없다. 분석 경로와 같은 값을 본다.

| 변수 | 기본 | 쓰임 |
| --- | --- | --- |
| `CATEGORY_NAME_DUP_THRESHOLD` | 0.90 | 이름 임베딩 흡수 문턱 |
| `CATEGORY_GUARD_MIN_COSINE` | 0.45 | centroid 오염 가드 |
| `LLM_MODEL_NAME` | — | 재제안 호출 모델 |

임계값을 따로 두지 않은 것은 의도다 — 복사해 두면 한쪽만 튜닝돼서 "재분석하면
기준이 달라지는" 사고가 난다.

## 함께 바뀐 파일

기능 본체는 [`app/reanalyze.py`](../app/reanalyze.py) 한 파일이고,
[`app/main.py`](../app/main.py) 는 `include_router` 한 줄만 닿는다. 나머지는 곁다리다.

| 파일 | 변경 | 왜 |
| --- | --- | --- |
| `app/search.py` | `set_category()` 추가(7줄) | 카테고리만 부분 병합. `index_document` 로 쓰면 제목·요약·태그를 빈 값으로 덮어쓴다 |
| `app/stages.py` | `_build_candidates` → `build_candidates` (rename) | 후보 수집 로직을 재분석이 그대로 쓴다. 복사하면 후보 우선순위가 두 곳에서 갈린다. 다른 모듈의 `_` 함수를 부르지 않으려고 이름만 공개로 바꿨다 |
| `app/category.py` | docstring 1줄 | 위 rename 반영 |
| `app/gemini_client.py` | `_mock_json` 분기 2줄 | `MOCK_AI=true` 로컬 개발에서 재분석이 빈 응답으로 죽지 않게 |
| `scripts/category_store_check.py`, `docs/CATEGORY_VECTOR_STORE.md` | 호출·참조명 갱신 | 위 rename 반영 |

## 열려 있는 사항

| 항목 | 내용 |
| --- | --- |
| 명세 번호 | `10-7` 편입 여부 팀 확정 필요 |
| `categoryId` 체계 | 신규 카테고리의 `categoryId` 는 이름 해시다. Spring 이 자기 id 를 부여한 뒤 10-5 후보에 실어 주면 다음 재분석부터는 그 id 로 통일된다 |
| 이전 centroid 감산 | 재분석으로 카테고리가 바뀌어도 **이전 카테고리 centroid 에서 그 이미지 몫을 빼지 않는다**(누적 upsert 라 감산 경로가 없다). 재분석 비율이 높아지면 카테고리별 재계산 배치가 필요하다 — `app/reanalyze.py` 의 `ponytail:` 주석 |
| 거부 이름 재제안 | 모델이 거부된 이름을 다시 내면 재프롬프트 없이 1회 폴백한다. 폴백 WARNING 이 자주 뜨면 프롬프트를 손볼 것 |
| 멱등성 | 같은 요청을 다시 보내면 다시 판정한다(LLM 이라 결과가 달라질 수 있다). 재시도 안전성이 필요하면 Spring 이 결과를 보관 |

---

구현: [`app/reanalyze.py`](../app/reanalyze.py) · 엔드포인트 등록 [`app/main.py`](../app/main.py) ·
판정 코어 [`app/category.py`](../app/category.py) · 후보 수집 [`app/stages.py`](../app/stages.py) ·
자체 점검 `python test/test_reanalyze.py` (OpenSearch·Gemini 키·네트워크 불필요, 12케이스) ·
Swagger `/docs` 에서 바로 호출 가능

> `ai/ai_main/test/` 는 `.gitignore` 대상이라 저장소에 올라가지 않는다. clone 만 한
> 상태에서는 이 파일이 없다 — 로컬에서 직접 두고 쓴다.
