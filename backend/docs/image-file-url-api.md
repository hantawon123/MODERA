# 이미지 원본·썸네일 URL 발급 API (안드로이드 전달용)

> 작성일: 2026-08-06 (302 리다이렉트 → JSON 응답으로 개정)
>
> 대상 브랜치: `feat/image-file-url-json` → `develop/backend`
>
> 목적: 앱이 들고 있는 presigned URL이 만료됐을 때, 상세 조회 전체를 다시 부르지 않고
> **새 URL만 받아 재시도**할 수 있는 API를 제공한다.
>
> ⚠️ 이전 버전(302 리다이렉트)과 계약이 다르다. **JSON으로 URL 값을 준다** — 안드로이드
> 요청을 반영해 교체했다.

## 1. 엔드포인트

```
GET /api/v1/images/{imageId}/file        원본 URL 발급   (code I213)
GET /api/v1/images/{imageId}/thumbnail   썸네일 URL 발급 (code I214)
Authorization: Bearer {accessToken}
```

### 성공 응답 (공통 envelope)

```json
{
  "result": "SUCCESS",
  "code": "I213",
  "message": "요청이 성공했습니다.",
  "data": {
    "url": "https://.../pictures/7/101-a.jpg?X-Amz-Algorithm=...&X-Amz-Signature=..."
  },
  "timestamp": "2026-08-06T06:00:00.000Z"
}
```

- `url` 유효시간은 **1시간**. 같은 imageId라도 호출마다 값이 달라진다(매 요청 신규 서명).
- **만료되는 값이므로 Room에 영구 저장하지 말 것.** 저장은 `imageId`만 하고, URL은
  필요할 때 이 API로 받는다.

### 썸네일 폴백

썸네일이 아직 없는 이미지(분석 전 등)에 `/thumbnail`을 호출하면 404가 아니라 **원본
URL을 대신 준다.** 클라이언트에서 분기할 필요가 없다.

### 에러

| 상태 | code | 언제 |
|---|---|---|
| 401 | `UNAUTHORIZED` | accessToken 없음·무효 |
| 404 | `IMAGE_NOT_FOUND` | 없거나 접근할 수 없는 imageId |

> **403은 내려가지 않는다.** 존재하지 않음·삭제됨·타인 소유·업로드 미완료를 구분하지 않고
> 전부 404로 통일한다(리소스 열거 방지 — 5-2·5-6과 같은 정책).

## 2. 권장 사용 패턴

```
이미지 로드 (기존 보유 URL)
  └ 403 (만료) → GET /images/{id}/file → data.url로 재시도
```

- 상세 조회(5-2) 응답의 `imageUrl`/`thumbnailUrl`은 그대로 유지된다. 화면 첫 로드는
  그 값을 쓰고, 만료 시에만 이 API로 갱신하는 것이 왕복이 가장 적다.
- **Coil 캐시 키는 URL이 아니라 `imageId` 기준으로 지정할 것.** presigned URL은 호출마다
  바뀌므로 URL을 키로 쓰면 매번 캐시 미스가 난다.

```kotlin
AsyncImage(
    model = ImageRequest.Builder(context)
        .data(freshUrl)                        // 이 API로 받은 data.url
        .memoryCacheKey("image-$imageId")
        .diskCacheKey("image-$imageId")
        .build(),
    contentDescription = null,
)
```

## 3. OkHttp 인터셉터 주의 — 여전히 중요

**Bearer 토큰은 경로가 `/api/`이고 `X-Amz-Signature` 쿼리가 없는 요청에만 붙일 것.**

data.url(presigned)로 이미지를 로드할 때 `Authorization` 헤더가 따라가면 MinIO가 인증
방식 충돌로 **400**을 반환한다. 같은 도메인이라 OkHttp가 헤더를 자동으로 떼주지 않는다.

```kotlin
val isApiCall = url.encodedPath.startsWith("/api/")
val isPresigned = url.queryParameter("X-Amz-Signature") != null
// isApiCall && !isPresigned 일 때만 Authorization 추가
```

## 4. 카테고리 아이콘과 헷갈리지 말 것

카테고리 아이콘은 **다른 방식**이다(6-1 참고).

| | 카테고리 아이콘 | 사용자 이미지(이 문서) |
|---|---|---|
| 주소 | `{공개 도메인}/category-thumbnails/{categoryId}.png` — 영구, 그대로 로드 | 이 API로 받은 presigned URL — 1시간 |
| 인증 | 불필요 | API 호출에 Bearer 필수 (이미지 로드 자체는 불필요) |
| Room 저장 | URL 저장 가능 | `imageId`만 저장, URL은 재발급 |

카테고리 아이콘은 범용 픽토그램이라 공개 버킷이지만, 사용자 이미지는 개인 데이터라
공개할 수 없다 — 그래서 만료되는 서명 URL + 재발급 API 구조다.
