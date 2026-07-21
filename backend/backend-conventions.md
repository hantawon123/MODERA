# 백엔드 컨벤션 (신규 도메인 추가 시 팀 규칙)

> 이 문서는 실제 코드(`global` 패키지, Liquibase changelog)를 근거로 역추적한 규칙입니다.
> 근거가 없는 항목은 **⚠️ 컨벤션 없음 - 팀 결정 필요**로 표시했습니다.
> 온보딩 실습은 [onboarding-prompt.md](./onboarding-prompt.md) 참고.

---

## 1. 패키지 / 레이어 구조

**규칙**: 새 도메인은 `global`과 분리된 **최상위 형제 패키지**로 만든다.

```
com.ssafy.modera
├── global      ← 공통 인프라 전용. 도메인 지식이 들어가면 안 됨.
└── {domain}    ← 예: memo, post, user 등 새 도메인은 여기
```

**근거**:
- 현재 코드에는 [`global`](src/main/java/com/ssafy/modera/global) 패키지만 존재하고 도메인 패키지 예시가 없음.
- [`LogAspect.java`](src/main/java/com/ssafy/modera/global/log/LogAspect.java) 18~22행의 포인트컷 `execution(* com.ssafy..*Controller.*(..))`, `execution(* com.ssafy..*Service.*(..))`이 `com.ssafy.` 하위 **어디에 있든** 클래스명 접미사만 보고 자동 로깅 대상으로 잡음. → 패키지 위치는 자유롭지만 클래스 접미사(`*Controller`, `*Service`)는 반드시 지켜야 자동 로깅이 걸림.
- [`ErrorCode.java`](src/main/java/com/ssafy/modera/global/domain/ErrorCode.java) 19~26행에 이미 `// User (사용자, U-xxx)`, `// Auth (인증/인가, A-xxx)` 같은 도메인별 주석 블록 관례가 있음 → 새 도메인은 자기 접두어(예: `M-xxx` for Memo)를 새로 할당.

**⚠️ 컨벤션 없음 - 팀 결정 필요**: 도메인 패키지 내부를 `controller/service/repository/domain/dto` 평면 구조로 할지, `application/domain/infrastructure` 계층형으로 할지는 실제 도메인 코드 예시가 하나도 없어 확인 불가. 팀이 하나를 정해서 이 문서에 추가해야 함.

---

## 2. Entity 규칙

**규칙**: 모든 Entity는 [`BaseTimeEntity`](src/main/java/com/ssafy/modera/global/domain/BaseTimeEntity.java)를 상속한다.

```java
@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseTimeEntity {
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
```

- `extends BaseTimeEntity`만 하면 `createdAt`/`updatedAt`이 자동으로 채워짐. `@EnableJpaAuditing`은 [`JpaAuditingConfig.java`](src/main/java/com/ssafy/modera/global/config/JpaAuditingConfig.java)가 이미 전역 활성화해두었으므로 추가 설정 불필요.
- **DB 컬럼 네이밍**: [`01_create_table_users.sql`](src/main/resources/db/changelog/20260101/01_create_table_users.sql)에서 확인되는 패턴 — **snake_case**, PK는 `{테이블명 단수}_id` (예: `test_id`), 각 컬럼에 `COMMENT '한글 설명'` 부여.

**⚠️ 컨벤션 없음 - 팀 결정 필요**: 연관관계(`@ManyToOne`/`@OneToMany`) 페치 전략, 양방향/단방향 여부는 기존 코드에 연관관계를 가진 Entity 예시가 없어 확인 불가. 팀 컨벤션으로 "기본 지연 로딩(LAZY) + 단방향 우선" 같은 규칙을 별도로 정할 것을 권장.

---

## 3. 응답 규칙

**핵심 규칙: Entity를 절대 그대로 컨트롤러 응답으로 반환하지 말 것.** 반드시 응답 전용 DTO(`XxxResponse`)로 변환해서 내려준다. (Entity 직접 노출 시 지연 로딩 프록시 직렬화 문제, DB 스키마 변경이 API 스펙에 그대로 새어나가는 문제가 생기므로 응답 DTO 계층이 이를 막는 방어선.)

모든 API 응답은 [`CommonResponse<T>`](src/main/java/com/ssafy/modera/global/domain/dto/CommonResponse.java)로 감싼다.

```java
public static <T> CommonResponse<T> onSuccess(T data) { ... }
public static CommonResponse<Void> onSuccess() { ... }
public static CommonResponse<Void> onFailure(ErrorCode errorCode) { ... }
```

`data` 자리에 무엇을 넣을지는 상황에 따라 다름:

| 상황 | data 타입 | 근거 |
|---|---|---|
| 단건 조회/생성/수정 | 응답 DTO 직접 | [`CommonResponse.onSuccess(T data)`](src/main/java/com/ssafy/modera/global/domain/dto/CommonResponse.java) |
| 삭제 등 데이터 없는 성공 | 없음(`Void`) | `CommonResponse.onSuccess()` |
| 총 개수/전체 페이지 수가 필요한 목록(게시판형) | `PageResponse<T>` | [`PageResponse.from(Page<T>)`](src/main/java/com/ssafy/modera/global/domain/dto/PageResponse.java) |
| count 쿼리 없이 다음 페이지 여부만 필요한 목록(무한스크롤) | `SliceResponse<T>` | [`SliceResponse.from(Slice<T>)`](src/main/java/com/ssafy/modera/global/domain/dto/SliceResponse.java) |

**⚠️ 컨벤션 없음 - 팀 결정 필요**: 컨트롤러가 `CommonResponse<T>`를 직접 반환할지 `ResponseEntity<CommonResponse<T>>`로 감쌀지는 실제 컨트롤러 예시가 하나도 없어 확정 불가.

---

## 4. 에러 규칙

**Step 1 — ErrorCode 추가**: [`ErrorCode.java`](src/main/java/com/ssafy/modera/global/domain/ErrorCode.java)에 도메인별 주석 블록 + 접두어로 그룹을 이어서 추가한다.

```java
// Memo (메모, M-xxx)
MEMO_NOT_FOUND(HttpStatus.NOT_FOUND, "M001", "메모를 찾을 수 없습니다."),
```

⚠️ **필수 확인**: 코드값(`M001` 등)을 추가하기 전에 **전체 enum을 훑어서 코드가 중복되지 않는지 직접 확인**할 것. 현재 코드에도 이미 `JWT_INVALID(A005)`/`INVALID_REFRESH_TOKEN(A005)`처럼 코드값이 중복된 사례가 있음(30~33행 vs 49~51행) — 자동으로 걸러주는 장치가 없으므로 사람이 직접 확인해야 함.

**Step 2 — 예외 던지기**: 도메인 전용 예외 클래스를 새로 만들지 않는다. [`BusinessException`](src/main/java/com/ssafy/modera/global/exception/BusinessException.java)은 이미 범용이므로:

```java
throw new BusinessException(ErrorCode.MEMO_NOT_FOUND);
```

**Step 3 — 처리 흐름**: [`GlobalExceptionAdvice`](src/main/java/com/ssafy/modera/global/exception/GlobalExceptionAdvice.java)가 `@RestControllerAdvice`로 전역에서 잡아 자동으로 `CommonResponse.onFailure(errorCode)`로 변환한다. 컨트롤러/서비스에서 별도 try-catch 불필요.

```
throw new BusinessException(ErrorCode.MEMO_NOT_FOUND)
  → GlobalExceptionAdvice.handleBusinessException()  (GlobalExceptionAdvice.java 27~33행)
  → CommonResponse.onFailure(ErrorCode.MEMO_NOT_FOUND) 로 응답
```

`@Valid` 검증 실패(`BindException`/`MethodArgumentNotValidException`), 잘못된 요청(`HttpMessageNotReadableException` 등)도 전부 같은 파일에서 자동 처리됨(35~84행) — 컨트롤러가 신경 쓸 필요 없음.

---

## 5. DB 규칙 (Liquibase)

**핵심 규칙: 운영 DB든 로컬 DB든 테이블을 직접 손으로 만들거나 수정하지 말 것.** 모든 스키마 변경은 Liquibase changelog로만 반영한다. 근거: [`application-local.yml`](src/main/resources/application-local.yml)의 `jpa.hibernate.ddl-auto: validate` — Hibernate가 테이블을 만들지 않고 엔티티와 실제 스키마가 일치하는지 검증만 하므로, Liquibase가 스키마의 유일한 진실 공급원이다.

**파일 위치/네이밍 규칙**:
- 마스터 파일([`db.changelog-master.yaml`](src/main/resources/db/db.changelog-master.yaml))은 `includeAll`로 `db/changelog` 전체를 스캔하므로 **직접 수정할 필요 없음**.
- 새 폴더는 **작업 날짜 기준 `YYYYMMDD`** 디렉토리(예: `db/changelog/20260707/`).
- 파일명은 [`LiquibaseIncludeAllFilter.java`](src/main/java/com/ssafy/modera/global/liquibase/LiquibaseIncludeAllFilter.java) 18행의 정규식 `^[0-9]+_[a-zA-Z0-9_]+\.sql$`을 반드시 통과해야 함 (예: `01_create_table_memos.sql`). ⚠️ 규칙을 어기면 **에러 없이 조용히 스킵**되므로(32~38행, `log.debug`만 남김), 마이그레이션이 반영 안 됐는데도 앱은 정상 기동되는 상황이 생길 수 있음 — 반드시 파일명부터 재확인.
- 파일 내용은 [`01_create_table_users.sql`](src/main/resources/db/changelog/20260101/01_create_table_users.sql) 1~3행처럼 `-- liquibase formatted sql` + `-- changeset {작성자}:{id}` 헤더 필수.

---

## 6. 인증 규칙

인증이 필요한 API에서 로그인한 사용자 정보를 받는 법:

```java
@GetMapping("/{memoId}")
public CommonResponse<MemoResponse> getMemo(
        @AuthenticationPrincipal PrincipalDetails principal,
        @PathVariable Long memoId) {
    Long userId = principal.getUserId();
    ...
}
```

**근거**: [`PrincipalDetails.java`](src/main/java/com/ssafy/modera/global/security/principal/PrincipalDetails.java)가 표준 `UserDetails`를 구현하고, [`JwtAuthenticationFilter.java`](src/main/java/com/ssafy/modera/global/security/filter/JwtAuthenticationFilter.java) 54~55행에서 `SecurityContextHolder`에 `JwtAuthenticationToken(principal=PrincipalDetails, ...)`을 넣어두므로, Spring Security 표준 방식인 `@AuthenticationPrincipal` 바인딩이 추가 설정 없이 그대로 동작한다. `principal.getUserId()`, `principal.getAuthUser().email()`([`AuthUser.java`](src/main/java/com/ssafy/modera/global/security/dto/AuthUser.java))로 필요한 정보를 꺼낸다.

인증/인가 대상 URL 지정은 두 곳을 같이 봐야 한다:
- [`SecurityConfig.java`](src/main/java/com/ssafy/modera/global/config/SecurityConfig.java) 76~82행 `authorizeHttpRequests` — 인가 규칙 (기본은 `anyRequest().authenticated()`).
- [`JwtAuthenticationFilter.java`](src/main/java/com/ssafy/modera/global/security/filter/JwtAuthenticationFilter.java) 28~35행 `excludeUrlPatterns` — 토큰 파싱 자체를 건너뛸 공개 URL.

새 API가 로그인 없이 열려야 한다면 **두 곳 다** 확인/수정해야 한다(하나만 고치면 필터는 통과해도 인가 단계에서 막히거나, 그 반대 상황이 생길 수 있음).

---

## 7. PR 리뷰 체크리스트 (초보가 자주 놓치는 항목)

- [ ] Entity를 그대로 응답에 쓰지 않고 응답 DTO(`XxxResponse`)로 변환했는가
- [ ] 새 `ErrorCode` 추가 시 코드값이 기존 enum과 중복되지 않는지 전체를 훑어 확인했는가
- [ ] 도메인 전용 예외 클래스를 새로 만들지 않고 `BusinessException(ErrorCode.XXX)`를 그대로 썼는가
- [ ] Liquibase 파일이 `{숫자}_{설명}.sql` 정규식을 통과하는지, 날짜 폴더에 제대로 들어갔는지 (기동 로그에 스킵 경고가 없는지)
- [ ] DB 테이블을 직접 만들거나 수정하지 않고 Liquibase changelog로만 반영했는가
- [ ] 새 API의 인증 필요 여부를 `SecurityConfig`(인가)와 `JwtAuthenticationFilter`(필터 제외 목록) 양쪽에서 일관되게 처리했는가
- [ ] 컨트롤러/서비스 클래스명이 정확히 `*Controller`/`*Service`로 끝나서 `LogAspect` 자동 로깅 대상이 되는가
- [ ] 목록 API가 `PageResponse`(전체 개수 필요)인지 `SliceResponse`(무한스크롤)인지 프론트 요구사항에 맞게 골랐는가
- [ ] 요청 DTO에 비밀번호 등 민감정보가 있다면, `LogAspect`가 메서드 인자를 그대로 로그에 찍으므로 마스킹/제외 처리를 했는가
- [ ] Swagger 문서 어노테이션(`@CommonReadErrorDocs` 등)을 재사용했다면, 예시 `code` 값이 실제 `ErrorCode`와 일치하는지 재확인했는가 (기존 문서 예시 자체가 실제 값과 어긋난 사례 있음)
- [ ] `@Valid`로 요청 DTO 검증 애노테이션(`@NotBlank` 등)을 넣었는가 (넣기만 하면 `GlobalExceptionAdvice`가 나머지를 처리함)

**⚠️ 컨벤션 없음 - 팀 결정 필요** (체크리스트에 넣기 전에 팀이 먼저 정해야 하는 것들):
- 테스트 작성 스타일 (현재 `src/test`에 컨텍스트 로드 테스트 1개뿐, 도메인 테스트 예시 없음)
- Service 계층 인터페이스+구현 분리 여부 (보안 계층(`JwtTokenProvider`/`Impl`)은 분리되어 있지만, 일반 도메인 서비스까지 강제하는 근거는 없음)
- `@Transactional` 부착 위치/전략
- Repository에서 QueryDSL 커스텀 구현체 네이밍 규칙 (의존성은 있으나 실제 예시 없음)