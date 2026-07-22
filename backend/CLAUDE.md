# backend/ 작업 규칙

MODERA 백엔드는 2서버 SOA다. 이 파일은 이 저장소에서 코드를 작성할 때 반드시
지켜야 하는 경계를 정리한다. 사람이 읽는 소개는 [README.md](./README.md) 참고.

## 아키텍처

- **`api-server`**: 회원·인증, 이미지 등록·Presigned URL, 보관함, 조회·검색, 이벤트 발행·구독
- **`analysis-worker`**: Redis Streams 이벤트 소비 → AI 분석 → 결과 저장 → 결과 이벤트 발행
- 두 서버는 **Redis Streams 이벤트로만** 통신한다. 직접 호출(HTTP 등)하지 않는다.
- **공유 가능한 건 `event-contract` 모듈 하나뿐**이다(이벤트 DTO·상수, Jackson만 의존).
  **Entity, Repository, Service, Liquibase changeset은 두 서버 사이에 절대 공유하지 않는다.**
  worker에 api-server 코드를 import하거나 그 반대를 하려는 시도는 그 자체로 설계 위반이다.

## DB 배치 원칙

- 로컬(`local-infra`): PostgreSQL 컨테이너 **2개** — `api-db`(database `modera_api`),
  `analysis-db`(database `modera_analysis`)
- 운영: PostgreSQL 컨테이너 1개에 database 2개(로컬과 이름은 같음). 앱 코드는 `DB_HOST`
  환경변수만 다르게 받으므로, 로컬/운영을 가리키는 조건문을 코드에 넣지 않는다.
- `modera_api` 안에는 **schema 4개**: `user_schema`, `image_schema`, `library_schema`,
  `query_schema`. `modera_analysis`는 `public` schema 하나만 쓴다.
- **api-server는 `modera_analysis`에, analysis-worker는 `modera_api`에 절대 접속하지
  않는다.** 커넥션 설정도, JPA 엔티티도 상대 DB를 향하지 않는다.

### schema 경계 규칙 (가장 자주 어기기 쉬운 규칙)

- 같은 schema 내부에서만 실제 FK를 쓴다(예: `library_schema.user_image.category_id
  → library_schema.category`).
- **schema 경계를 넘는 FK·JOIN·JPA 연관관계(`@ManyToOne` 등)는 절대 만들지 않는다.**
  다른 schema/DB가 소유한 값(예: `user_id`, `image_id`)은 그냥 컬럼으로만 저장하는
  **논리 참조**다. JPA에서도 relation이 아니라 `Long`/`UUID` 평범한 필드로 매핑한다
  (`UserImage.categoryId`가 예시 — FK는 있지만 relation 매핑은 안 함).
  다른 schema 데이터가 필요하면 애플리케이션 코드에서 각자 조회해 조합한다
  (`AnalysisResultEventHandler`가 `user_schema`/`image_schema`/`library_schema`를
  각각 별도 리포지토리로 조회해 `query_schema`에 쓰는 방식 참고).
- `query_schema` 내부의 조회(같은 schema 안에서의 SELECT)는 자유롭다.
- `query_schema.*`(`user_image_view`, `image_search_document`)는 **원본이 아니라
  이벤트를 합친 read model**이다. 유실되면 이벤트를 재생하거나 원본에서 재구축할 수
  있어야 한다는 전제를 깨는 변경(예: 여기에만 존재하는 새 데이터를 만드는 것)을
  하지 않는다.
- `structured_fields`/`key_information`(JSONB), `tag_names`(TEXT[]), `embedding`
  (vector)처럼 Hibernate가 표준으로 못 다루는 타입이 있는 테이블은 JPA 엔티티 대신
  `JdbcTemplate` 기반 저장소로 작성한다(`UserImageViewRepository`,
  `ImageSearchDocumentRepository`, `AnalysisResultRepository` 참고).

### Liquibase

- 각 서버가 자기 DB의 Liquibase만 실행한다. 마스터 파일은
  `{module}/src/main/resources/db/changelog/db.changelog-master.yaml`이 `001-init-schema.sql`
  하나를 include하는 구조고, 그 안에 `--liquibase formatted sql` +
  `--changeset {author}:{번호}-{이름}` 헤더로 테이블 단위 changeset이 나열되어 있다.
- **이미 적용된 changeset은 절대 수정하지 않는다**(체크섬이 깨져 다른 환경에서
  기동이 실패한다). 스키마를 바꿔야 하면 새 changeset을 추가한다(기존 파일에
  새 `--changeset` 블록을 이어 붙이거나, `002-xxx.sql`을 새로 만들어 master yaml에
  include를 추가).
- 확장(`vector`, `pg_bigm`)은 DB init 스크립트(`local-infra/{api-db,analysis-db}/init/`)에서
  만든다. Liquibase는 schema·테이블·인덱스만 다룬다.

## 이벤트 계약 (event-contract)

- Stream `image-analysis`(api → worker, group `analysis-workers`),
  Stream `analysis-result`(worker → api, group `api-consumers`)
- envelope 공통 필드: `eventId`(UUID), `eventType`, `version`(int), `occurredAt`(ISO-8601),
  `payloadJson`(payload를 Jackson으로 직렬화한 문자열)
- 계약을 바꾸려면(필드 추가/제거) `event-contract` 모듈의 record를 고치고,
  **두 서버 모두** 다시 빌드해서 반영한다 — 한쪽만 고치고 배포하면 역직렬화가 깨진다.
- 두 스트림 다 Consumer Group + XACK을 쓰는 at-least-once다. 새 컨슈머를 추가할 때는
  중복 수신을 반드시 가정한다(`AnalysisResultConsumer`의 eventId 기준 Redis SET dedup,
  `AnalysisResultRepository`의 `UNIQUE(image_id, model_version)` + `ON CONFLICT DO
  NOTHING` 패턴 참고).

## 응답 규약

`/api/v1/**`의 모든 응답은 `ApiResponse<T>` envelope로 감싼다
(`{result, code, message, data, timestamp}`) — `/internal/**`·actuator·swagger는
예외. 새 `/api/v1/**` 컨트롤러는 클래스에 `@ApiV1Controller`를 붙이고 메서드는
`ApiResponse<T>`를 리턴해야 한다(둘 다 빠뜨리면 그 엔드포인트만 envelope가 깨진다).
실패는 `BusinessException(errorCode)`를 던지면 `GlobalExceptionHandler`가 알아서
변환한다 — 컨트롤러에서 직접 `ApiResponse.fail(...)`을 만들 일은 거의 없다.
자세한 건 [backend-conventions.md](./backend-conventions.md) 3·4절.

## 인증 경계 — 외부는 JWT, 내부는 공유 토큰

- `/api/v1/**`(인증 필요 API): `Authorization: Bearer {accessToken}`,
  `@AuthenticationPrincipal Long userId`로 받는다(역할/권한 개념 없음, principal이
  `Long` 그 자체).
- `/internal/**`(서버 간 내부 API, 예: MinIO webhook): `X-Webhook-Token` 헤더 +
  컨트롤러 자체 비교. JWT 필터가 아예 안 탄다. **이 경계를 섞지 않는다** — 내부
  API에 JWT를 걸거나, 외부 API에 공유 토큰 방식을 쓰지 않는다.
- 자세한 건 [backend-conventions.md](./backend-conventions.md) 6절.

## 이벤트 컨슈머 예외 정책

컨슈머가 레코드 하나를 처리하다 만나는 예외는 두 갈래다: **영구 오류**(envelope/
payload 파싱 실패 — 재시도해도 똑같이 실패) → eventId 로그 + XACK으로 스킵.
**일시 오류**(그 외, DB/Redis 등 인프라 문제로 추정) → eventId 로그만 남기고
XACK 안 함(PEL에 남아 재전달 대기, 지금은 자동 재처리 배치가 없어 수동 확인 필요).
worker의 "AI 분석 자체 실패 → job FAILED + ANALYSIS_FAILED 발행"은 이 분류와
별개로 기존 정책을 유지한다(도메인 실패라 재시도 대상 아님).
자세한 건 [backend-conventions.md](./backend-conventions.md) 8절.

## 로깅

`X-Request-Id`(api-server, 없으면 자동 생성)와 이벤트 `eventId`(양쪽 컨슈머)를
MDC에 심어서 로그 패턴에 자동으로 붙인다 — 새 코드를 추가해도 이 MDC 값을 직접
조작할 필요는 없고, 컨슈머를 새로 만들 때만 "처리 시작 시 MDC.put, 종료 시
finally에서 MDC.remove" 패턴을 따르면 된다. prod 로그 레벨은
`root: INFO` + `org.hibernate.SQL: ERROR` 고정. **비밀번호·JWT·토큰 해시·
Authorization/X-Webhook-Token 헤더 값을 통째로 로그에 남기지 않는다** — userId,
deviceId, eventId 같은 식별자만 남긴다. 자세한 건
[backend-conventions.md](./backend-conventions.md) 9절.

## 하지 말 것

- schema 경계를 넘는 JPA 연관관계·FK·JOIN 생성
- worker가 `modera_api`에, api-server가 `modera_analysis`에 접속하는 코드
- api-server/analysis-worker 사이에 `event-contract` 이외의 코드 공유(Entity, Repository, Service, Liquibase changeset)
- **비밀값(비밀번호, 시크릿, 토큰, 액세스 키)을 코드나 커밋되는 파일에 하드코딩하는 것.**
  전부 환경변수로 주입한다. 로컬 개발용 더미 값(`localdev` 등)은 `local-infra/` 안에서만
  허용한다(운영 프로필 yml은 fallback 없이 `${VAR}`만 쓴다).
- **비밀번호·JWT·토큰 해시·Authorization/X-Webhook-Token 헤더 값을 `log.*`에 통째로 찍는 것**
  (하드코딩 금지와는 별개 문제 — 코드엔 안 적어도 런타임에 로그로 새면 마찬가지다)
- Presigned URL을 DB에 저장하는 것(`s3_key`만 저장하고 URL은 요청 시 매번 생성)
- 운영 배포 관련 파일(운영 compose, `Jenkinsfile`, Nginx 설정)을 건드리는 것 — 인프라 담당 영역
- 이미 적용된 Liquibase changeset을 수정하는 것(새 changeset을 추가할 것)
- `/api/v1/**` 컨트롤러에 `@ApiV1Controller`를 안 붙이거나 `ApiResponse<T>`가 아닌
  타입을 리턴하는 것(응답 규약 위반)
- `/internal/**`에 JWT를, `/api/v1/**`에 공유 토큰 방식을 쓰는 것(인증 경계 위반)
