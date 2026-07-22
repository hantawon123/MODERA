# modera-backend

MODERA 백엔드. `api-server`와 `analysis-worker` 2개의 독립 Spring Boot 서버로 구성된
SOA(서비스 지향 아키텍처) 구조다. 두 서버는 Redis Streams 이벤트로만 통신하고,
각자 자신이 소유한 PostgreSQL 데이터베이스만 접근한다.

> **이 저장소가 처음이라면** 아래 개요보다 [SETUP.md](./SETUP.md)를 먼저 보는 걸
> 추천한다 — 사전 설치부터 전체 스택 기동, curl로 직접 확인까지 순서대로 되어 있다.

## 구조

```
backend/
├─ settings.gradle, build.gradle, gradle.properties   # 루트(멀티모듈 공통 설정)
├─ api-server/            # 회원·인증, 이미지 등록, 보관함, 조회·검색, 이벤트 발행·구독
├─ analysis-worker/       # 분석 이벤트 소비 → AI 분석 → 결과 저장 → 결과 이벤트 발행
├─ event-contract/        # 두 서버가 공유하는 유일한 모듈(이벤트 DTO·상수). Entity/Repository/Service는 공유 금지
└─ local-infra/           # 로컬 개발용 docker-compose 스택
```

## 로컬 기동 순서

### 1. 인프라만 띄우고 IDE에서 앱 실행 (평소 개발)

```bash
cd local-infra
docker compose up -d              # api-db, analysis-db, redis, minio 4개만 기동
```

각 앱은 IDE 또는 CLI에서 `local` 프로필로 직접 실행한다(JWT_SECRET은 fallback이 없어 필수):

```bash
JWT_SECRET=$(openssl rand -base64 32) \
  ./gradlew :api-server:bootRun --args='--spring.profiles.active=local'

./gradlew :analysis-worker:bootRun --args='--spring.profiles.active=local'
```

### 2. 앱까지 전부 컨테이너로 (통합 확인용)

jar는 gradle이 먼저 만들어야 한다(외부 빌드 방식 Dockerfile이라 컨테이너 안에서 빌드하지 않는다):

```bash
./gradlew bootJar
cd local-infra
docker compose --profile app up -d --build   # 인프라 4개 + api-server + analysis-worker = 6개
```

코드를 바꾼 뒤에는 `./gradlew bootJar`로 다시 jar를 만들고 `--build`로 재기동해야 반영된다.

### 인프라만 내리기 / 전부 내리기

```bash
docker compose down                 # 인프라만 정지 (볼륨은 유지)
docker compose --profile app down   # 앱 포함 전부 정지
```

## 프로필

| 프로필 | 대상 | DB/Redis/MinIO 호스트 | 용도 |
|---|---|---|---|
| `local` | 두 앱 공통 | `localhost` (호스트에서 직접 실행) | IDE/CLI로 직접 실행하는 평소 개발 |
| `docker` | 두 앱 공통 | `api-db`/`analysis-db`/`redis`/`minio` (컨테이너 네트워크) | `docker compose --profile app up`으로 앱까지 컨테이너로 띄울 때 |
| `prod` | 두 앱 공통 | 전부 환경변수로 주입, fallback 없음 | 운영 배포 (운영 compose/Jenkinsfile은 인프라 담당 영역, 이 repo에서 건드리지 않음) |

`local`/`docker` 모두 값이 없으면 개발용 기본값으로 fallback하지만, `JWT_SECRET`과
(prod의 `MINIO_WEBHOOK_TOKEN`/`AI_SERVER_URL` 등) 비밀값은 fallback이 없어 미주입 시 기동에 실패한다.

이 저장소를 개발 중인 로컬 환경은 다른 프로젝트 컨테이너가 표준 포트(5432, 6379,
9000/9001, 8080/8081)를 이미 쓰고 있어서, `local-infra`의 호스트 포트를 다음과 같이
옮겨뒀다(컨테이너 내부 포트는 전부 표준값 그대로):

| 서비스 | 컨테이너 내부 포트 | 이 환경의 호스트 포트 |
|---|---|---|
| api-db | 5432 | 5433 |
| analysis-db | 5432 | 5434 |
| redis | 6379 | 6380 |
| minio API | 9000 | 9002 |
| minio 콘솔 | 9001 | 19001 |
| api-server (app profile) | 8080 | 8090 |

**이 포트 목록은 이 저장소나 프로젝트의 표준이 아니라 이 개발 PC 하나의 사정이다.**
다른 팀원 PC에 표준 포트(5432, 6379, 9000/9001, 8080)가 비어 있다면 그대로 표준값을
써도 되고, 실제로 그게 더 정상적인 상태다. `local-infra/docker-compose.yml`의 각
서비스 `ports:` 항목은 `호스트포트:컨테이너포트` 형식이므로, 콜론 왼쪽(호스트포트)만
자기 PC 사정에 맞게 바꾸면 된다 — 오른쪽(컨테이너포트)은 절대 건드리지 말 것
(애플리케이션 yml의 컨테이너 내부 기본값과 어긋나면 `docker` 프로필이 깨진다).

## 이벤트 흐름

```
클라이언트                 api-server                    analysis-worker
    │                          │                                │
    │  POST /api/v1/images     │                                │
    ├─────────────────────────>│ image_asset + user_image 저장  │
    │  presigned PUT URL       │ (clientRequestId 멱등 처리)     │
    │<─────────────────────────┤                                │
    │                          │                                │
    │  PUT (binary)            │                                │
    ├───────────────────────────────> MinIO ──────────┐         │
    │                          │                       │         │
    │                          │  ObjectCreated webhook│         │
    │                          │<──────────────────────┘         │
    │                          │ image_asset.upload_status       │
    │                          │   = UPLOADED                    │
    │                          │                                │
    │                          │  XADD image-analysis            │
    │                          │  (IMAGE_UPLOADED)                │
    │                          ├───────────────────────────────>│
    │                          │                                │ XREADGROUP(analysis-workers)
    │                          │                                │ analysis_job PENDING→PROCESSING
    │                          │                                │ AnalysisClient(mock|fastapi) 호출
    │                          │                                │ analysis_result 저장
    │                          │                                │  (UNIQUE(image_id, model_version))
    │                          │                                │ analysis_job COMPLETED
    │                          │  XADD analysis-result           │
    │                          │  (ANALYSIS_COMPLETED)           │
    │                          │<───────────────────────────────┤
    │                          │ XREADGROUP(api-consumers)       │
    │                          │ user_image.analysis_status 갱신 │
    │                          │ user_image_view upsert          │
    │                          │ image_search_document upsert    │
```

- 두 스트림 다 Consumer Group + XACK을 쓰는 at-least-once 전달이다. api-server 쪽은
  eventId 기준 Redis SET(`modera:processed-events:analysis-result`)으로 중복 처리를
  막고, analysis-worker 쪽은 `analysis_result.UNIQUE(image_id, model_version)`로
  같은 조합의 중복 저장을 막는다(worker는 job은 재시도마다 새로 남긴다).
- `query_schema.user_image_view`/`image_search_document`는 원본이 아니라 이벤트를
  합친 read model이다. 유실되면 이벤트를 재생하거나 원본(`library_schema`,
  `image_schema`, 이벤트 payload)에서 재구축해야 한다.
- `embedding`(vector(768))은 이벤트 계약에 없어 `image_search_document`에는 채워지지
  않는다. analysis-worker의 `analysis_result.embedding`에만 존재한다.

## 환경변수

| 변수 | 필요 서버 | 설명 | fallback |
|---|---|---|---|
| `JWT_SECRET` | api-server | JWT 서명 키 | 없음 (전 프로필 필수) |
| `DB_HOST` / `DB_PORT` / `DB_NAME` / `DB_USERNAME` / `DB_PASSWORD` | 둘 다 | PostgreSQL 접속 정보(각자 자기 DB만) | local/docker는 기본값 있음, prod는 없음 |
| `DB_POOL_MAX` / `DB_POOL_MIN` | 둘 다 (prod) | HikariCP 풀 크기 | 20 / 10 |
| `REDIS_HOST` / `REDIS_PORT` / `REDIS_PASSWORD` | 둘 다 | Redis Streams 접속 정보 | local/docker는 기본값 있음 |
| `S3_INTERNAL_ENDPOINT` | api-server | 서버→MinIO 접근·서명용 엔드포인트 | local/docker 기본값 있음 |
| `S3_PUBLIC_ENDPOINT` | api-server | presigned URL에 노출할 공개 엔드포인트(호스트 치환용) | local/docker 기본값 있음 |
| `S3_REGION` / `S3_ACCESS_KEY` / `S3_SECRET_KEY` | api-server | MinIO 자격증명 | local/docker는 minio 기본 계정 |
| `S3_BUCKET_PICTURES` / `S3_BUCKET_THUMBNAILS` | api-server | 버킷 이름 | `pictures` / `thumbnails` |
| `MINIO_WEBHOOK_TOKEN` | api-server | `/internal/storage/events` 인증 토큰(`X-Webhook-Token` 헤더와 비교) | local/docker는 더미값, prod는 없음 |
| `ANALYSIS_CLIENT` | analysis-worker | `mock` \| `fastapi` | local/docker는 `mock` |
| `AI_SERVER_URL` | analysis-worker | `analysis.client=fastapi`일 때 호출할 AI 서버 주소 | 없음(fastapi 선택 시 필수) |
| `SERVER_URL` | api-server (prod) | Swagger 서버 목록에 표시할 배포 도메인 | `https://api.example.com` |

## 개선 TODO

- **webhook 재전송 시 `analysis_job`이 매번 새로 생긴다.** 같은 이미지에 대해 MinIO
  ObjectCreated webhook이 재전송되면(네트워크 재시도 등) `image-analysis` 이벤트가
  다시 발행되고, analysis-worker는 그때마다 새 `analysis_job` 행을 만든다.
  `analysis_result`는 `UNIQUE(image_id, model_version)`로 중복 저장을 막지만
  `analysis_job` 자체에는 그런 방어가 없어, 같은 이미지의 job 이력이 계속 쌓인다.
  당장 기능 문제는 아니지만(각 job이 독립적으로 완료 처리됨) 장기적으로는 정리가
  필요하다 — webhook 쪽에서 이미 `UPLOADED`인 image_asset은 재발행을 skip할지,
  아니면 job 쪽에 자체 dedup(예: image_id + 최근 PENDING/PROCESSING 상태 체크)을
  둘지 결정 필요.

## 하지 않는 것 / 주의

- `library_schema`/`image_schema`/`user_schema`/`query_schema`는 전부 `modera_api`
  하나의 DB 안에 있지만, schema 경계를 넘는 FK나 JPA 연관관계는 만들지 않는다.
  다른 schema 데이터가 필요하면(예: read model 갱신) 애플리케이션 코드에서 각자
  조회해 조합한다.
- api-server는 `modera_analysis`에, analysis-worker는 `modera_api`에 접속하지 않는다.
- Presigned URL은 DB에 저장하지 않는다(`s3_key`만 저장, URL은 요청 시 생성).
- 운영 배포 관련 파일(운영 compose, Jenkinsfile, Nginx)은 이 저장소의 인프라 담당
  영역이라 건드리지 않는다.
