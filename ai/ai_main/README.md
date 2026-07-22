# 스크린샷 지식 DB — AI 서비스 (FastAPI 내부 API)

프로토타입 노트북을 서버용으로 재구성한 것입니다. 명세 10장(내부 API)을 따릅니다.

## 실행

```bash
pip install -r requirements.txt
cp .env.example .env        # 값 채우기
uvicorn app.main:app --host 0.0.0.0 --port 8000
```

`GEMINI_API_KEY` 와 `INTERNAL_TOKEN` 은 기본값이 없습니다. 설정하지 않으면 첫 요청 시점에 바로 실패합니다.

## 제공 엔드포인트

| 명세 | Method | Path | 처리 |
| --- | --- | --- | --- |
| 10-1 | POST | `/internal/v1/analyze` | 비동기. 202 반환 후 10-4 로 콜백 |
| 10-2 | POST | `/internal/v1/embed` | 동기 |
| 10-3 | POST | `/internal/v1/query/parse` | 동기. 실패 시 `parsedConditions: null` 로 degrade |
| — | POST | `/internal/v1/search` | 동기. OpenSearch 키워드(BM25) 검색. `userId` 로 격리 |
| — | GET | `/health` | 헬스체크 |

FastAPI 가 호출하는 쪽: 10-4 콜백, 10-5 지식 후보 조회.

모든 요청에 `X-Internal-Token` 헤더가 필요합니다. 에러는
`{"error": CODE, "message": ..., "detail": {}}` 형식입니다.

## 구조

```
app/
  config.py         환경변수 설정 (자격증명 기본값 없음)
  schemas.py        요청·응답 스키마 (내부 snake_case ↔ 경계 camelCase)
  gemini_client.py  Gemini 호출 격리 (SDK 교체 시 이 파일만 수정)
  storage.py        S3 원본 이미지 조회
  spring_client.py  10-4 콜백 / 10-5 후보 조회
  category.py       카테고리 유사도 판정 (무상태)
  stages.py         LLM / IMAGE_ANALYSIS / AGENT 단계 실행 + 검색 색인
  search.py         OpenSearch 키워드 검색 (nori 색인/조회)
  jobs.py           jobId+stage 멱등 처리
  main.py           FastAPI 엔드포인트
```

## 검색 (OpenSearch)

FastAPI 가 색인·검색을 전담한다. AGENT 단계가 끝나면 요약·태그·카테고리·OCR
원문을 색인하고, `/internal/v1/search` 로 BM25 키워드 검색을 제공한다.
한글 품질을 위해 **nori** 형태소 분석기를 쓰므로 플러그인이 설치된 이미지가 필요하다
(`opensearch.Dockerfile` 참고). 로컬/시연은 `docker-compose up -d` 로 nori 포함
OpenSearch + AI 서비스를 함께 띄운다.

> 색인은 best-effort 다. OpenSearch 가 잠시 죽어도 분석·콜백은 정상 진행되고,
> 해당 이미지는 검색 인덱스에만 누락된다(재분석 시 덮어쓰기).

## 팀 확인이 필요한 사항

1. **카테고리 대표 벡터.** 지금은 10-5 응답에 벡터가 없어 카테고리 *이름* 임베딩으로 비교합니다. pgvector 에 카테고리 centroid 를 유지하고 10-5 응답에 `representativeVector` 를 실어주면 정확도가 올라갑니다. 스키마는 이미 이 필드를 받도록 준비돼 있습니다.
2. **기본 카테고리 위치.** 사용자 카테고리가 하나도 없을 때 `stages.DEFAULT_CATEGORIES` 17개를 후보로 씁니다. 원칙적으로는 Spring DB 시드로 옮기는 편이 낫습니다.
3. **`categoryCreated` 필드.** AGENT 결과에 신규 카테고리 여부를 담았습니다. 명세에 없는 필드라 Spring 과 합의가 필요합니다(불필요하면 제거).
3-1. **`documentVector` 필드.** AGENT 결과에 검색용 문서 임베딩(요약 기준, `DOCUMENT`)을 함께 실어 보냅니다. Spring 은 콜백 한 번으로 메타데이터와 벡터를 같이 받아 pgvector 등에 적재하면 됩니다(임베딩 재호출 불필요). `embeddingModel`·`embeddingDimension` 도 함께 넘어가니 벡터 컬럼 차원과 일치하는지 확인이 필요합니다. 명세 밖 필드라 Spring 과 계약 확정이 필요합니다.
4. **`keyInformation`.** 명세 10-4·6-2 에 있는 필드라 AGENT 가 생성하도록 넣었습니다. MVP 에서 안 쓸 거면 프롬프트에서 빼면 됩니다.
5. **멱등 저장소.** `jobs.py` 는 프로세스 메모리 기반이라 단일 인스턴스 전제입니다. 다중 인스턴스·재시작 대응이 필요하면 Redis 로 옮겨야 합니다.
6. **SDK.** `google-generativeai` 는 지원 종료 예고 상태입니다. `gemini_client.py` 한 파일만 고치면 `google-genai` 로 옮길 수 있습니다.

## 범위에서 제외

- **구조화 데이터(structuredData)** — MVP 제외. AGENT 는 `{"type": null, "fields": {}}` 빈 형태만 반환합니다. 확장 시 `stages.run_agent_generation` 프롬프트에 유형별 스키마를 추가하세요.
