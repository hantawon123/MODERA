# [제안] feat(api): 연관 이미지 조회 api 호출부

> **팀원 리뷰 필요** — api-server는 담당자가 따로 있어 이 MR은 제안이다.
> 머지 여부·구조 변경은 담당자 판단에 맡긴다. `develop/backend`에 직접 머지하지 않았다.

- **source**: `feat/similar-image-api`
- **target**: `develop/backend`
- **선행 의존**: worker 쪽 검색 내부 API MR(`feat/similar-image-search`)이 먼저 머지돼야
  실제로 동작한다. 컴파일·기동에는 의존이 없어 순서가 뒤집혀도 깨지지는 않는다
  (worker가 없으면 연관 목록이 빈 배열로 나갈 뿐).

## 무엇을 하는가

`GET /api/v1/images/{imageId}/similar?limit=10`

worker의 `GET /internal/v1/images/{imageId}/similar`로 유사도(임베딩 코사인)를 받고,
api-server가 `query_schema.user_image_view`에서 화면 데이터를 붙여 내보낸다.
유사도는 `modera_analysis`, 화면 데이터는 `modera_api`에 있어서 한쪽만으로는 응답을 못 만든다.

```json
{
  "result": "SUCCESS", "code": "SUCCESS", "message": "요청이 성공했습니다.",
  "data": {
    "baseImageId": 101,
    "baseTitle": "ASCII 해커톤",
    "count": 3,
    "list": [
      { "imageId": 102, "title": "ASCII 해커톤 발표자료", "summary": "...",
        "favorite": true, "thumbnailUrl": "http://.../thumb.webp?X-Amz-...",
        "tags": ["해커톤", "발표"], "category": "행사", "score": 0.9950372 }
    ]
  },
  "timestamp": "2026-07-30T01:21:03.653Z"
}
```

아이템 필드명은 명세서 5-1 목록과 맞췄다(`tags`/`category`/`favorite`). `score`만 추가,
`uploadedAt`은 유사도순 정렬이라 뺐다. 페이지네이션이 없어 `PageResponse`는 쓰지 않고
목록 필드명(`list`)만 맞췄다.

## 변경 파일

| 파일 | 내용 |
| --- | --- |
| `global/config/WorkerClientConfig` | worker 전용 `RestClient` 빈. baseUrl + `X-Internal-Token` 기본 헤더, connect 1s / read 3s |
| `domain/image/client/WorkerSearchClient` | worker 호출 + 응답 record 매핑, 실패 시 빈 목록 |
| `domain/query/repository/UserImageViewRepository` | `findAllByUserIdAndImageIdIn` 추가 (기존 메서드 무변경) |
| `domain/query/repository/UserImageViewSummary` | 목록용 조회 결과 record |
| `domain/image/service/ImageSimilarService` | 소유권 검증 → worker 호출 → IN 일괄 조회 → 순서 유지 조립 |
| `domain/image/service/ThumbnailUrlFactory` | `thumbnail_key` → presigned GET URL(1시간) |
| `domain/image/dto/SimilarImagesResponse`, `SimilarImageItemResponse` | 응답 DTO |
| `domain/image/controller/ImageController` | 메서드 1개 추가 (기존 메서드 무변경) |
| `application-{local,docker,prod}.yml` | `worker.base-url`, `internal.token` |

기존 코드 수정은 컨트롤러 메서드 추가와 리포지토리 메서드 추가 두 군데뿐이다.

## 리뷰해주셨으면 하는 판단 3가지

### 1. worker 호출 실패 → 500이 아니라 빈 목록 (graceful degradation)

연관 자료는 상세 화면의 부가 패널이다. 본체(5-2 상세)는 이미 성공적으로 조회된
상태인데 부가 패널 하나 때문에 화면 전체를 500으로 되돌리는 건 손해가 크다고 봤다.
worker가 죽어도 상세 화면은 "연관 자료 없음"으로 뜨는 게 낫다는 판단.

대신 조용히 삼키면 설정 오류를 영원히 못 잡으므로 로그 레벨로 원인을 나눴다.

| 상황 | 레벨 | 이유 |
| --- | --- | --- |
| 4xx (토큰 불일치 401, 경로 오타 404) | `error` | 설정·계약 문제. 재시도해도 안 낫는다 |
| 5xx · 연결 실패 · 타임아웃 | `warn` | 일시 장애 |

CLAUDE.md 8절의 컨슈머 영구/일시 오류 분류와 같은 관점이다.
**반대 의견 있으면 여기가 가장 바꾸기 쉬운 지점이다** — `WorkerSearchClient`의
catch 블록에서 `BusinessException`을 던지도록 바꾸면 500 정책으로 전환된다.

### 2. worker에 limit보다 넉넉히 요청한다 (over-fetch)

worker는 `modera_analysis`만 보므로 `user_image_view.del_yn`을 알 수 없다. 삭제 필터는
api-server만 할 수 있다. 그래서 딱 limit만 받아오면 **limit=2인데 1건만 나가는** 일이
생긴다(E2E에서 실제로 재현됐다 — worker가 준 top-2 중 1건이 삭제된 이미지였다).

`limit * 2 + 5`(최대 100)를 받아 필터링한 뒤 limit으로 자른다. worker 쪽 벡터 스캔
LIMIT만 조금 커지는 비용이고, 대신 클라이언트가 요청한 개수를 채워준다.

이 비대칭 자체를 없애려면 worker가 필터링에 필요한 정보를 알아야 하는데, 그건
DB 경계를 넘는 일이라(CLAUDE.md) 하지 않았다.

### 3. limit 범위를 벗어나면 400이 아니라 경계값으로 당긴다

`limit`은 1~50으로 clamp한다(`limit=0` → 1, `limit=999` → 50). 명세서가 `sort`에는
`INVALID_PARAMETER`(400)를 쓰지만, 부가 기능에서 요청을 실패시키는 쪽이 손해가
크다고 봐서 clamp를 골랐다. 400이 맞다면 `ImageSimilarService.clampLimit`을
검증으로 바꾸면 된다.

## 로컬 E2E 검증 결과

worker(8081) + api-server(8080)를 실제로 띄우고 JWT 로그인까지 실제 흐름으로 확인했다.

| 시나리오 | 결과 |
| --- | --- |
| 정상 조회 | 200, 유사도 내림차순 유지 `[102, 103, 104]`, `baseTitle` 채워짐 |
| 삭제된 이미지(`del_yn='Y'`) | 결과에서 제외됨 |
| 뷰 row 없는 이미지 | 결과에서 제외됨 |
| 썸네일 없는 이미지 | `thumbnailUrl: null` |
| `limit` 1/2/3/5/999/0/-7 | 각각 1/2/3/3/3/1/1건, clamp 정상 |
| 소유권 없는 imageId | 404 `IMAGE_NOT_FOUND` |
| 존재하지 않는 imageId | 404 `IMAGE_NOT_FOUND` |
| accessToken 없음 | 401 |
| **worker 프로세스 중단** | 200 + `count: 0`, 18ms 응답, `warn` 로그 |
| **internal.token 불일치** | 200 + `count: 0`, `error` 로그 (토큰 값은 로그에 안 남음) |

`score`는 worker 값과 정확히 일치한다(`0.9950372`). float을 double로 넓히면
`0.9300000071525574` 같은 노이즈가 생기므로 응답까지 `Float`으로 들고 갔다.

## 참고 / 후속

- **테스트 코드가 없다.** api-server에 아직 테스트가 한 건도 없어서 기존 관례에 맞췄다.
  `WorkerSearchClient`의 실패 분기와 `ImageSimilarService`의 순서 유지·필터링은
  단위 테스트 가치가 높은 지점이라, 테스트 도입 시 우선 후보로 추천한다.
- `ThumbnailUrlFactory`는 5-1 목록 조회에서도 그대로 쓸 수 있게 도메인 서비스로 뺐다.
  5-1을 구현할 때 재사용하면 presigned 로직이 두 곳에 생기지 않는다.
- `internal.token`은 worker의 `internal.callback.token`과 **같은 값**이어야 한다.
  운영 배포 시 두 컨테이너에 같은 `INTERNAL_TOKEN`을 주입해야 하는데, 배포 파일은
  인프라 담당 영역이라 건드리지 않았다. **이 부분만 확인 부탁드린다.**
