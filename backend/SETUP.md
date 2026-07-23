# SETUP — 이 저장소를 처음 받은 사람을 위한 셋업 가이드

이 문서 하나만 보고 이 컴퓨터가 처음이어도 `backend/` 전체(인프라 4개 + api-server +
analysis-worker)를 로컬에서 띄우고, 실제로 회원가입부터 이미지 등록·분석까지 동작하는
걸 확인할 수 있게 만든 가이드다. 아키텍처 설명은 [README.md](./README.md), 코드
작성 규칙은 [CLAUDE.md](./CLAUDE.md)·[backend-conventions.md](./backend-conventions.md)를
참고한다.

---

## 0. 사전 준비물

| 프로그램 | 필요 이유 | 확인 명령 |
|---|---|---|
| **Git** | 저장소 클론 | `git --version` |
| **Docker Desktop** (Compose v2 포함) | PostgreSQL·Redis·MinIO·앱 컨테이너 | `docker --version`, `docker compose version` |
| **JDK 21** | Gradle 빌드/실행 | `java -version` |

- macOS: `brew install --cask docker`, `brew install openjdk@21`
- Windows: [Docker Desktop 설치 파일](https://www.docker.com/products/docker-desktop/) +
  [Temurin JDK 21](https://adoptium.net/) 설치 후 재부팅(WSL2 활성화 필요할 수 있음)
- Linux: 배포판 패키지 매니저로 `docker`, `docker-compose-plugin`, `openjdk-21-jdk` 설치

> **JDK가 21이 아니어도 된다?** 이 프로젝트는 `settings.gradle`에
> `foojay-resolver-convention` 플러그인이 있어서, Gradle이 필요하면 JDK 21을
> 자동으로 내려받아 쓴다(인터넷 연결 필요). 그래도 처음 빌드가 느려지는 걸 피하려면
> JDK 21을 미리 설치해두는 걸 권장한다.

IDE는 자유(IntelliJ, VS Code 등). IntelliJ라면 `backend/` 폴더를 Gradle 프로젝트로
열면 `api-server`/`analysis-worker`/`event-contract` 3개 모듈이 자동으로 잡힌다.

---

## 1. 클론 & 브랜치

```bash
git clone https://lab.ssafy.com/s15-webmobile4-sub1/S15P11D207.git
cd S15P11D207
git checkout feature/soa-setup   # 또는 이 작업이 머지된 이후의 develop/backend
cd backend
```

앞으로 이 가이드의 모든 명령은 `backend/`가 현재 디렉터리라고 가정한다.

---

## 2. 인프라 4개 기동 (PostgreSQL ×2, Redis, MinIO)

```bash
cd local-infra
docker compose up -d
```

첫 실행은 `api-db` 이미지를 직접 빌드한다(pgvector 베이스 + pg_bigm 소스 빌드,
1~2분 걸릴 수 있음). 아래처럼 4개 컨테이너가 전부 `healthy`가 될 때까지 기다린다.

```bash
docker compose ps
```

```
NAME                 STATUS
modera-api-db        Up ... (healthy)
modera-analysis-db   Up ... (healthy)
modera-redis         Up ... (healthy)
modera-minio         Up ... (healthy)
```

### ⚠️ 포트 충돌이 나면

이 프로젝트를 만든 개발 PC에는 이미 다른 프로젝트가 표준 포트(5432, 6379,
9000/9001, 8080)를 쓰고 있어서 `local-infra/docker-compose.yml`의 호스트 포트를
5433/5434/6380/9002/19001/8090으로 옮겨뒀다. **당신 컴퓨터에 그 포트들이 비어
있다면 이 조정은 필요 없다.** `docker compose up -d` 시 `port is already
allocated` 에러가 나면:

1. 어떤 프로세스가 그 포트를 쓰는지 확인: `lsof -i :5433` (macOS/Linux) 등
2. 남의 컨테이너/프로세스면 건드리지 말고, `local-infra/docker-compose.yml`의
   해당 서비스 `ports:` 항목에서 **콜론 왼쪽(호스트 포트)만** 비어있는 다른 값으로
   바꾼다. 오른쪽(컨테이너 내부 포트)은 절대 건드리지 않는다 — 바꾸면 아래
   `docker` 프로필 yml의 기본값과 어긋나서 앱이 컨테이너 모드로 못 뜬다(3번
   방법으로 실행할 때는 상관없음).
3. 자세한 표는 [README.md의 "포트" 절](./README.md#프로필) 참고.

---

## 3. api-server 실행 (로컬에서 직접 — 평소 개발 방식)

### 3-1. JWT_SECRET 준비

`jwt.secret`은 fallback이 없어서 안 넣으면 기동 자체가 실패한다. base64 문자열이어야
한다(하이픈 등 일반 텍스트를 넣으면 `Decoders.BASE64.decode`가 예외를 던진다).

```bash
openssl rand -base64 32
# 예: JST22TJeTXAaCo/L6KKPBDYAbKyxmR7DUxJCcQup770=
```

### 3-2. 실행

`backend/` 루트에서(local-infra는 계속 떠 있는 상태로):

```bash
JWT_SECRET="위에서 만든 값" \
  ./gradlew :api-server:bootRun --args='--spring.profiles.active=local'
```

macOS/Linux는 위 한 줄이면 된다. Windows PowerShell이라면:

```powershell
$env:JWT_SECRET="위에서 만든 값"
./gradlew :api-server:bootRun --args='--spring.profiles.active=local'
```

`application-local.yml`이 나머지 값(DB, Redis, MinIO, MinIO webhook 토큰)은
전부 `local-infra`의 컨테이너 주소로 기본값을 잡아두고 있어서 추가 환경변수 없이도
뜬다. 로그 마지막에 `Started ApiServerApplication`이 보이면 성공.

**로컬 8080 포트가 이미 다른 프로세스(다른 프로젝트 등)에 쓰이고 있다면** 인자를
하나 더 붙여서 임시로 다른 포트를 쓸 수 있다:
`--args='--spring.profiles.active=local --server.port=18080'`

### 확인

```bash
curl http://localhost:8080/actuator/health
```

`{"status":"UP", ...}`이 나오면 정상이다. Swagger UI는
`http://localhost:8080/swagger-ui.html`.

---

## 4. analysis-worker 실행 (별도 터미널)

`backend/` 루트에서:

```bash
./gradlew :analysis-worker:bootRun --args='--spring.profiles.active=local'
```

web 서버가 없는 프로세스라 포트/health 엔드포인트가 없다. 로그에
`Started AnalysisWorkerApplication`이 보이고, 그 뒤로 프로세스가 계속 떠 있으면
(바로 안 죽으면) 정상이다. 기본은 `analysis.client=mock`이라 진짜 AI 서버 없이도
분석 파이프라인이 동작한다.

---

## 5. 전체 시나리오로 직접 확인 (curl)

두 앱 다 뜬 상태에서 아래를 순서대로 실행해본다. `BASE`를 실제 쓰는 api-server
포트로 맞춘다(위에서 포트를 안 바꿨으면 8080).

```bash
BASE=http://localhost:8080

# 1) 회원가입
curl -s -X POST $BASE/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"loginId":"tester01","password":"password123","email":"tester01@example.com","nickname":"테스터"}'

# 2) 로그인 → accessToken 받기
curl -s -X POST $BASE/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"loginId":"tester01","password":"password123","deviceId":"my-laptop"}'
# 응답의 data.accessToken을 아래 TOKEN에 붙여넣는다
TOKEN="위에서 받은 accessToken"

# 3) JWT로 이미지 등록 (presigned URL이 함께 온다)
curl -s -X POST $BASE/api/v1/images \
  -H "Content-Type: application/json" -H "Authorization: Bearer $TOKEN" \
  -d '{"clientRequestId":"setup-test-001","fileName":"test.png","contentType":"image/png","contentHash":"'$(printf 'a%.0s' {1..64})'","fileSize":100}'
```

`result:"SUCCESS"`와 `presignedPutUrl`이 담긴 응답이 오면 회원가입·로그인·인증·
이미지 등록까지 전부 정상 동작하는 것이다. 토큰 없이 3번 요청을 다시 보내면
`401 {"code":"UNAUTHORIZED"}`가 나오는 것도 확인해보면 좋다.

이벤트 파이프라인(webhook→분석→조회모델 반영)까지 눈으로 보고 싶다면
[README.md의 "이벤트 흐름" 절](./README.md#이벤트-흐름)과
`/internal/storage/events`에 샘플 MinIO 이벤트를 보내는 예시를 참고한다
(`MINIO_WEBHOOK_TOKEN`은 local 프로필 기본값이 `local-dev-webhook-token`).

---

## 6. (선택) 앱까지 전부 컨테이너로 띄우기

IDE 없이 통째로 확인하고 싶을 때. jar를 먼저 만들어야 한다(컨테이너 안에서
빌드하지 않는 외부 빌드 방식이라서).

```bash
cd backend
./gradlew bootJar
cd local-infra
JWT_SECRET="위에서 만든 base64 값" docker compose --profile app up -d --build
```

`docker compose ps`로 6개 컨테이너(`api-db`, `analysis-db`, `redis`, `minio`,
`api-server`, `analysis-worker`)가 다 떴는지 확인한다. `api-server`는 호스트
`8090`으로 노출된다(`http://localhost:8090`). `JWT_SECRET`을 안 넣으면
`docker-compose.yml`에 있는 로컬 전용 더미 base64 값으로 대체된다(실제 배포에는
쓰지 말 것).

코드를 고친 뒤에는 `./gradlew bootJar`로 다시 jar를 만들고
`docker compose --profile app up -d --build`를 다시 실행해야 반영된다.

내릴 때:

```bash
docker compose --profile app down   # 앱 포함 전부 정지
# 또는
docker compose down                 # 인프라만 정지(다음에 또 쓸 거면 이쪽)
```

---

## 7. 자주 겪는 문제

| 증상 | 원인 | 해결 |
|---|---|---|
| `docker compose up -d`에서 `port is already allocated` | 이 컴퓨터의 다른 프로그램이 같은 포트를 씀 | 위 "2. 포트 충돌이 나면" 참고 |
| api-server 기동 시 `Illegal base64 character` | `JWT_SECRET`이 base64가 아닌 일반 문자열 | `openssl rand -base64 32`로 다시 생성 |
| api-server 기동 시 `JWT_SECRET`이 비어서 실패 | 환경변수를 안 넣고 그냥 `bootRun`함 | 3-2번처럼 `JWT_SECRET=... ./gradlew ...` 형태로 실행 |
| `Connection refused`로 DB/Redis/MinIO 연결 실패 | `local-infra`가 안 떠 있거나 아직 `healthy`가 아님 | `cd local-infra && docker compose ps`로 확인 후 재시도 |
| 이미지 등록 API가 401 | Authorization 헤더 없음/만료된 토큰 | 로그인 다시 해서 새 accessToken 받기(30분 유효) |
| `docker compose --profile app up`이 옛날 코드로 뜸 | jar를 다시 안 만듦 | `./gradlew bootJar` 먼저 실행 후 `--build`로 재기동 |
| analysis-worker가 기동 후 바로 종료됨(정상이 아님) | 컴파일 에러거나 DB 연결 실패 | 로그 확인. `analysis-db` 컨테이너가 healthy인지, `./gradlew :analysis-worker:compileJava`가 성공하는지 확인 |

더 심층적인 인프라/아키텍처 배경은 [README.md](./README.md)를 참고한다.
