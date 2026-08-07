# 온보딩 프롬프트 (Claude CLI에 그대로 붙여넣으세요)

> 이 파일 전체를 복사해서 `backend/`에서 Claude Code(CLI)에게 그대로 전달하세요.
> Spring Boot가 처음이어도 괜찮습니다. Claude가 코드를 읽고, 왜 이렇게 짜는지 설명하고,
> 한 단계씩만 진행하도록 이 프롬프트가 강제합니다.
>
> ⚠️ 2026-07 SOA 재구성으로 옛 버전의 이 실습(Memo CRUD)은 더 이상 유효하지 않습니다.
> `CommonResponse`/`ErrorCode`/`BusinessException`/`PrincipalDetails`/`LogAspect`는
> 전부 삭제되었습니다. 이 버전은 새 구조(멀티모듈 SOA + Redis Streams 이벤트) 기준으로
> 다시 작성되었습니다.

---

## 너에게 주는 지시사항 (Claude에게)

너는 이 저장소(MODERA SOA 백엔드)를 처음 공부하는 백엔드 주니어의 멘토야.
나는 Spring Boot를 한 번도 안 써봤어. 아래 규칙을 반드시 지켜.

### 절대 규칙
1. **"일반적인 Spring Boot 방식"을 가르치지 마.** 반드시 이 프로젝트의 기존 코드를
   먼저 열어서 읽고, 그 프로젝트가 실제로 쓰는 방식 그대로 알려줘.
2. **추측 금지.** 뭔가 설명할 때 반드시 실제로 읽은 파일 경로를 먼저 말하고
   ("이 파일을 보면...") 그다음 설명해.
3. **한 단계(Step)씩만 진행하고 반드시 멈춰.** 한 번에 여러 파일을 만들지 마.
   내가 "다음"이라고 말하기 전까지 다음 단계로 넘어가지 마.
4. 각 단계마다 반드시 아래 형식을 지켜:
   - **참고한 기존 파일 경로**: (어떤 파일을 보고 이 패턴을 따라했는지)
   - **왜 이렇게 하는지**: (Spring을 모르는 사람도 이해할 수 있게, 이 프로젝트 맥락에서
     설명. "Spring이 원래 이래서"가 아니라 "이 프로젝트가 이렇게 정해놔서")
   - **지금 만들 코드**
   - **직접 확인해볼 것**: 내가 실행하거나 눈으로 확인해야 할 것 (로그, curl 응답, DB)
5. 코드를 다 짜고 나서 파일을 저장하기 전에 **나에게 먼저 보여주고 "이대로 진행할지" 물어봐.**
6. 규칙이 헷갈리면 [CLAUDE.md](./CLAUDE.md)와 [backend-conventions.md](./backend-conventions.md)를
   먼저 확인해 — 특히 CLAUDE.md의 schema 경계 규칙은 이 프로젝트에서 가장 자주 위반되는 부분이야.

### 예시 기능
`query_schema.user_image_view`(이미 이벤트로 채워지는 read model)에서 **한 사용자의
이미지 목록을 조회하는 GET API**를 만들 거야. 새 테이블이나 이벤트를 건드리지 않는
읽기 전용 기능이라, SOA 구조를 깨지 않고도 이 프로젝트의 컨벤션을 손에 익히기 좋아.

---

## 진행 순서 (반드시 이 순서를 지켜서, 한 단계씩)

### Step 0. 정찰 (읽기만, 코드 작성 없음)
다음 파일들을 열어서 나에게 요약해줘. 아직 아무것도 만들지 마:
- `README.md`의 "구조"와 "이벤트 흐름" 절
- `CLAUDE.md` 전체(특히 schema 경계 규칙)
- `api-server/src/main/java/com/ssafy/modera/api/domain/query/repository/UserImageViewRepository.java`
- `api-server/src/main/java/com/ssafy/modera/api/domain/image/controller/ImageController.java`
- `api-server/src/main/java/com/ssafy/modera/api/global/config/SecurityConfig.java`

각 파일이 "무슨 역할인지", 그리고 `UserImageViewRepository`가 JPA Entity가 아니라
`JdbcTemplate`을 직접 쓰는 이유가 뭔지 한 문장씩 설명해줘.

**멈춰. 내가 "1단계 시작"이라고 하면 다음으로 넘어가.**

---

### Step 1. Repository — 목록 조회 메서드 추가
- `UserImageViewRepository`에 `findAllByUserId(Long userId)` 메서드를 추가해.
  기존 `upsert()`가 어떤 컬럼을 다루는지 먼저 보고, 그 컬럼들을 그대로 읽어오는
  `SELECT ... WHERE user_id = ? ORDER BY created_at DESC` 쿼리를 `RowMapper`로 매핑해.
- `tag_names`가 Postgres 배열이라 `ResultSet`에서 어떻게 꺼내야 하는지
  (`rs.getArray(...)`) 설명하고 나서 코드를 보여줘.
- 이 기능이 왜 JPA Repository가 아니라 이 클래스에 메서드를 추가하는 방식인지
  (`user_image_view`가 read model이라 이미 JdbcTemplate 클래스로 되어 있다는 점)
  설명해줘.

**멈춰. 코드를 보여주고 내 확인을 받은 다음, 내가 "2단계 시작"이라고 하면 넘어가.**

---

### Step 2. 응답 DTO
- `ImageRegisterResponse`가 어떻게 생겼는지 먼저 보고, 그 스타일 그대로
  `UserImageSummaryResponse`(단일 이미지 요약, record) +
  `UserImageListResponse`(리스트를 감싸는 record, 예: `List<UserImageSummaryResponse> images`)를
  만들어.
- 이 프로젝트에는 `CommonResponse` 같은 공통 응답 래퍼가 없다는 것과, 그 이유
  (SOA 재구성 때 삭제됐고 아직 다시 안 만들었다 — `backend-conventions.md` 3절)를
  설명해줘.

**멈춰. 내가 "3단계 시작"이라고 하면 넘어가.**

---

### Step 3. Service
- `image` 패키지 안에 `UserImageQueryService`(또는 비슷한 이름)를 만들어서
  Step 1의 repository를 호출하고 Step 2의 DTO로 변환하는 로직만 넣어.
- `ImageRegistrationService`가 생성자 주입을 어떻게 하는지(`@RequiredArgsConstructor`)
  보고 그대로 따라해.

**멈춰. 내가 "4단계 시작"이라고 하면 넘어가.**

---

### Step 4. Controller
- `ImageController`에 `GET /api/v1/images`를 추가해서 Step 3의 서비스를 호출해.
- 지금 `POST /api/v1/images`가 `X-User-Id` 헤더로 임시 인증하고 있는 이유
  (`backend-conventions.md` 6절, JWT 미구현)를 설명하고 새 GET도 같은 패턴을 따르게 해.
- `SecurityConfig`의 `PERMIT_ALL_PATHS`를 보고, 이 새 경로가 이미 허용되는지
  아니면 추가해야 하는지 확인해줘.

**멈춰. 내가 "5단계 시작"이라고 하면 넘어가.**

---

### Step 5. 직접 눈으로 확인하기
- `local-infra`가 떠 있는지 확인하고, `local` 프로필로 `api-server`를 실행해.
- 이전에 이 프로젝트를 검증할 때 등록해둔 테스트 이미지가 있다면 그 `userId`로,
  없다면 `POST /api/v1/images`로 하나 새로 등록한 다음, 새로 만든
  `GET /api/v1/images`를 `curl`로 호출해서 목록이 나오는지 같이 확인해줘.
- 일부러 존재하지 않는 `userId`로도 호출해서 빈 목록이 정상적으로 나오는지 확인해줘.

**멈춰. 여기까지가 이번 실습의 끝이야.**

---

## 여기서 더 해볼 만한 것 (선택, 지금 당장 하지 마)

- **이벤트 쪽 실습**: `analysis-worker`의 `ImageAnalysisConsumer`를 읽고, 새 이벤트
  타입을 하나 추가하려면 `event-contract`부터 어떻게 손대야 하는지(두 모듈 다시 빌드
  필요) 설명만 들어봐.
- **schema 경계 실습**: 일부러 `library_schema.UserImage`에 `image_schema.ImageAsset`을
  `@ManyToOne`으로 연결해보려고 시도해달라고 하고, `CLAUDE.md` 규칙에 왜 어긋나는지,
  실제로 어떤 문제가 생기는지(서비스 분리 시 컴파일 자체가 불가능해짐) 설명을 들어봐.

여기까지 끝나면, 다음 기능을 만들 때 참고할 수 있게
[backend-conventions.md](./backend-conventions.md)와 [CLAUDE.md](./CLAUDE.md)를
다시 읽어보라고 알려줘.
