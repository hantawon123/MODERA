# 스크린샷 지식 DB — AI 서비스 (FastAPI 내부 API)

프로토타입 노트북을 서버용으로 재구성한 것입니다. 명세 10장(내부 API)을 따릅니다.

> 2026-07-22: 별도로 진행되던 `fastapi/` 서버와 병합했습니다. 이 디렉터리가 단일 기준입니다.
> 병합 내역은 아래 [병합 기록](#병합-기록) 참고.

## 실행

```bash
pip install -r requirements.txt
cp .env.example .env        # 값 채우기
uvicorn app.main:app --host 0.0.0.0 --port 8000

python test_pipeline.py     # Gemini·Spring·S3 없이 도는 자체 점검
```

`GEMINI_API_KEY` 와 `INTERNAL_TOKEN` 은 기본값이 없습니다. 설정하지 않으면 첫 요청 시점에 바로 실패합니다.
로컬 개발에서는 `.env` 파일을 자동으로 읽고, 운영(Docker)에서는 컨테이너 환경변수를 그대로 씁니다.

## 제공 엔드포인트

| 명세 | Method | Path | 처리 |
| --- | --- | --- | --- |
| 10-1 | POST | `/internal/v1/analyze` | 비동기. 202 반환 후 10-4 로 콜백 |
| 10-2 | POST | `/internal/v1/embed` | 동기 |
| 10-3 | POST | `/internal/v1/query/parse` | 동기. 실패 시 `parsedConditions: null` 로 degrade |
| — | POST | `/internal/v1/analyze/upload` | **명세 외. MVP 수동 테스트 전용** (아래 참고) |
| — | GET | `/health` | 헬스체크 |

FastAPI 가 호출하는 쪽: 10-4 콜백, 10-5 지식 후보 조회.

모든 요청에 `X-Internal-Token` 헤더가 필요합니다. 에러는
`{"error": CODE, "message": ..., "detail": {}}` 형식입니다.

### `/internal/v1/analyze/upload` — Postman 테스트용

운영 경로는 `모바일 → Spring → S3(s3Key) → stage 별 /analyze` 입니다. 이 엔드포인트는
Spring 과 S3 없이 파이프라인 전체를 확인하기 위한 것으로, **콜백도 멱등 처리도 하지 않고**
LLM → IMAGE_ANALYSIS → AGENT 를 순서대로 실행해 결과를 동기로 돌려줍니다.

- Body: **form-data**, Header: `X-Internal-Token`

| key | type | 필수 | 설명 |
|---|---|---|---|
| `image` | **File** | O | 스크린샷 파일 |
| `ocrText` | Text | X | 모바일 OCR 텍스트. **비우면 서버가 비전 모델로 직접 OCR** |
| `userId` | Text | X | 10-5 후보 조회 대상 (기본 0) |
| `maxTags` / `language` | Text | X | 기본 10 / ko |

응답은 AGENT 결과에 `ocrText` · `llm` · `imageAnalysis` 를 덧붙인 형태라, 어느 단계에서
무슨 판단이 나왔는지 한 번에 볼 수 있습니다. OCR 텍스트가 비정보성이면 비전·AGENT 를
건너뛰고 `기타` 로 끝냅니다(프로토타입 분기와 동일).

```bash
curl -X POST http://localhost:8000/internal/v1/analyze/upload \
  -H "X-Internal-Token: dev-secret-change-me" \
  -F "image=@./shot.png"
```

## 구조

```
app/
  config.py         환경변수 설정 (.env 자동 로드, 자격증명 기본값 없음)
  schemas.py        요청·응답 스키마 (내부 snake_case ↔ 경계 camelCase)
  gemini_client.py  Gemini 호출 격리 (google-genai, response_schema 로 JSON 강제)
  storage.py        S3 원본 이미지 조회
  spring_client.py  10-4 콜백 / 10-5 후보 조회
  category.py       카테고리 유사도 판정 (무상태)
  stages.py         LLM / IMAGE_ANALYSIS / AGENT 단계 실행 + 업로드 테스트 파이프라인
  jobs.py           jobId+stage 멱등 처리
  main.py           FastAPI 엔드포인트
test_pipeline.py    자체 점검 (외부 호출 없음)
```

## 프로토타입에서 바뀐 점

- **easyocr 제거.** OCR 은 모바일 온디바이스가 수행하고 Spring 이 텍스트를 넘겨줍니다.
  (`/analyze/upload` 로 텍스트 없이 이미지만 올린 경우에만 서버가 대신 OCR 합니다.)
- **로컬 파일 → S3.** 비전 분석은 `input.image.s3Key` 로 S3 객체를 읽습니다.
- **통짜 파이프라인 → stage 분리.** 한 요청은 한 단계만 실행하고 결과를 콜백합니다. 다음 단계 진행 여부는 Spring 이 판단합니다.
- **전역 CategoryStore 제거.** 요청마다 10-5 로 사용자별 후보를 받아 판정합니다.
- **print → logging.** 단계 시작·종료·카테고리 판정 근거가 로그로 남습니다.
- **API 키 하드코딩 제거.**

## 병합 기록

`fastapi/` 서버에서 다음을 가져왔습니다(2026-07-22, 팀 합의 반영).

| 항목 | 내용 |
|---|---|
| SDK 교체 | `google-generativeai`(지원 종료) → **`google-genai`**. 모든 모델 호출에 `response_schema` 적용 |
| 임베딩 | 텍스트마다 1콜 → **배치 1콜**. 모델은 `gemini-embedding-2` 유지(입력 8192 토큰·멀티모달, 3072차원) |
| 업로드 테스트 | `/internal/v1/analyze/upload` + 서버 사이드 OCR |
| 자체 점검 | `test_pipeline.py` |
| `.env` 로딩 | 로컬 개발용. 자격증명 필수 규칙은 그대로 유지 |
| 비명세 필드 | AGENT 결과에 `categoryId` / `categorySimilarity` / `categoryMatchedBy` 추가 |

버린 것: 통짜 파이프라인, in-memory `CategoryStore`, `dict` 기반 멱등 처리, camelCase 직접 선언 스키마.

## 팀 확인이 필요한 사항

1. **카테고리 대표 벡터.** 지금은 10-5 응답에 벡터가 없어 카테고리 *이름* 임베딩으로 비교합니다. pgvector 에 카테고리 centroid 를 유지하고 10-5 응답에 `representativeVector` 를 실어주면 정확도가 올라갑니다. 스키마는 이미 이 필드를 받도록 준비돼 있습니다.
2. **기본 카테고리 위치.** 사용자 카테고리가 하나도 없을 때 `stages.DEFAULT_CATEGORIES` 17개를 후보로 씁니다. 원칙적으로는 Spring DB 시드로 옮기는 편이 낫습니다.
3. **멱등 저장소.** `jobs.py` 는 프로세스 메모리 기반이라 단일 인스턴스 전제입니다. 다중 인스턴스·재시작 대응이 필요하면 Redis 로 옮겨야 합니다.
4. **유사도 임계값 0.80.** 실제 스크린샷 분포를 보고 조정해야 합니다.

**합의 완료:** `structuredData` 는 `{"type": null, "fields": {}}` 형태 / 명세 외 필드(`categoryCreated` 등)는
당분간 유지 / `keyInformation` 유지 / 임베딩은 `gemini-embedding-2`.

## 범위에서 제외

- **구조화 데이터(structuredData)** — MVP 제외. AGENT 는 `{"type": null, "fields": {}}` 빈 형태만 반환합니다. 확장 시 `stages.run_agent_generation` 프롬프트에 유형별 스키마를 추가하세요.
