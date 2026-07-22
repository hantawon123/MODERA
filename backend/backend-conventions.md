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

**⚠️ 컨벤션 없음 - 팀 결정 필요**: 옛 `CommonResponse<T>`/`PageResponse`/`SliceResponse`
래퍼는 삭제되었고 아직 다시 만들지 않았습니다. 지금 있는 API(`POST /api/v1/images`,
`POST /internal/storage/events`)는 `ResponseEntity<T>`에 응답 전용 record를 그대로
담아 반환합니다(`ImageController`, `ImageRegisterResponse` 참고). 목록/검색 API가
생기면 이때 페이지네이션 응답 형태(`PageResponse` 부활, 아니면 다른 방식)를
팀이 정해야 합니다.

**유지되는 규칙**: Entity를 그대로 컨트롤러 응답으로 반환하지 않는다. 응답 전용
DTO(`XxxResponse`, record)로 변환해서 내려준다 — 이유는 옛 문서와 동일
(지연 로딩 프록시 직렬화 문제, DB 스키마 변경이 API 스펙에 새는 문제).

---

## 4. 에러 처리

**⚠️ 컨벤션 없음 - 팀 결정 필요**: 옛 `ErrorCode`/`BusinessException`/
`GlobalExceptionAdvice`(전역 예외 처리 + 코드값 체계)는 전부 삭제되었고 아직
다시 만들지 않았습니다. 지금 코드는 상황에 맞는 표준 예외(`IllegalStateException`,
`IllegalArgumentException` 등)를 그때그때 던지고 있어(`ImageRegistrationService`,
`AnalysisResultEventHandler` 참고), 처리되지 않은 예외는 Spring 기본 500 응답으로
나갑니다. API가 늘어나기 시작하면 이 부분부터 팀이 정해야 합니다(옛 문서의
`ErrorCode` 접두어 체계, `BusinessException` 단일화, `@RestControllerAdvice` 패턴을
그대로 부활시킬지, 다른 방식을 쓸지).

이벤트 컨슈머(`AnalysisResultConsumer`, `ImageAnalysisConsumer`)는 레코드 단위로
try-catch해서 한 이벤트 처리 실패가 스트림 전체를 막지 않게 한다 — 실패해도 로그만
남기고 `finally`에서 XACK한다(재시도 큐 같은 정교한 처리는 아직 없다는 뜻이기도
하다. 실패 이벤트 재처리는 지금은 수동이다).

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

**⚠️ 미구현**: 옛 `PrincipalDetails`/`JwtAuthenticationFilter`/`@AuthenticationPrincipal`은
전부 삭제되었고 아직 다시 만들지 않았습니다. `POST /api/v1/images`는 지금 `X-User-Id`
헤더로 userId를 임시로 받고 있고(`ImageController` 참고), `SecurityConfig`의
permitAll 목록에 이 경로가 임시로 들어가 있습니다. 코드에 `// TODO: JWT 인증 도입 후...`
주석으로 표시해뒀습니다.

JWT 인증을 다시 만들 때 최소 확인할 것:
- `SecurityConfig.PERMIT_ALL_PATHS`에서 `/api/v1/images` 제거
- `ImageController`의 `X-User-Id` 헤더 파라미터를 `@AuthenticationPrincipal` 등으로 교체
- `/internal/storage/events`는 JWT가 아니라 `X-Webhook-Token` 헤더로 별도 인증하므로
  이 경로는 그대로 permitAll + 컨트롤러 자체 토큰 비교를 유지한다.

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

## 9. PR 리뷰 체크리스트

- [ ] Entity를 그대로 응답에 쓰지 않고 응답 DTO로 변환했는가
- [ ] schema 경계를 넘는 FK·JOIN·JPA 연관관계를 만들지 않았는가 ([CLAUDE.md](./CLAUDE.md))
- [ ] api-server가 `modera_analysis`를, analysis-worker가 `modera_api`를 건드리지 않는가
- [ ] `event-contract` 이외의 코드를 두 서버가 공유하지 않는가
- [ ] Liquibase changeset을 새로 추가했다면 이미 적용된 changeset을 수정하지 않았는가,
      author:id가 그 모듈 안에서 유일한가
- [ ] 새 컨슈머를 추가했다면 at-least-once 중복 수신을 가정하고 dedup/멱등 저장을
      넣었는가, TTL이 필요한 캐시성 키에 TTL을 걸었는가
- [ ] 비밀값(시크릿, 토큰, 자격증명)을 하드코딩하지 않고 환경변수로 뺐는가
      (더미값은 `local-infra/` 안에서만 허용)
- [ ] Presigned URL을 DB에 저장하지 않고 `s3_key`만 저장했는가
- [ ] 새 API의 인증/인가가 필요한지 확인했는가 — 지금은 JWT가 없어 임시 우회
      패턴(`X-User-Id`)이 남아있다면 반드시 TODO로 표시했는가

**⚠️ 컨벤션 없음 - 팀 결정 필요** (다시 정해야 하는 것들):
- 응답 래퍼(`CommonResponse` 부활 여부), 페이지네이션 응답 형태
- 전역 예외 처리 체계(`ErrorCode`/`BusinessException` 부활 여부)
- 인증/인가 재구현(JWT 발급·검증, `@AuthenticationPrincipal` 바인딩)
- 테스트 작성 스타일(현재 도메인 테스트 예시 없음, "각 단계 최소 스모크 수준" 정도만
  수동 검증됨)
- Service 계층 인터페이스+구현 분리 여부
- `@Transactional` 부착 위치/전략
