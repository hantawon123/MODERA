# 모델 비용 최적화 1차 (2026-08-03)

토큰 소모가 예상을 크게 웃돌아 원인을 조사하고, 기능별로 모델을 재배치했다.
브랜치 `feat/model-cost-optimization`.

## 문제

AGENT 대량 분석이 `gemini-3.5-flash` 단일 모델로 돌고 있었고, 토큰 청구가
프롬프트 크기로 추정한 값보다 훨씬 컸다.

## 원인 (실측·조사 결과)

1. **단가 자체가 비쌌다.** 3.5-flash 는 $1.50/$9.00(1M 토큰, 입력/출력) —
   2.5-flash($0.30/$2.50)의 입력 5배·출력 3.6배. "flash = 싸다"는 가정이
   3.5 세대에서 깨졌다.
2. **thinking 토큰.** 3.5-flash 는 내부 추론(thinking)이 기본 medium 으로
   켜져 있고, thinking 토큰은 **출력 단가($9/M)로 과금**된다. 구
   `google-generativeai` SDK 는 thinkingConfig 를 전달할 수 없어 끌 방법이
   없었다. 2.5-flash 도 기본 ON(dynamic)이라 같은 문제가 잠복해 있었다.
3. 부수 요인: 검색 조건 변환(10-3)까지 AGENT 모델을 따라가며 검색마다
   상위 모델 단가를 내고 있었다.

## 단가표 (2026-08-03 조사, GMS 허용 모델만)

| 모델 | 입력/출력 ($/1M) | 숨은 추론 비용 |
|---|---|---|
| gemini-2.5-flash-lite | 0.10 / 0.40 | thinking 기본 OFF |
| gemini-2.5-flash | 0.30 / 2.50 | thinking 기본 ON — budget=0 으로 꺼야 함 |
| gemini-2.5-pro | 1.25 / 10.00 | thinking 못 끔(최소 128) |
| gemini-3.5-flash | 1.50 / 9.00 | thinking 기본 medium, thinking_level 로만 제어 |
| (참고) gpt-5-mini | 0.25 / 2.00 | reasoning 기본 medium — minimal 필요 |
| (참고) gpt-5-nano | 0.05 / 0.40 | 동일 |
| (참고) claude-haiku-4-5 | 1.00 / 5.00 | thinking 기본 OFF |
| (참고) claude-sonnet-4-6 | 3.00 / 15.00 | 품질 카드 |

주의: 실비용은 GMS 크레딧 환산 기준이라 리스트 단가와 다를 수 있다.
usage 로그(아래)로 실측할 것.

## 1차 배치 (현재 적용)

| 기능 | 환경변수 | 모델 | 근거 |
|---|---|---|---|
| AGENT 융합 분석 (대량 주력) | `LLM_MODEL_NAME` | gemini-2.5-flash | 장당 출력 단가 72% 절감. 품질은 111장 A/B 로 확인 예정 |
| 정보성 판정 | `INFORMATIVE_MODEL_NAME` | gemini-2.5-flash | lite 는 계산기·배터리 누수 실측 있어 금지 |
| 문서 생성 (희소, 사용자 대면) | `DOCUMENT_MODEL_NAME` | gemini-3.5-flash | 품질 우선. 호출이 희소해 단가 무의미 |
| 검색 조건 변환 (지연 민감) | `QUERY_PARSE_MODEL_NAME` | gemini-2.5-flash-lite | 단순 추출 작업. 비용+지연 동시 개선 |
| 이미지 분석 폴백 | `VISION_MODEL_NAME` | gemini-2.5-flash-lite | 기존 유지 |
| 카테고리 재분석 | (LLM 따라감) | gemini-2.5-flash | 희소 호출, 분리 가치 없음 |
| 임베딩 | `EMBEDDING_MODEL_NAME` | gemini-embedding-2 @768 | pgvector·저장 벡터 계약 — 변경 불가 |

미설정 시 INFORMATIVE/DOCUMENT/QUERY_PARSE 는 LLM 을 따라간다(하위호환).

## thinking 제어

`google-genai` SDK 로 이전하면서 thinking 을 설정으로 제어한다
(`app/gemini_client.py`). 세대별 파라미터가 다르고 한 요청에 둘 다 보내면
400 이라, 모델명을 보고 하나만 고른다.

| 환경변수 | 대상 | 값 | 현재 |
|---|---|---|---|
| `GEMINI_THINKING_BUDGET` | 2.5 계열 | 토큰 정수. 0=끔, 음수=미전송 | 0 (끔) |
| `GEMINI_THINKING_LEVEL` | 3.x 계열 | minimal/low/medium/high, 빈 값=미전송 | 빈 값 (문서 생성 품질 우선) |

## 변경 내역

- 모델 역할 분리: `config.py` 에 DOCUMENT/QUERY_PARSE 스위치 추가,
  `internal_api.py`(10-3)·`document.py` 가 전용 모델 사용
- SDK 이전: google-generativeai → google-genai (`gemini_client.py` 전면 교체,
  공개 함수 시그니처 불변 — 호출부 수정 없음)
- JSON 모드(`response_mime_type`) 적용 — 코드펜스·잡토큰 제거
- 호출마다 usage INFO 로그: `Gemini usage model=... prompt=... thoughts=...
  candidates=... total=...` — **thoughts 가 thinking 토큰**이다. 절감 검증은
  이 값이 None/0 인지로 판정한다
- 임베딩 배치 100건 분할(구 SDK 자동 분할 대체)

검증(2026-08-03, GMS 실호출): generate(2.5-flash)·vision(lite)·embed(768)
3경로 통과. thinking 차단 실측 — `prompt=22 thoughts=None candidates=38
total=60` (total = prompt + candidates, thinking 0).

## 남은 일

1. **111장 A/B**: 3.5-flash → 2.5-flash 하향의 카테고리 정확도 확인.
   미달이면 후보 순서: gpt-5-mini(reasoning minimal) → claude-haiku-4-5.
   어느 쪽이든 별도 클라이언트 신설 필요.
2. **문서 생성 품질 모니터링**: 불만 생기면 첫 카드는 claude-sonnet-4-6.
3. **2.5 세대 은퇴(2026-10-16)**: 그 이후 운영 시 GMS 에 3.x 저가 라인
   (3.5-flash-lite $0.30/$2.50, 3.1-flash-lite $0.25/$1.50) 추가 요청 필요.
4. **GPT/Claude 도입 시**: 추론 제어 변수는 프로바이더별로 따로 둔다
   (`OPENAI_REASONING_EFFORT=minimal` 등). GEMINI_THINKING_* 은 Gemini 전용
   — 단위·개념이 달라 통합 변수는 만들지 않는다.
