# API 명세 변경점 (2026-07, 공통 규약 구현)

`feature/soa-setup` 브랜치에서 응답 래퍼·에러 처리·인증·로깅 규약을 구현하면서
기존 API 명세와 달라진 점을 정리했다. **명세 담당자·안드로이드 팀 전달용** —
클라이언트 파싱 로직에 직접 영향을 주는 항목 위주로 적었다.

---

## 1. 모든 응답에 envelope가 씌워진다 (`result` 필드 신설)

`/api/v1/**`의 모든 응답(성공/실패 공통)이 아래 형태로 바뀌었다. 기존에 응답
바디를 바로 파싱하던 클라이언트 코드는 전부 `data` 한 단계 더 파고 들어가야 한다.

```json
{
  "result": "SUCCESS 또는 FAIL",
  "code": "성공은 SUCCESS 고정, 실패는 에러코드 문자열",
  "message": "사람이 읽는 메시지",
  "data": { "...": "실제 데이터. 실패 시 보통 null" },
  "timestamp": "ISO-8601 UTC, 예: 2026-07-23T06:00:00.000Z"
}
```

- **`/internal/**`(서버 간 내부 API)와 actuator/swagger에는 이 envelope가 없다** —
  클라이언트가 직접 호출할 일이 없는 경로라 영향 없음.
- 실패 시 `data`가 항상 `null`인 건 아니다. **검증 실패(400)일 때는 `data`에 필드별
  오류 배열이 담긴다**: `[{"field": "email", "message": "must not be blank"}, ...]`.

## 2. login / refresh / logout 요청에 `deviceId` 필드 신설

기존 명세에는 없던 필드다. 기기별로 refreshToken을 따로 관리하기 위해(RTR —
Refresh Token Rotation) 추가했다.

| API | 필드 변화 |
|---|---|
| `POST /api/v1/auth/login` | 요청 바디에 `deviceId`(문자열, 필수) 추가 |
| `POST /api/v1/auth/refresh` | 요청 바디에 `refreshToken` 외 `deviceId`(문자열, 필수) 추가 |
| `POST /api/v1/auth/logout` | 요청 바디에 `refreshToken`, `deviceId` 둘 다 필수 |

같은 계정이라도 기기(`deviceId`)가 다르면 refreshToken이 독립적으로 관리된다.
같은 `deviceId`로 다시 로그인하면 이전 refreshToken은 자동으로 새 값으로
교체된다(재사용 불가).

**클라이언트 쪽 확인 필요**: 안드로이드 앱에서 기기를 식별할 안정적인 값(예: 설치
UUID, ANDROID_ID 등)을 `deviceId`로 보내야 한다. 앱 재설치 시 값이 바뀌면
이전 로그인 세션과 별개로 취급된다.

## 3. `logout`은 인증(Bearer)이 필수다 — 다른 auth API와 다르게 동작

`register`/`login`/`refresh`는 토큰 없이 호출하지만(SecurityConfig permitAll),
**`logout`은 `Authorization: Bearer {accessToken}` 헤더가 반드시 있어야 한다**.
없으면 요청 바디와 무관하게 401(`UNAUTHORIZED`)로 즉시 거절된다.

## 4. 에러 응답에 표준 에러 코드 도입

실패 응답의 `code` 필드가 아래처럼 고정된 문자열로 나온다(클라이언트에서 `code`
기준으로 분기 처리 가능):

| code | HTTP status | 의미 |
|---|---|---|
| `INVALID_PARAMETER` | 400 | 요청 검증 실패 |
| `UNAUTHORIZED` | 401 | 인증 안 됨(토큰 없음/무효) |
| `FORBIDDEN` | 403 | 인증은 됐지만 권한 부족 |
| `NOT_FOUND` | 404 | 리소스 없음(공통) |
| `CONFLICT` | 409 | 상태 충돌(공통) |
| `INTERNAL_ERROR` | 500 | 서버 내부 오류 |
| `DUPLICATE_LOGIN_ID` | 409 | 회원가입 시 아이디 중복 |
| `DUPLICATE_EMAIL` | 409 | 회원가입 시 이메일 중복 |
| `LOGIN_FAILED` | 401 | 로그인 실패(아이디/비밀번호 구분 없음 — 계정 열거 방지) |
| `INVALID_REFRESH_TOKEN` | 401 | refreshToken 무효/만료/재사용(회전된 옛 토큰)/deviceId 불일치 |
| `IMAGE_NOT_FOUND` | 404 | 이미지 없음 또는 본인 소유 아님(둘을 구분하지 않음) |
| `DUPLICATE_IMAGE` | 409 | (예약됨, 아직 실제로 발생하는 API 없음) |

**`IMAGE_NOT_FOUND`가 두 가지 경우를 구분하지 않는 것에 주의**: 존재하지 않는
`imageId`든, 다른 사용자 소유의 `imageId`든 똑같이 404로 응답한다(403이 아니다) —
다른 사용자의 리소스가 "존재는 한다"는 사실 자체를 클라이언트에 노출하지 않기
위한 의도적 설계다.

## 5. 새 엔드포인트: 이미지 단건 조회

기존 명세에 없던 `GET /api/v1/images/{imageId}`가 추가됐다(본인 소유 이미지의
등록 상태·분석 상태 확인용). 목록 조회/검색 API는 아직 없다.

## 6. 요청 추적용 헤더 `X-Request-Id` (선택)

클라이언트가 요청에 `X-Request-Id` 헤더를 보내면 서버가 그 값을 그대로 응답
헤더로 돌려준다(안 보내면 서버가 8자리 값을 생성해서 응답 헤더에 채워준다).
필수 아님 — 버그 리포트 시 클라이언트가 자체 요청 ID를 같이 보내주면 서버 로그와
대조하기 쉬워진다는 정도의 부가 기능.

---

## 미결 — 이후 정할 것

- 에러 `code` 값이 지금은 `DUPLICATE_LOGIN_ID`처럼 의미가 드러나는 문자열이다.
  옛 프로젝트에는 `U001`류 prefix+번호 체계가 있었는데, 이번에는 승계하지 않았다
  — **prefix 복귀 여부는 이 문서를 계기로 한 API 명세 회의에서 정한다.** 바뀌어도
  각 코드의 문자열 값만 바뀌고 응답 구조(`code` 필드 자체)는 그대로다.
