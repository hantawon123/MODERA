# 문서화 API 산출물 샘플

[`../DOCUMENT_API.md`](../DOCUMENT_API.md) 의 `POST /internal/v1/documents` 를 **실제로 호출해
받은 출력**이다. 손으로 쓴 예시가 아니다. 입력은 전부 합성 데이터(가상의 스크린샷 분석 결과)다.

| 파일 | 입력 | 무엇을 보여주나 |
| --- | --- | --- |
| [`구매후보-비교.request.json`](구매후보-비교.request.json) | — | 아래 두 파일을 만든 **요청 본문 그대로**. 그대로 다시 호출할 수 있다 |
| [`구매후보-비교.md`](구매후보-비교.md) | 도서 쇼핑 3장 + 중복 1장 + 빈 항목 1장 | 신품/중고 2섹션 분리, 중복 `imageId` 무시, 빈 항목 `skipped` 제외 |
| [`구매후보-비교.response.json`](구매후보-비교.response.json) | 위와 동일 | 응답 전문. `skipped` 가 채워진 실제 예시 |
| [`여행-일정.md`](여행-일정.md) | 항공·숙소·맛집·관광지·렌터카 5장 | **섹션 자동 분리**(예약 확정 / 후보), 요청 `title` 고정, 섹션별 출처 분리 |
| [`여행-일정.response.json`](여행-일정.response.json) | 위와 동일 | 응답 전문. Spring 이 받는 실제 JSON 구조 |

생성 정보 — `구매후보-비교` 는 2026-07-30, 모델 `gemini-2.5-flash-lite`(현재 기본값,
`LLM_MODEL_NAME`). 응답 3.2초.

> `여행-일정.response.json` 의 `modelVersion` 은 `gemini-3.5-flash-lite` 로 남아 있다.
> 그 이름은 GMS 화이트리스트에 없어(`400 Model is not available`) 지금은 재현되지 않는다.
> 기록된 출력이라 손으로 고치지 않았다 — 입력 payload 도 보관돼 있지 않아 다시 만들려면
> 입력부터 새로 써야 한다. `구매후보-비교` 쪽만 request/response 가 짝으로 맞는다.

## 읽을 때 볼 것

- **N장 → 문서 1개.** 두 샘플 모두 여러 장이 하나의 글로 합쳐졌다. 장당 문서가 아니다.
- **응답 형식은 두 샘플이 동일하다.** 최상위 8필드 + `sections[]` 4필드. `skipped` 가 비면
  `[]` 로만 나오고 필드 자체는 사라지지 않는다 — Spring 파서는 한 형태만 다루면 된다.
- **섹션 구성은 `category` 가 아니라 `instruction` 을 따른다.** `여행-일정` 의 입력 카테고리는
  예약·음식·여행 3종인데, `"예약 확정된 것과 후보만 있는 것을 구분해 줘"` 라는 지시에 맞춰
  2섹션으로 갈렸다.
- **사실은 원문 그대로 보존된다.** 예약번호(`A1B2C3`, `HT-9931`), 금액, 시각이 입력 OCR·
  `keyInformation` 값과 일치한다. 지어낸 값은 없다.
- **출처가 두 층으로 붙는다.** 섹션 끝의 `> 출처: #201, #202` 와 문서 맨 끝의 출처 표.
  사용자가 원본 스크린샷을 되찾을 수 있게 하기 위한 것이다.
- **정렬은 계약이 아니다.** `구매후보-비교` 의 `instruction` 은 "가격순으로 비교해 줘" 인데
  섹션 안 항목은 32,000원 → 28,800원 순으로 나왔다. 정렬을 보장해야 하면 프롬프트
  규칙([`app/document.py`](../../app/document.py) `build_prompt`)에 명시해야 한다.

## 다시 만들려면

```bash
curl -X POST http://localhost:8000/internal/v1/documents \
  -H "X-Internal-Token: $INTERNAL_TOKEN" -H 'Content-Type: application/json' \
  -d @docs/samples/구매후보-비교.request.json
```

Swagger `/docs` 의 Example Value 를 Try it out 해도 같은 성격의 출력이 나온다.
모델 출력이라 문장·섹션 구성은 매번 조금씩 달라진다.
