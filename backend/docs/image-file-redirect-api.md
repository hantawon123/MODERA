# 이미지 원본·썸네일 리다이렉트 API (안드로이드 전달용)

> 작성일: 2026-08-05
>
> 대상 브랜치: `feat/image-file-redirect` → `develop/backend`
>
> 목적: 앱이 이미지를 로컬 우선(local-first)으로 다룰 수 있도록, **만료되지 않는 불변
> 주소**를 제공한다. 기존 JSON 응답의 presigned URL은 유효시간이 있어 Room에 저장하거나
> 이미지 캐시 키로 쓸 수 없었다.

## 1. 무엇이 달라지나

기존(5-2 상세 조회)은 그대로 유지된다. 이번 건은 **추가**다.

| | 기존 (JSON) | 추가 (리다이렉트) |
|---|---|---|
| 주소 | 응답 본문의 `imageUrl`/`thumbnailUrl` (매번 다름) | `/api/v1/images/{imageId}/file`·`/thumbnail` (**불변**) |
| Room 저장 | 불가(만료) | **가능** |
| 이미지 캐시 키 | 서명이 바뀌어 매번 캐시 미스 | **안정** |
| 응답 형식 | 공통 JSON envelope | **302 리다이렉트(본문 없음)** |

앱 코드에 presigned URL이 등장하지 않게 되는 것이 핵심이다. 앱은 불변 경로만 알고,
서버가 매 요청 인가를 거친 뒤 그 순간의 presigned URL을 `Location`으로 안내한다.

## 2. 엔드포인트

```
GET /api/v1/images/{imageId}/file        원본
GET /api/v1/images/{imageId}/thumbnail   썸네일
Authorization: Bearer {accessToken}
```

### 요청

| 항목 | 값 |
|---|---|
| Path | `imageId` (Integer) |
| Header | `Authorization: Bearer {accessToken}` — 필수 |
| Query | 없음 |

### 성공 응답

```http
HTTP/1.1 302 Found
Location: https://.../pictures/7/101-a.jpg?X-Amz-Algorithm=...&X-Amz-Signature=...
Cache-Control: no-store
```

- 본문 없음. **공통 JSON envelope로 감싸지 않는다.**
- `Location`의 presigned URL 유효시간은 **1시간**(조회용 GET presign의 기존 설정과 동일).
- 같은 `imageId`라도 호출할 때마다 `Location` 값은 달라진다(매 요청 신규 서명).
- `Cache-Control: no-store`인 이유: `Location`에 실린 값이 만료되는 URL이라 중간 캐시에
  남으면 만료된 주소가 재사용된다. **이미지 자체의 캐시는 앱이 이 불변 경로를 키로
  직접 관리한다**(리다이렉트를 캐시하지 말라는 뜻이지 이미지를 캐시하지 말라는 뜻이 아니다).

### 썸네일 폴백

썸네일이 아직 만들어지지 않은 이미지(분석 전 등)에 `/thumbnail`을 호출하면 404가 아니라
**원본 키로 302**한다. 클라이언트에서 분기할 필요가 없다.

### 에러

| 상태 | code | 언제 |
|---|---|---|
| 401 | `UNAUTHORIZED` | accessToken 없음·무효 |
| 404 | `IMAGE_NOT_FOUND` | **없거나 접근할 수 없는 imageId** |

에러는 기존 공통 에러 JSON 형식 그대로다.

```json
{"result":"FAIL","code":"IMAGE_NOT_FOUND","message":"이미지를 찾을 수 없습니다.","data":null,"timestamp":"..."}
```

> **403은 내려가지 않는다.** 존재하지 않음·삭제됨·타인 소유·업로드 미완료를 구분하지 않고
> 전부 404로 통일한다. 구분해서 알려주면 `imageId`를 순회하며 "있지만 남의 것"을 식별할 수
> 있어 리소스 열거가 가능해지기 때문이다. 클라이언트는 **404 = 없거나 접근 불가** 한 가지로
> 다루면 된다.

## 3. 안드로이드 연동 메모

### OkHttp 인터셉터 주의 — 가장 중요

**Bearer 토큰은 경로가 `/api/`이고 `X-Amz-Signature` 쿼리가 없는 요청에만 붙일 것.**

presigned 리다이렉트 대상에 `Authorization` 헤더가 따라가면 MinIO가 인증 방식 충돌로
**400**을 반환한다. 같은 도메인의 `/s3/` 경로로 프록시되므로 **OkHttp가 헤더를 자동으로
떼주지 않는다**(OkHttp는 호스트가 바뀔 때만 민감 헤더를 제거한다).

```kotlin
class AuthInterceptor(private val tokenProvider: TokenProvider) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val url = request.url

        val isApiCall = url.encodedPath.startsWith("/api/")
        val isPresigned = url.queryParameter("X-Amz-Signature") != null

        return if (isApiCall && !isPresigned) {
            chain.proceed(
                request.newBuilder()
                    .header("Authorization", "Bearer ${tokenProvider.accessToken()}")
                    .build()
            )
        } else {
            chain.proceed(request)   // presigned 대상에는 토큰을 붙이지 않는다
        }
    }
}
```

### Coil 사용

Room에는 불변 경로를 저장하고, 그대로 로더에 넘기면 된다.

```kotlin
// Room 저장: "/api/v1/images/101/thumbnail"
AsyncImage(
    model = "$BASE_URL/api/v1/images/$imageId/thumbnail",
    contentDescription = null,
)
```

- OkHttp가 302를 자동으로 따라가므로 앱에서 리다이렉트를 직접 처리할 필요가 없다.
- Coil의 디스크 캐시 키는 요청 URL(= 불변 경로)이라 서명이 바뀌어도 **캐시가 재사용**된다.
- 캐시가 살아 있는 동안에는 서버 왕복이 아예 없다. 302는 캐시 미스일 때만 발생한다.

### 404 처리

썸네일 폴백 덕분에 "분석 전이라 썸네일이 없음"으로 404가 나지는 않는다. 404가 오면
**이미지가 없거나 접근할 수 없는 것**이므로 플레이스홀더를 띄우고, 필요하면 목록을
갱신해 삭제된 항목을 정리하면 된다.

## 4. 카테고리 아이콘과 헷갈리지 말 것

카테고리 아이콘은 **다른 방식**으로 이미 확정돼 있다(이번 작업 범위 아님).

| | 카테고리 아이콘 | 사용자 이미지(이번 건) |
|---|---|---|
| 주소 | `{공개 도메인}/category-thumbnails/{categoryId}.png` | `/api/v1/images/{imageId}/thumbnail` |
| 인증 | 불필요 | **Bearer 필수** |
| 응답 | 이미지 바이트 직접(200) | **302 리다이렉트** |
| 이유 | AI가 만든 일반 아이콘이라 공개 가능 | 사용자 스크린샷이라 공개 불가 |

## 5. 서버 구현 메모

- `ImageFileRedirectService` — 소유권 검증 + 키 조회 + presign. 스키마 경계를 넘는 JOIN을
  만들지 않기 위해 `library_schema.user_image`(소유권), `image_schema.image_asset`(원본 키),
  `image_schema.thumbnail`(썸네일 키)을 각각 조회해 애플리케이션에서 조합한다.
- `ImageFileUrlFactory` — 원본(pictures 버킷) presign. 썸네일은 기존 `ThumbnailUrlFactory` 재사용.
- 유효시간은 조회용 GET presign의 기존 값 1시간을 그대로 쓴다. 업로드용 PUT의 10분은 용도가
  다른 설정이라 끌어오지 않았다.
- SecurityConfig 변경 없음 — `anyRequest().authenticated()`라 새 경로가 자동으로 인증 대상이다.
