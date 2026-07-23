# 백엔드 컨벤션 (신규 도메인 추가 시 팀 규칙)

> 이 문서는 실제 코드(`api-server`/`analysis-worker`의 `domain`·`global` 패키지,
> Liquibase changelog)를 근거로 역추적한 규칙입니다.
> 근거가 없는 항목은 **⚠️ 컨벤션 없음 - 팀 결정 필요**로 표시했습니다.
> SOA 아키텍처 자체의 규칙(schema 경계, 이벤트 계약, DB 배치)은
> [CLAUDE.md](./CLAUDE.md)를 먼저 보세요. 이 문서는 그 아래 레벨의 코드 스타일입니다.
>
> 2026-07 SOA 재구성으로 옛 단일모듈 구조(`CommonResponse`/`ErrorCode`/
> `BusinessException`/`LogAspect`/JWT 필터 등)는 전부 삭제되고 새로 짜는 중입니다.
> 옛 문서를 그대로 믿지 마세요 — 이 문서가 현재 실제 코드 기준입니다.

---

## 1. 모듈 / 패키지 구조

**규칙**: 새 기능은 `domain/{기능명}/{entity,repository,service,controller,dto}` 구조로 만든다.

```
api-server/src/main/java/com/ssafy/modera/api/
├── ApiServerApplication.java
├── domain/
│   ├── user/{entity,repository}          # 예: User(닉네임 조회용 최소 매핑)
│   ├── image/{entity,repository,service,controller,dto}  # 예: 이미지 등록
│   ├── library/{entity,repository}       # 예: UserImage
│   ├── query/repository                  # query_schema 대상 JdbcTemplate 저장소
│   ├── storage/{controller,service,dto}  # MinIO webhook 수신
│   └── event/                            # EventPublisher, 스트림 컨슈머
└── global/config/                        # 도메인 지식 없는 공통 설정(Security, S3, Jackson)

analysis-worker/src/main/java/com/ssafy/modera/worker/
├── AnalysisWorkerApplication.java
├── domain/
│   ├── analysis/{entity,repository,client}  # AnalysisJob, analysis_result 저장소, AnalysisClient
│   └── event/                               # EventPublisher, ImageAnalysisConsumer
└── global/config/
```

**근거**: `api-server/src/main/java/com/ssafy/modera/api/domain/image/`,
`analysis-worker/src/main/java/com/ssafy/modera/worker/domain/analysis/` 등 실제 디렉토리 구조.

옛 문서에 있던 "클래스명이 `*Controller`/`*Service`로 끝나야 자동 로깅된다"는 규칙은
**더 이상 근거가 없습니다** — `LogAspect`가 삭제되어 자동 AOP 로깅이 없습니다.
로그는 각 클래스에서 `@Slf4j` + `log.info(...)`로 직접 남깁니다
(`StorageWebhookService`, `AnalysisResultConsumer` 등 참고).

---

## 2. Entity 규칙

**옛 `BaseTimeEntity`(자동 생성/수정 시각 채우기)는 존재하지 않습니다.** JPA Auditing도
켜져 있지 않습니다. `created_at`/`updated_at`은 각 서비스 코드가 `OffsetDateTime.now()`를
직접 넣습니다(`ImageRegistrationService.createNew()` 참고).

**규칙**:
- 테이블이 `user_schema`/`image_schema`/`library_schema` 등 특정 schema에 있으면
  `@Table(name = "...", schema = "...")`를 반드시 명시한다
  (`ImageAsset`, `UserImage`, `User` 참고).
- 생성자는 `@NoArgsConstructor(access = AccessLevel.PROTECTED)`(JPA 프록시용) +
  필요한 필드만 받는 `@Builder` 생성자 조합을 쓴다. 세터 없이 상태 변경은
  의도가 드러나는 메서드로 한다(예: `ImageAsset.markUploaded(OffsetDateTime)`,
  `UserImage.applyAnalysisStatus(String, OffsetDateTime)`).
- **schema 경계를 넘는 컬럼(다른 schema/DB가 소유한 값)은 relation이 아니라
  평범한 `Long`/`UUID` 필드로 매핑한다.** `UserImage.userId`가 예시 — FK가 없는 논리
  참조라 `@ManyToOne` 대신 그냥 `Long`이다. 같은 schema 내부 FK(`UserImage.categoryId
  → library_schema.category`)도 마찬가지로 relation 매핑 없이 ID만 들고 있는다
  (자세한 이유는 [CLAUDE.md](./CLAUDE.md)의 schema 경계 규칙).
- Hibernate가 표준으로 못 다루는 컬럼 타입(`vector`, `jsonb`, `text[]`, DB의 `CHAR(n)`)이
  있으면:
  - `CHAR(n)`처럼 타입 자체가 어긋나는 경우 `@JdbcTypeCode(SqlTypes.CHAR)`를 붙인다
    (`ImageAsset.contentHash` 참고 — 안 붙이면 `ddl-auto: validate`가 기동 시 실패한다).
  - `vector`/`jsonb`/`text[]`처럼 애초에 JPA 매핑이 마땅치 않은 컬럼이 있는 테이블은
    **JPA 엔티티를 쓰지 않고 `JdbcTemplate` 기반 저장소로 통째로 작성한다**
    (`UserImageViewRepository`, `ImageSearchDocumentRepository`,
    `AnalysisResultRepository` 참고). JSONB는 `org.postgresql.util.PGobject`,
    배열은 `Connection.createArrayOf`, vector는 `?::vector` 캐스팅 + 텍스트 리터럴로
    바인딩한다.

**DB 컬럼 네이밍**: Liquibase changelog(`001-init-schema.sql`) 기준 snake_case,
PK는 `{테이블명 단수}_id`.

---

## 3. 응답 규칙

**`/api/v1/**`의 모든 응답은 `ApiResponse<T>`(`global/response`)로 감싼다.**

```json
{ "result": "SUCCESS|FAIL", "code": "...", "message": "...", "data": {...}, "timestamp": "ISO-8601 UTC" }
```

- 성공: `ApiResponse.success(data)` 또는 `ApiResponse.success(message, data)`
- 실패: `GlobalExceptionHandler`가 `BusinessException`/검증 실패/그 외 예외를 잡아서
  자동으로 `ApiResponse.fail(...)`로 변환한다 — **컨트롤러가 직접 fail을 만들 일은
  거의 없다.** 그냥 `BusinessException(errorCode)`를 던지면 된다(4절).
- `/internal/**`, actuator, swagger에는 이 envelope를 적용하지 않는다(기존 형태 유지).

**적용 범위는 `@ApiV1Controller` 마커 애노테이션으로 잡는다** — `@RestControllerAdvice`가
URL 패턴을 직접 못 봐서 만든 우회다. **새 `/api/v1/**` 컨트롤러를 만들면 클래스에
`@ApiV1Controller`를 반드시 붙인다.** 안 붙이면 그 컨트롤러의 예외가
`GlobalExceptionHandler`를 안 타고 Spring 기본 오류 응답(빈 바디)으로 나간다.

**자동 래핑이 아니라 컨트롤러가 명시적으로 `ApiResponse.success(...)`를 반환한다**
(2026-07, 대상이 API 몇 개뿐인 시점에 `ResponseBodyAdvice` 전역 인터셉터는 과하다고
판단). 그래서 **모든 `/api/v1/**` 컨트롤러 메서드는 반드시 `ApiResponse<T>`를 리턴
타입으로 써야 한다** — 빠뜨리면 컴파일은 되지만 그 엔드포인트만 envelope 없이 나가서
클라이언트 파싱이 깨진다. 자동 강제 장치가 없으니 PR 리뷰(10절 체크리스트)로 잡는다.

**목록/페이지네이션**: `PageResponse<T>`(`global/response`, Spring Data `Page`에서
변환하는 `from(Page<T>)` 포함)가 이미 있다. 아직 실제로 쓰는 목록 API는 없다
(`ImageDetailResponse`는 단건 조회).

**유지되는 규칙**: Entity를 그대로 컨트롤러 응답으로 반환하지 않는다. 응답 전용
DTO(`XxxResponse`, record)로 변환해서 내려준다 — 이유는 옛 문서와 동일
(지연 로딩 프록시 직렬화 문제, DB 스키마 변경이 API 스펙에 새는 문제).

---

## 4. 에러 처리

`/api/v1/**`은 `ErrorCode`(인터페이스) + `BusinessException` + `GlobalExceptionHandler`
(`@RestControllerAdvice(annotations = ApiV1Controller.class)`)로 처리한다.
`BusinessException(errorCode, message?, detail?)`을 던지면 해당 `ErrorCode.getStatus()`
+ `ApiResponse.fail(...)` envelope로 자동 변환된다. `MethodArgumentNotValidException`은
400 + `data`에 `[{"field","message"}]` 배열로, 그 외 처리되지 않은 예외는 500
`INTERNAL_ERROR`로(스택트레이스는 로그에만) 변환된다.

**`ErrorCode`는 인터페이스, 실제 코드는 소유 도메인의 enum에 둔다**(2026-07 개정 —
처음엔 단일 flat enum으로 만들었다가, 도메인이 늘어날 걸 감안해 옛 프로젝트에 있던
인터페이스+도메인별 enum 구조로 전환했다):

- `GlobalErrorCode`(`global/exception`) — 어느 도메인에도 속하지 않는 공통/인프라
  오류만(`INVALID_PARAMETER`, `UNAUTHORIZED`, `FORBIDDEN`, `NOT_FOUND`, `CONFLICT`,
  `INTERNAL_ERROR`).
- 도메인 비즈니스 오류는 그 도메인 패키지 밑 `exception/`에 자기 enum을 만든다
  (예: `domain/user/exception/UserErrorCode`, `domain/image/exception/ImageErrorCode`).
  **다른 도메인의 enum에 코드를 얹지 않는다.**
- **`code` 문자열은 전체 enum을 통틀어 유일해야 한다.** enum이 분리되어 있어
  컴파일러가 중복을 못 잡아준다 — 새 코드를 추가하기 전에 관련 enum들을 직접 훑어서
  겹치지 않는지 확인할 것(옛 문서에도 있던, `ErrorCode` 코드값 중복 사고의 재발 방지
  규칙).
- 지금 `code` 값은 `DUPLICATE_LOGIN_ID`처럼 의미가 드러나는 이름 그대로다. 옛
  프로젝트의 `U001`류 prefix+번호 레지스트리(도메인별 prefix를 미리 할당하고 순번을
  영구 결번 처리하는 체계)는 **이번 semantic 이름 체계에서는 승계하지 않았다** —
  prefix 복귀 여부는 안드로이드와의 API 명세 회의에서 별도로 정한다. 그때 가서
  바뀌어도 각 enum 상수의 `code` 문자열 값만 고치면 되고, 인터페이스 계약이나
  호출부(`ApiResponse.fail`, `GlobalExceptionHandler`, `BusinessException`)는 전혀
  건드릴 필요가 없는 구조다.

이벤트 컨슈머의 예외 처리(HTTP 요청이 아니라 Redis Streams 메시지 처리 중 예외)는
전혀 다른 정책을 쓴다 — [8절](#8-이벤트-컨슈머-예외-처리) 참고.

---

## 5. DB 규칙 (Liquibase)

**핵심 규칙은 그대로다: 테이블을 손으로 만들거나 수정하지 않는다.** 모든 스키마
변경은 Liquibase changelog로만 반영한다(`ddl-auto: validate` — Hibernate는 검증만).

**파일 구조가 옛 문서와 다르다**:
- 옛: `db.changelog-master.yaml`이 `includeAll`로 날짜 폴더(`YYYYMMDD/`) 전체를 스캔.
- 새: 각 모듈(`api-server`, `analysis-worker`)의
  `src/main/resources/db/changelog/db.changelog-master.yaml`이
  `001-init-schema.sql` **파일 하나**를 `include`하는 구조. 그 파일 안에
  `--liquibase formatted sql` 헤더 + 테이블 단위 `--changeset {author}:{번호}-{이름}` +
  `--rollback`이 나열된다.
- 두 모듈의 changelog는 완전히 분리되어 있고 서로의 DB를 모른다.

**새 changeset을 추가할 때**:
- **이미 적용된 changeset은 절대 수정하지 않는다**(체크섬이 어긋나 다른 환경에서
  기동이 실패한다).
- 기존 `001-init-schema.sql`에 새 `--changeset` 블록을 이어 붙이거나, 새 파일
  (`002-xxx.sql`)을 만들어 `db.changelog-master.yaml`에 `include`를 추가한다.
- changeset id는 그 모듈 안에서 유일해야 한다. author는 지금까지 `modera-api`
  (api-server), `analysis-worker`(analysis-worker)를 써왔다.
- 확장(`vector`, `pg_bigm`)은 Liquibase가 아니라 `local-infra/{api-db,analysis-db}/init/`의
  DB init 스크립트에서 만든다.

---

## 6. 인증 규칙

**2026-07 구현 완료**(`global/security/`, `domain/user/`). 옛 `PrincipalDetails`/
`AuthUser`/역할(role) 개념은 없다 — 이 서비스는 역할 구분이 없어서 principal을
**`Long userId` 그 자체**로 단순화했다.

**외부 = JWT, 내부 = 공유 토큰 — 절대 섞지 않는다**:
- `/api/v1/**`(로그아웃 등 인증 필요 API)는 `Authorization: Bearer {accessToken}`.
- `/internal/**`(MinIO webhook 수신)는 `X-Webhook-Token` 헤더 + 컨트롤러 자체 문자열
  비교. `JwtAuthenticationFilter.shouldNotFilter()`가 `/internal/` 접두어를 아예
  건너뛰어서 이 필터를 타지도 않는다. 새 내부 전용 API를 추가해도 JWT를 걸지 말고
  이 패턴을 따를 것.

**컨트롤러에서 로그인 사용자를 받는 표준 패턴**:
```java
@PostMapping
public ResponseEntity<ApiResponse<X>> foo(@AuthenticationPrincipal Long userId, ...) { ... }
```
`JwtAuthenticationFilter`가 유효한 토큰이면
`new UsernamePasswordAuthenticationToken(userId, null, authorities)`를
`SecurityContext`에 넣어서 `getPrincipal()`이 `Long`을 바로 돌려주기 때문에 별도
UserDetails 래퍼가 필요 없다.

**JwtAuthenticationFilter는 `@Component`가 아니다.** `SecurityConfig.
securityFilterChain()`에서 `new JwtAuthenticationFilter(jwtTokenProvider)`로 직접
만들어 `addFilterBefore(...)`로 끼워 넣는다 — `@Component`로 등록하면 Spring Boot가
필터 빈을 모든 요청에 자동으로 한 번 더 걸어버려서(FilterRegistrationBean 자동구성)
Security 체인의 `addFilterBefore`와 이중 실행될 수 있다.

**401/403도 envelope로 나가야 한다.** Spring Security 필터 체인은
`@RestControllerAdvice`(`GlobalExceptionHandler`)보다 앞단이라 그게 못 잡는다 —
`JsonAuthenticationEntryPoint`(401)/`JsonAccessDeniedHandler`(403)를 따로 만들어
`SecurityConfig`의 `exceptionHandling(...)`에 등록해뒀다. 새로 Security 관련 코드를
만질 때 이 둘을 빠뜨리면 401/403이 빈 바디로 나가는 회귀가 생긴다.

**JWT/비밀번호**:
- `JwtTokenProvider`(`global/security/jwt`)가 발급·검증을 모두 담당(JJWT 0.13
  fluent API, `Jwts.builder()...signWith(key)`/`Jwts.parser()...parseSignedClaims(...)`).
  `jwt.secret`은 반드시 **base64**여야 한다(`Decoders.BASE64.decode`로 키를 만든다 —
  일반 문자열을 넣으면 기동이 실패한다).
  accessToken 30분 / refreshToken 14일(`application.yml`의 `jwt.*`, 옛 문서와 동일 값).
- refreshToken도 JWT다(jti 포함, 128비트+ 엔트로피). **원문은 DB에 저장하지 않고
  SHA-256 해시(64자 hex)만 저장한다**(`AuthService.hash()`, `RefreshToken.tokenHash`).
  재발급(RTR)마다 해시를 교체하므로, 회전 전 옛 refreshToken을 다시 써도 이제 어떤
  DB 행과도 해시가 안 맞아서 자연히 막힌다 — 별도의 "폐기된 토큰" 테이블이 없다.
- 비밀번호는 `PasswordEncoderConfig`의 `DelegatingPasswordEncoder`로 인코딩한다.
  저장값 앞에 `{bcrypt}` 접두어가 붙는다 — `users.password_hash`가 bcrypt 해시
  길이(60자)가 아니라 72자인 이유가 이 접두어 때문이다. 나중에 인코딩 방식을 바꿔도
  접두어로 기존 값과 공존할 수 있다.
- 비밀번호·토큰 원문은 어떤 `log.*` 호출에도 넣지 않는다(userId/deviceId 등
  식별자만). PR 리뷰 때마다 새로 추가한 로그 문에 이게 없는지 확인할 것.

**`SecurityConfig.PERMIT_ALL_PATHS`**: `/api/v1/auth/register`·`/login`·`/refresh`만
permitAll이다(토큰이 아직 없는 시점에 부르는 API라서). **`logout`은 Bearer가
필수라 permitAll에 넣지 않았다** — 지시문 원문은 "`/api/v1/auth/**` permitAll"이었지만
logout의 "Bearer 필수" 요구사항과 모순되어 좁혔다(명세 변경점, `SETUP.md`/PR 설명
참고). 새 인증 불필요 API를 추가할 때 이 배열에 정확한 경로만 추가할 것 — 와일드카드로
상위 경로를 통째로 열면 의도치 않게 인증이 필요한 하위 API까지 뚫릴 수 있다.

---

## 7. 이벤트 컨슈머/퍼블리셔 규칙 (신규)

- 이벤트 발행은 각 모듈의 `EventPublisher`(작은 클래스, `event-contract`의
  `EventEnvelope.of(...)`로 감싸서 XADD)로 한다. 두 모듈에 같은 이름의 클래스가
  각각 있다 — **공유 금지 규칙 때문에 의도적으로 중복**이다.
- 이벤트 소비는 `@PostConstruct`에서 Consumer Group을 만들고(이미 있으면
  `BUSYGROUP` 예외를 무시), 별도 스레드에서 `XREADGROUP` 블로킹 루프를 도는
  구조로 통일한다(`AnalysisResultConsumer`, `ImageAnalysisConsumer` 참고).
  - web 서버가 있는 api-server는 가상 스레드(`Thread.ofVirtual()`)를 써도 된다
    (Tomcat이 프로세스를 살려둔다).
  - web 서버가 없는 analysis-worker는 **일반 platform thread**를 써야 한다
    (가상 스레드는 항상 daemon이라 그것만으로는 프로세스가 안 죽지 않는다 —
    `ImageAnalysisConsumer` 상단 주석 참고).
- BUSYGROUP 감지는 `e.getMessage()`가 아니라 **cause 체인을 끝까지 순회**해야 한다.
  Spring Data Redis가 `RedisSystemException`으로 감싸면서 실제 메시지가
  `getCause()`에 들어간다(`isBusyGroup()` 구현 참고 — 처음엔 이 버그로 재기동이 계속
  실패했었다).
- at-least-once 전달을 항상 가정한다. 중복 수신 시 안전하게 만드는 방법은 상황에
  따라 고른다:
  - Redis SET으로 eventId dedup(`AnalysisResultConsumer`) — **TTL을 꼭 걸 것**.
    멤버 단위 TTL이 없으니 SADD할 때마다 키 전체에 EXPIRE를 갱신한다.
  - DB unique 제약 + `ON CONFLICT DO NOTHING`(`AnalysisResultRepository`) — 저장
    자체가 멱등이면 이쪽이 더 간단하다.

---

## 8. 이벤트 컨슈머 예외 처리

컨슈머(`AnalysisResultConsumer`, `ImageAnalysisConsumer`)가 레코드 하나를 처리하다
마주치는 예외는 두 갈래로 나눠서 다르게 반응한다.

**영구 오류(재시도해도 항상 같은 결과)** — envelope 자체가 필드맵에서 못 만들어지거나
(`EventEnvelope.fromFieldMap`), payload JSON이 깨져서 역직렬화가 안 되는 경우.
eventId와 함께 ERROR 로그를 남기고 **XACK해서 스킵**한다 — 큐에 남겨봐야 다음에도
똑같이 실패해서 스트림만 막힌다.

**일시 오류(DB/Redis 등 인프라 문제로 추정)** — 그 외 모든 예외(핸들러가 DB에 쓰다가
실패하는 경우 등). eventId와 함께 ERROR 로그만 남기고 **XACK하지 않는다** — 메시지가
Consumer Group의 PEL(Pending Entries List)에 남아 재전달을 기다린다.
- **TODO**: 지금은 PEL에 쌓인 메시지를 자동으로 재할당·재처리하는 배치가 없다.
  `XAUTOCLAIM`으로 일정 시간 이상 대기 중인 PEL 항목을 주기적으로 걷어 재처리하는
  스케줄러를 추가해야 "재전달을 기다린다"가 실제로 의미를 가진다. 그 전까지는 일시
  오류가 나면 `XPENDING`으로 확인하고 사람이 수동 개입해야 한다.

worker의 `handleImageUploaded` 안에서 **AI 분석 자체의 실패**(`AnalysisClient`가
예외를 던지는 경우)는 위 두 갈래와 별개로, 원래부터 있던 정책을 그대로 유지한다 —
job을 FAILED로 기록하고 `ANALYSIS_FAILED`를 발행한 뒤 "정상 처리됨"으로 취급해
XACK한다. 인프라 오류가 아니라 도메인상 실패라 재시도 대상이 아니기 때문이다.

**구현**: `processRecord()`를 envelope 파싱(실패 시 즉시 스킵) → payload 파싱
(실패 시 `PayloadParseException`으로 감싸 "영구 오류"로 구분) → 핸들러 호출(그 외
예외는 전부 "일시 오류") 3단계로 나눈다. `PayloadParseException`은 두 모듈에 각각
있는 private static 내부 클래스다 — event-contract 밖에서는 코드를 공유하지 않는
규칙 때문에 의도적으로 중복이다.

---

## 9. 로깅 규칙

**MDC로 요청/이벤트를 추적한다.** 로그 패턴(`application.yml`의
`logging.pattern.console`)에 `[reqId=%X{requestId} eventId=%X{eventId}]`가 붙어
있어서, MDC에 심은 값이 그 스코프 안의 **모든** 로그 라인(직접 안 건드린 라이브러리
로그 포함)에 자동으로 찍힌다.

- **requestId**(api-server만, `RequestIdFilter`): `X-Request-Id` 헤더가 있으면 그대로,
  없으면 UUID 앞 8자리를 생성해 MDC에 심고 응답 헤더로도 돌려준다. `@Order
  (HIGHEST_PRECEDENCE)`로 Security 필터 체인 전체를 감싸서 401/403 응답에도 붙는다.
  같은 필터가 액세스 로그(`method path status 소요ms`, actuator 제외)도 남긴다.
- **eventId**(api·worker 양쪽): 컨슈머가 레코드 처리를 시작할 때 `MDC.put`, 끝나면
  (성공/영구오류/일시오류 전부) `finally`에서 `MDC.remove` — 8절의 컨슈머 예외
  처리 3단계 구조에서 두 번째 try 블록을 감싸는 형태로 들어간다. `EventPublisher`는
  발행 시 `eventId`/`eventType`/`stream`을 로그 **메시지**에 남긴다(MDC에는 안 심음
  — 발행은 "처리 스코프"가 아니라서). worker가 하나의 IMAGE_UPLOADED를 처리하며
  ANALYSIS_COMPLETED를 새로 발행하면, 그 발행 로그 줄에는 "MDC의 들어온 eventId"와
  "메시지 속 나간 eventId"가 함께 찍혀서 두 이벤트가 인과관계로 이어진다 — 새
  컨슈머를 만들 때도 이 패턴(들어온 이벤트를 MDC에, 나간 이벤트는 메시지에)을
  유지하면 추적이 끊기지 않는다.
- analysis-worker는 web이 없어 requestId가 항상 빈 문자열이다 — 정상이다.

**로그 레벨**: local은 자유(`com.ssafy.modera: DEBUG` 등). prod는
`root: INFO` + `org.hibernate.SQL: ERROR`(각 모듈 `application-prod.yml`, 4단계부터
고정) — 새 프로필을 만들어도 이 기준을 벗어나지 않는다.

**민감정보를 로그에 남기지 않는다.** 비밀번호, JWT(access/refresh 둘 다), 토큰 해시
원문, `Authorization`/`X-Webhook-Token` 헤더 값을 통째로 찍는 `log.*` 호출을 만들지
않는다 — userId, deviceId, eventId, imageId 같은 식별자만 남긴다. `AuthService`가
`log.info("로그인 성공: userId={}, deviceId={}", ...)`처럼 하는 걸 기준으로 삼을 것.

---

## 10. PR 리뷰 체크리스트

- [ ] Entity를 그대로 응답에 쓰지 않고 응답 DTO로 변환했는가
- [ ] schema 경계를 넘는 FK·JOIN·JPA 연관관계를 만들지 않았는가 ([CLAUDE.md](./CLAUDE.md))
- [ ] api-server가 `modera_analysis`를, analysis-worker가 `modera_api`를 건드리지 않는가
- [ ] `event-contract` 이외의 코드를 두 서버가 공유하지 않는가
- [ ] Liquibase changeset을 새로 추가했다면 이미 적용된 changeset을 수정하지 않았는가,
      author:id가 그 모듈 안에서 유일한가
- [ ] 새 컨슈머를 추가했다면 at-least-once 중복 수신을 가정하고 dedup/멱등 저장 +
      영구/일시 오류 구분(8절)을 넣었는가, TTL이 필요한 캐시성 키에 TTL을 걸었는가
- [ ] 비밀값(시크릿, 토큰, 자격증명)을 하드코딩하지 않고 환경변수로 뺐는가
      (더미값은 `local-infra/` 안에서만 허용)
- [ ] Presigned URL을 DB에 저장하지 않고 `s3_key`만 저장했는가
- [ ] **새 `/api/v1/**` 컨트롤러/메서드가 `@ApiV1Controller` + `ApiResponse<T>` 리턴을
      빠짐없이 쓰는가**(하나라도 빠지면 그 엔드포인트만 envelope 없이 나가거나
      예외가 envelope 없이 나간다 — 3절)
- [ ] 새 에러 코드를 소유 도메인 enum에 추가했는가(`GlobalErrorCode`에 도메인 코드를
      끼워넣지 않았는가), `code` 문자열이 다른 enum과 안 겹치는가(4절)
- [ ] 새 API의 인증 경계를 확인했는가 — `/api/v1/**`면 JWT(permitAll 와일드카드로
      상위 경로를 통째로 열지 않기), `/internal/**`면 공유 토큰 방식을 그대로
      따랐는가(6절)
- [ ] 새로 추가한 `log.*` 호출에 비밀번호·토큰 원문이 안 찍히는가, 이벤트 컨슈머라면
      MDC eventId 패턴을 따랐는가(9절)

**⚠️ 컨벤션 없음 - 팀 결정 필요** (아직 안 정한 것들):
- 테스트 작성 스타일(현재 도메인 테스트 예시 없음, "각 단계 최소 스모크 수준" 정도만
  수동 검증됨)
- Service 계층 인터페이스+구현 분리 여부
- `@Transactional` 부착 위치/전략
- 에러 코드 `U001`류 prefix+번호 레지스트리 복귀 여부(4절 — 안드로이드와의 API
  명세 회의에서 결정 예정)
