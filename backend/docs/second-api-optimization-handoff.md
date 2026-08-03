# 2차 API 개선(유사 이미지 tx 분리·검색 컨슈머 분리) 인수인계

> 작성일: 2026-08-04
>
> 브랜치: `chore/performance-2nd-api`
>
> 목적: 1차 개선(로그인 트랜잭션 분리, Hikari 30)과 같은 방법론으로 두 가지 구조
> 문제를 수정하고, mock Kakao 사용자 + 시딩 데이터 기반으로 개선 전후를 같은
> 조건에서 비교한다.
>
> 상태: 코드 수정·단위 테스트·로컬 E2E 검증 완료. 서버 측정(시딩·E1~E3)은 미실행.

## 1. 왜 이 두 가지인가 (측정 근거)

| 문제 | 근거 |
|---|---|
| A. 유사 이미지·문서화 추천이 worker HTTP(연결 1초·읽기 3초)를 `@Transactional` 안에서 호출 | 로그인에서 같은 패턴(bcrypt-in-tx)이 130명 유입 시 Hikari 고갈(`CannotCreateTransactionException`)을 만든 것을 1차에서 실측·수정. worker가 느려지는 순간 similar 요청들이 커넥션을 3초씩 점유해 무관한 API 전체로 고갈이 전파되는 동일 구조가 남아 있었다 |
| B. worker의 image-analysis 컨슈머가 단일 스레드라 시맨틱 검색이 이미지 분석 뒤에 줄을 섬 | 코드 자체에 "AI 동기 호출이 단일 컨슈머 스레드를 점유해 분석 소비가 지연된다 … 전용 executor 또는 스트림 분리 TODO" 주석이 있었고, 로컬 부하에서 업로드·검색이 같은 `image-analysis-consumer` 스레드에서 순차 처리되는 것을 확인했다. 검색은 api가 최대 10초를 기다리는 사용자 대기 왕복이다 |

## 2. 변경 내용

### A. `ImageSimilarService` 트랜잭션 경계 분리 (api-server)

- `ImageSimilarService`의 클래스 `@Transactional(readOnly = true)` 제거. worker HTTP
  호출과 응답 조립은 트랜잭션 밖에서 수행
- DB 조회는 신설 `ImageSimilarReader`의 짧은 읽기 트랜잭션 3개로 이동
  (소유권+제목 / 결과 재검증 요약 / 문서화 기준 검증) — `LoginCredentialReader`와
  같은 패턴
- **기능 불변**: 응답·에러코드·검증 순서 동일. PostgreSQL 기본 격리(READ COMMITTED)
  에서는 한 트랜잭션 안의 조회도 문장마다 최신 커밋을 읽으므로 트랜잭션을 쪼개도
  관측 가능한 동작이 같다. `ImageSimilarTransactionBoundaryTest`가 경계를 고정한다

### B. `ImageAnalysisConsumer` 시맨틱 검색 전용 스레드 분리 (analysis-worker)

- `IMAGE_SEMANTIC_SEARCH_REQUESTED`만 전용 단일 스레드(`semantic-search-consumer`)
  에서 처리. 이미지 분석(`IMAGE_UPLOADED`/`REUPLOAD`)은 기존 메인 루프 그대로
- **의미론 보존**: payload 파싱은 메인 스레드(파싱 실패=영구 오류 → XACK 스킵 정책
  유지), XACK은 처리 완료 후에만(at-least-once·PEL 회수 불변), shutdown 시 미완료
  검색은 XACK 없이 남겨 재기동 후 PEL로 회수. 유일한 변화는 "이벤트 타입 간 전역
  순서"가 사라지는 것인데 검색↔분석 사이에는 순서 의존이 없다(검색은 원래 분석
  완료본만 본다)
- 이벤트 계약·스트림·api-server 코드는 무변경

### 검증 완료 내역 (로컬)

- `:api-server:test`, `:analysis-worker:test` 전체 통과 (경계 테스트 2종,
  전용 스레드·XACK 보류 테스트 3종 포함)
- 로컬 E2E 스모크(user-flow.js) 24/24 통과 — 파이프라인·similar·시맨틱 검색·문서
  전 구간. worker 로그에서 검색이 `semantic-search-consumer` 스레드에서 처리되고
  `IMAGE_SEARCH_COMPLETED` 발행까지 확인

## 3. 관련 파일

| 파일 | 역할 |
|---|---|
| `api-server/.../image/service/ImageSimilarReader.java` | A: 신설 읽기 트랜잭션 경계 |
| `api-server/.../image/service/ImageSimilarService.java` | A: 트랜잭션 제거·조립만 담당 |
| `api-server/.../ImageSimilarTransactionBoundaryTest.java` | A: 경계 고정 테스트 |
| `analysis-worker/.../event/ImageAnalysisConsumer.java` | B: 검색 전용 executor |
| `analysis-worker/.../event/ImageAnalysisConsumerTest.java` | B: 라우팅·스레드·XACK 테스트 |
| `ops/mock-ai/server.py` | 시딩용 확장: 카테고리 로테이션·검색 키워드·scheduleData(1/7)·documentVector, `/internal/v1/documents`·`/internal/v1/images/search/semantic` 추가, `PORT` env |
| `ops/k6/kakao-user-seed.js` + `run-kakao-user-seed.sh` | 실파이프라인 시딩(계층 분포 50/200/500) + 검증 |
| `ops/k6/kakao-user-capacity.js` | **v2**: 06_detail_similar·07_semantic 흐름 추가(총 7개) |
| `ops/k6/semantic-search-load.js` | E3 검색 축 부하 |
| `ops/k6/run-worker-outage-drill.sh` | E2 러너(worker pause 창 자동화 + manifest) |
| `ops/k6/run-search-under-pipeline.sh` | E3 러너(업로드×검색 동시 + manifest) |

## 4. 시나리오 v2

v1의 5개 흐름(전부 목록 조회)은 A·B 대상 경로를 타지 않아서 2개를 추가했다.
흐름이 7개가 되었으므로 **v1 실행 결과와 절대치를 직접 비교하면 안 된다**
(v2 before/after 비교만 유효).

- `06_detail_similar`: 목록에서 얻은 imageId로 상세 → 유사 이미지. 시딩된 계정이면
  pgvector 실검색, 이미지 없는 계정이면 같은 종류의 조회로 degrade(흐름 유지)
- `07_semantic`: 시맨틱 검색(키워드 5종 로테이션 — mock-ai의 KEYWORDS와 동일 세트).
  `kind=search`로 분리 계측: 스트림 왕복(서버 대기 상한 10초)이라 앱 API의 p95
  300ms 판정과 섞으면 서로 오염되기 때문. 검색 자체 기준은 p95 ≤ 1.5초

## 5. 데이터 시딩

- 대상: 기존 bootstrap된 사용자 구간 **3,000,001~3,000,200 하나만** 시딩한다.
  같은 DB를 쓰므로 before(135)/after(2차) 컨테이너 모두 이 구간으로 로그인해
  비교하면 데이터 조건이 완전히 같다
- 분포(사용자 인덱스로 결정적): 라이트 50장(50%)·미들 200장(30%)·헤비 500장(20%)
  = 약 37,000장, 25 jobs/s 기준 약 25~30분. 합성 16KB 이미지 기준 MinIO 약 0.6GB
- 데이터 내용은 mock-ai가 imageId로 결정: 카테고리 7종 로테이션(+이름당 고정
  categoryId — worker가 id·이름이 둘 다 있어야 INITIAL_CATEGORY_RESOLVED를 발행),
  제목·태그에 검색 키워드, 이미지 1/7에 일정, documentVector는 **카테고리별 클러스터
  벡터**(완전 난수는 768차원에서 직교라 worker 유사도 임계값 0.6을 못 넘는다 —
  기저 85%+노이즈 15%로 같은 카테고리 코사인 ≈ 0.97) → 카테고리 필터·일정 목록·
  keyword/시맨틱 검색·similar가 전부 실데이터로 동작
- 로컬 검증 완료: 시딩 스모크에서 카테고리 행 생성·imageCount 분산, 일정 생성
  (`여행 영수증 캡처 1379`, 12-08 11:00), keyword 검색 히트, similar 실반환
  (score 0.97), 문서 생성까지 전 체인 확인
- **사전 팀 합의 필요**: 공유 DB에 영구 행 ~4만 개가 추가된다. `@perf.modera.test`
  구간이라 식별 가능하지만 삭제 정책은 별도 결정 전까지 직접 DELETE 금지(기존
  인수인계 문서와 동일 정책)

## 6. 실험 절차와 판정 기준

공통: worker를 fastapi + 확장 mock-ai로 전환(기존 `docker-compose.worker-override.yml`
절차), mock Kakao 기동, Prometheus scrape 5s 권장(로그인 유입 10초 창 해상도).

### E1. 용량 재측정 (기준선·한계 탐색)

- `run-kakao-user-capacity.sh`(팀원 러너 그대로, 스크립트만 v2)
  단계 `125 150 200 250 300 400`
- 판정(기존과 동일): kind:app p95 ≤ 300ms, 오류 1건이라도 발생 시 해당 단계에서
  중단, 인증 p95 ≤ 1초, 전원 100콜 완주 + kind:search p95 ≤ 1.5초
- 해석 주의: 시딩 때문에 v1의 2ms대 p95와 비교 불가. 관심사는 두 버전의 차이와
  최초 실패 단계

### E2. worker 장애 격리 (A 증명)

- `run-worker-outage-drill.sh` — 200명 부하 60초 시점에 worker를 60초 `docker pause`
  (stop이 아닌 pause: TCP를 매달아 읽기 타임아웃 3초 최악 경로 재현)
- 판정: pause 창 동안 similar 제외 API(kind:app) 오류 0 + p95 300ms 유지
- 기대: 개선 전 — Hikari pending↑·획득 실패>0·무관 API 오류 전파 /
  개선 후 — similar만 3초 후 빈 목록(200), 나머지 무영향

### E3. 업로드×검색 동시 (B 증명)

- `run-search-under-pipeline.sh` — pipeline-load 25 jobs/s 배경 + 검색 5회/초 90초
- 판정: 검색 실행 창의 semantic p95와 504(AI_SEARCH_TIMEOUT) 건수
- 기대: 개선 전 — 검색 p95 수 초·504 발생 가능 / 개선 후 — 수백 ms·504 0건.
  보너스로 파이프라인 한계 jobs/s(기존 35)도 재측정 가치

## 7. 서버 실행 순서

```bash
# ① 준비 (팀원 기존 절차 재사용)
docker compose -f docker-compose.mock-kakao.yml up -d          # mock Kakao
# worker → fastapi + mock-ai 전환 (ops/mock-ai/deploy-remote.sh 절차, 최신 server.py 반영)
# Prometheus scrape 5s로 임시 하향 후 reload

# ② 시딩 (1회, ~30분)
RESULT_DIR=/home/ubuntu/k6/results/seed-20260804 \
  /home/ubuntu/k6/run-kakao-user-seed.sh

# ③ before: 현재 135 이미지 상태에서
LEVELS="125 150 200 250 300 400" USER_OFFSET=3000000 \
  RESULT_DIR=/home/ubuntu/k6/results/e1-before /home/ubuntu/k6/run-kakao-user-capacity.sh
RESULT_DIR=/home/ubuntu/k6/results/e2-before /home/ubuntu/k6/run-worker-outage-drill.sh
RESULT_DIR=/home/ubuntu/k6/results/e3-before /home/ubuntu/k6/run-search-under-pipeline.sh

# ④ 2차 이미지 빌드·임시 기동 (start-current-api-test.sh 방식) + worker도 2차 이미지로
# ⑤ after: ③과 동일 명령, RESULT_DIR만 e1/e2/e3-after
# ⑥ 복구: 원본 컨테이너·Kakao 설정·scrape 원복, mock Kakao 정리
```

주의: E2/E3의 worker 컨테이너 이름(`WORKER_CONTAINER`), 네트워크, 결과 경로는
환경변수로 조정한다. B는 worker 변경이므로 **after 단계에서 worker 이미지도 2차로
교체**해야 한다(api만 바꾸면 E3가 before와 같게 나온다).

## 8. 측정 데이터 수집·정리 방법

모든 러너가 `manifest.tsv`(단계·epoch·ISO 시각)와 k6 `--summary-export` JSON을
남긴다. 측정 후 정리는 다음 순서로 한다(로컬에서 Claude가 수행하는 절차):

1. ssh 터널로 서버 Prometheus와 결과 디렉터리를 연다
   ```bash
   ssh -i I15D207T.pem -L 19090:localhost:9090 ubuntu@i15d207.p.ssafy.io
   ```
2. `manifest.tsv`의 구간별로 Prometheus API를 조회해 지표를 뽑는다. 실험별 핵심:
   - E1: `histogram_quantile(0.95, …http_server_requests_seconds_bucket…)` (uri별),
     `hikaricp_connections_pending/timeout_total`, 오류율(status 5xx)
   - E2: pause 창의 `max_over_time(hikaricp_connections_pending[창])`,
     `increase(hikaricp_connections_timeout_total[창])`, kind:app 대응 uri의 p95·5xx
   - E3: 검색 창의 semantic uri p95, `modera_analysis_pipeline_duration_seconds` p95,
     stream lag(`image-analysis`)
3. k6 JSON에서 kind별 p95·실패율·완주 수를 추출해 아래 결과 표를 채운다
4. Grafana는 같은 구간의 스크린샷 증거용으로만 쓴다(수치는 API 추출값 기준)

## 9. 결과 기록 (측정 후 채움)

### E1 단계별

| 버전 | 사용자 | 앱 p95 | 검색 p95 | 오류 | 완주 | 판정 |
|---|---:|---:|---:|---:|---:|---|
| 135 (before) | | | | | | |
| 2차 (after) | | | | | | |

### E2 pause 창

| 버전 | kind:app 오류 | kind:app p95 | Hikari pending 최대 | 획득 실패 | 판정 |
|---|---:|---:|---:|---:|---|

### E3 검색 창

| 버전 | 검색 p95 | 504 건수 | 파이프라인 p95 | 판정 |
|---|---:|---:|---:|---|

## 10. 해석 시 주의사항

- v2 흐름 7개 기준이므로 v1(5개) 결과와 절대치 비교 금지
- E1의 p95 상승은 시딩(실데이터 쿼리 비용)의 효과가 지배적 — 버전 간 차이로만 해석
- 검색 트래픽은 mock-ai 스왑 상태에서만 실행(실 AI로 흘리면 토큰 소모)
- 시딩은 1회만, before/after가 같은 데이터를 공유해야 A/B가 성립
