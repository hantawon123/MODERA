# modera

Spring Boot 백엔드 프로젝트를 빠르게 시작하기 위한 **보일러플레이트 템플릿**입니다.
JWT 인증, 예외 처리, 로깅, DB 마이그레이션, 관측(Observability), CI/CD가 미리 세팅되어 있어
새 도메인 코드만 얹으면 바로 개발을 시작할 수 있습니다.

> **처음 오셨나요?** 아래 [빠른 시작](#-빠른-시작-5분)만 따라 하면 앱이 뜹니다.
> Spring Boot가 처음이라면 [`onboarding-prompt.md`](./onboarding-prompt.md)를 Claude Code에 붙여넣어
> 이 프로젝트 컨벤션을 손으로 익히는 실습을 진행할 수 있습니다.

---

## 🧱 기술 스택

| 구분 | 사용 기술 |
|------|-----------|
| Language / Runtime | **Java 21** (Amazon Corretto), Virtual Threads(Project Loom) 활성화 |
| Framework | **Spring Boot 4.0.2** (Web MVC, Security, Data JPA, Validation, Actuator) |
| DB | **MySQL 8.0**, JPA/Hibernate, **QueryDSL 7.1** |
| Migration | **Liquibase** (파일명 규칙 기반 자동 include) |
| Cache | **Redis 7.2** |
| Auth | **JWT** (JJWT 0.13, Access + Refresh 토큰, Refresh는 쿠키) |
| API Docs | **Swagger / springdoc-openapi 3.0** |
| Build | Gradle (Wrapper 포함) |
| Infra | Docker / docker-compose, GitHub Actions (CI/CD) |

버전은 [`gradle.properties`](./gradle.properties)에서 한 곳으로 관리합니다.

---

## 📁 프로젝트 구조

```
src/main/java/com/ssafy/modera
├── ModeraApplication.java   # 진입점
├── domain/                              # 도메인 코드 (비즈니스 로직)
│   └── user/                            # 예시 도메인: 로그인 API
│       ├── AuthController.java
│       ├── AuthService.java
│       ├── User.java  /  UserRepository.java
│       └── dto/
└── global/                              # 공통 인프라 (도메인 지식 X)
    ├── config/                          # Security, CORS, Swagger, JPA Auditing 등
    ├── domain/                          # CommonResponse, BaseTimeEntity, ErrorCode ...
    ├── exception/                       # GlobalExceptionAdvice, BusinessException
    ├── security/                        # JWT 필터·토큰·핸들러
    └── log/                             # LogAspect, MDC 로깅 필터

src/main/resources
├── application.yml            # 공통 설정 (default profile = local)
├── application-local.yml      # 로컬 개발용
├── application-prod.yml       # 운영용 (환경변수 주입 필수)
└── db/changelog/              # Liquibase 마이그레이션 SQL

template-infra/               # 로컬 인프라 (MySQL + Redis) docker-compose
```

- **`global`** = 공통 인프라 전용. 도메인 지식이 들어가면 안 됩니다.
- **`domain/{name}`** = 새 도메인은 여기에 형제 패키지로 추가합니다.
- 자동 로깅(`LogAspect`)이 걸리려면 클래스 접미사(`*Controller`, `*Service`) 규칙을 지켜야 합니다.

> 도메인 추가 시 규칙은 [`backend-conventions.md`](./backend-conventions.md)에 정리되어 있습니다.

---

## ✅ 사전 준비물

| 필요 | 설명 |
|------|------|
| **JDK 21** | Corretto 21 권장. 없으면 Gradle toolchain이 자동으로 받아옵니다(foojay resolver). |
| **Docker / Docker Compose** | 로컬 MySQL·Redis를 컨테이너로 띄우는 데 사용 |
| Git | 형상 관리 |

IDE는 IntelliJ IDEA 기준으로 세팅되어 있습니다. Lombok / Annotation Processing 활성화가 필요합니다.

---

## 🚀 빠른 시작 (5분)

### 1) 로컬 인프라(MySQL + Redis) 띄우기

```bash
cd template-infra
cp .env.example .env        # 필요 시 계정/비밀번호 수정
docker compose up -d
```

- MySQL: `localhost:3308` (컨테이너 내부 3306 → 호스트 3308 매핑)
- Redis: `localhost:6379`
- 기본 DB/계정: `template_db` / `user` / `user1234` (`.env`에서 변경 가능)

> `.env` 없이도 기본값으로 동작하도록 되어 있지만, 값을 바꿨다면
> `application-local.yml`의 접속 정보와 일치하는지 확인하세요.

### 2) 애플리케이션 실행

프로젝트 루트에서:

```bash
# Windows (PowerShell)
.\gradlew.bat bootRun

# macOS / Linux
./gradlew bootRun
```

- 기본 프로필은 `local`이라 별도 지정 없이 실행됩니다.
- DB 스키마는 Liquibase가 기동 시 자동으로 마이그레이션합니다
  (`ddl-auto: validate` — Hibernate는 스키마를 만들지 않고 검증만 함).
- 서버 포트: **8080**

### 3) 동작 확인

| 항목 | URL |
|------|-----|
| Health check | http://localhost:8080/actuator/health |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| 예시 로그인 API | `POST http://localhost:8080/api/auth/login` |

로그인 요청 예시:

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"password"}'
```

응답은 공통 포맷 `CommonResponse`로 감싸져 나오고, Access Token은 body,
Refresh Token은 `Set-Cookie`로 내려옵니다.

---

## ⚙️ 환경 설정 (프로필)

| 프로필 | 파일 | 용도 |
|--------|------|------|
| `local` | `application-local.yml` | 로컬 개발 (기본값). SQL 포맷 출력, DEBUG 로깅 |
| `prod` | `application-prod.yml` | 운영. 모든 민감정보를 **환경변수로 주입** |

운영(`prod`)에서 반드시 주입해야 하는 환경변수:

```
JWT_SECRET       # 실제 비밀키 (로컬 더미키 사용 금지)
DB_HOST / DB_PORT / MYSQL_DATABASE / MYSQL_USER / MYSQL_PASSWORD
REDIS_HOST / REDIS_PORT / REDIS_PASSWORD
SERVER_URL       # Swagger에 표시될 배포 도메인
```

프로필을 바꿔 실행하려면:

```bash
./gradlew bootRun --args='--spring.profiles.active=prod'
```

---

## 🧪 테스트 & 빌드

```bash
./gradlew test          # 테스트
./gradlew build         # 테스트 + 빌드
./gradlew bootJar       # 실행 가능한 jar 생성 → build/libs/*.jar
```

---

## 🐳 Docker로 실행

멀티스테이지 [`Dockerfile`](./Dockerfile)로 이미지를 빌드합니다 (Corretto 21 기반).

```bash
docker build -t modera .
docker run -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e JWT_SECRET=... -e DB_HOST=... -e MYSQL_USER=... \
  modera
```

---

## 🔁 CI/CD (GitHub Actions)

| 워크플로 | 트리거 | 하는 일 |
|----------|--------|---------|
| [`ci.yml`](./.github/workflows/ci.yml) | `develop`/`main` PR·push | JDK 21 셋업 → 테스트 → 리포트 업로드 |
| [`cd.yml`](./.github/workflows/cd.yml) | `main` push | Docker 빌드 → **Trivy 취약점 스캔** → Docker Hub push → Discord 알림 |

CD를 쓰려면 리포지토리 Secrets에 `DOCKER_USERNAME`, `DOCKER_PASSWORD`,
`DISCORD_WEBHOOK`를 설정하고, `cd.yml`의 이미지 이름(`my-org/my-app`)을 실제 값으로 바꾸세요.

---

## 🗄️ DB 마이그레이션 (Liquibase)

- 마스터 changelog: `src/main/resources/db/changelog/db.changelog-master.yaml`
- `db/changelog/` 하위 파일을 **파일명 규칙**에 따라 자동으로 포함합니다
  (`LiquibaseIncludeAllFilter.java`가 강제).
- 새 테이블 추가 시: `db/changelog/YYYYMMDD/NN_설명.sql` 형식으로 파일을 추가하면 됩니다
  (예: `db/changelog/20260707/01_create_table_users.sql`).

자세한 컨벤션은 [`backend-conventions.md`](./backend-conventions.md) 참고.

---

## 📚 함께 보면 좋은 문서

- [`backend-conventions.md`](./backend-conventions.md) — 새 도메인 추가 시 팀 규칙(패키지·엔티티·응답·예외 등)
- [`onboarding-prompt.md`](./onboarding-prompt.md) — Spring Boot 입문자용 단계별 실습 프롬프트 (Claude Code에 붙여넣어 사용)
