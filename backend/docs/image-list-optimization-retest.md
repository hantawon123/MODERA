# 이미지 목록 최적화 및 API 인증 최적화 재측정

> 측정일: 2026-08-03
>
> 대상 브랜치: `develop/backend`
>
> 로컬 비교 브랜치: `chore/performance-test-api`
>
> 대상 컨테이너: `modera-api`
>
> 이미지 목록 요청: `GET /api/v1/images?page=0&size=20&categoryId=1744084819`

## 1. 코드 및 배포 설정 확인

- 로컬 `chore/performance-test-api` HEAD: `8aed1f49edb0d4c62fc46198bc1b76245ee65293`
- 원격 `origin/develop/backend` HEAD: `5bc249b902284fad175f6acadabb5b36d723cda2`
- `develop/backend`는 성능 개선 브랜치를 병합한 merge commit이다.
- 두 commit의 파일 내용을 `git diff`로 비교한 결과 차이가 없었다.
- 배포 API의 Actuator metric에서 Hikari 최대 커넥션이 `30`임을 확인했다.
- Worker의 Hikari 설정은 변경하지 않았다.
- 배포 직후 categoryId를 포함한 이미지 목록 smoke test가 성공했다.

즉, commit hash는 병합 commit 때문에 다르지만 테스트 대상 소스 내용은 로컬 성능 개선 브랜치와 동일하다.

## 2. 판정 기준

### API 부하 테스트

- 실행 시간: 60초
- 이미지 목록 응답시간 p95: 300ms 이하
- HTTP 오류율: 1% 미만
- dropped iteration: 0건
- checks 성공률: 99% 이상

### API 과부하 테스트

- 이미지 목록 부하: 30초
- health 감시: 45초
- health 요청 성공률: 100%
- HTTP 오류: 0건
- dropped iteration: 0건
- 테스트 종료 후 컨테이너와 의존 서비스가 정상 상태여야 한다.

### 사용자 흐름 테스트

- 각 사용자는 로그인 1회 후 실제 앱 사용 비율로 Spring API를 100회 호출한다.
- 실행 시간: 120초
- 사용자 시작 분산: 10초
- AI, Worker, MinIO 파이프라인은 제외한다.
- 일반 비즈니스 API p95: 300ms 이하
- HTTP 오류: 0건
- 모든 사용자가 100단계를 완료해야 한다.
- 로그인 p95는 별도 병목 지표로 기록한다.

## 3. 이미지 목록 API 부하 테스트

직전 고부하 실행의 잔여 영향을 피하기 위해 각 단계 사이에 cooldown을 두고 독립 실행했다. VU 부족으로 발생하는 dropped iteration을 방지하도록 충분한 VU를 미리 할당했다.

| 목표 부하 | 실제 전체 RPS | 이미지 목록 p95 | HTTP 오류 | dropped | 판정 |
|---:|---:|---:|---:|---:|---|
| **400 RPS** | **397.52** | **53.39ms** | **0건** | **0건** | **최대 통과** |
| **450 RPS** | **420.09** | **1.26초** | **36건** | **1,185건** | **최초 실패** |

400 RPS에서는 60초 동안 목표 요청을 손실 없이 처리했다. 450 RPS에서는 실제 처리량이 약 420 RPS에서 더 증가하지 않았고, 응답 지연·오류·dropped iteration이 동시에 발생했다. 따라서 현재 조건에서 0.3초 기준 최대 안정 처리량은 **400 RPS**이다.

## 4. 이미지 목록 API 과부하 테스트

| 목표 부하 | 비즈니스 요청 | 전체 p95 | HTTP 오류 | health | dropped | 판정 |
|---:|---:|---:|---:|---:|---:|---|
| **400 RPS** | 약 12,000건 | **102.91ms** | **0건** | **45/45** | **0건** | **최대 통과** |
| **450 RPS** | 약 13,150건 | **1.28초** | **9건** | **32/32** | **338건** | **최초 실패** |

450 RPS에서도 서버 프로세스는 살아 있었지만 요청 지연 때문에 45초 동안 health 확인이 32회만 실행됐고, 오류와 dropped iteration이 발생했다. 따라서 단순 생존이 아니라 정상 응답을 유지하는 30초 과부하 처리 한계는 **400 RPS**이다.

## 5. 최신 사용자 흐름 테스트

### 5.1 125명 독립 재측정

직전 과부하 테스트의 영향을 제거하고 Hikari `active=0`, `pending=0` 상태에서 다시 실행했다.

| 항목 | 결과 |
|---|---:|
| 계획된 시나리오 API 호출 | 12,500건 |
| 로그인 포함 비즈니스 요청 | 12,625건 |
| health 포함 전체 HTTP 요청 | 12,766건 |
| HTTP 오류 | **0건** |
| 완료 사용자 | **125/125명** |
| 성공 사용자 흐름 | **125/125명, 100%** |
| 일반 비즈니스 API p95 | **87.65ms** |
| 로그인 평균 | 2.21초 |
| 로그인 p95 | **4.10초** |
| 전체 평균 RPS | 90.40 |
| health 오류 | 0건 |

일반 API의 0.3초 기준과 기능 성공 조건은 통과했다. 다만 로그인 p95 300ms threshold를 넘었기 때문에 k6 프로세스 종료 코드는 99였다. 따라서 125명은 **기능 및 일반 API 기준 통과**, **로그인 응답시간 기준 실패**로 구분해야 한다.

### 5.2 130명 단계

| 항목 | 결과 |
|---|---:|
| 계획된 시나리오 API 호출 | 13,000건 |
| 로그인 포함 비즈니스 요청 | 12,930건 |
| health 포함 전체 HTTP 요청 | 13,070건 |
| HTTP 오류 | **5건** |
| 완료 사용자 | **128/130명** |
| 성공 사용자 흐름 | **125/130명, 96.15%** |
| 일반 비즈니스 API p95 | **143.28ms** |
| 로그인 평균 | 2.29초 |
| 로그인 p95 | **4.18초** |
| 전체 평균 RPS | 92.45 |
| health 오류 | 0건 |

130명은 일반 API p95 자체는 0.3초 이내였지만 기능 오류 5건이 발생했으므로 실패다. 최초 실패 단계에서 중단한다는 원칙에 따라 150명 이상은 실행하지 않았다.

## 6. 130명 실패 원인

130명의 로그인 시작이 10초 구간에 집중되면서 API Hikari 풀 30개가 모두 사용됐다. 서버 로그에서 최대 `waiting=30`이 확인됐으며 대기 요청이 약 3~5초 후 timeout으로 실패했다.

```text
modera-api-hikari-prod - Connection is not available
total=30, active=30, idle=0, waiting=30
request timed out after 3~5 seconds
```

실패 요청은 다음과 같다.

| API | 실패 수 | 대표 응답시간 |
|---|---:|---:|
| `POST /api/v1/auth/login` | 2건 | 3.09초, 4.68초 |
| `GET /api/v1/images` | 1건 | 5.03초 |
| `GET /api/v1/user` | 1건 | 3.09초 |
| `GET /api/v1/images/{imageId}` | 1건 | 3.10초 |

병목은 특정 이미지 목록 쿼리 하나가 아니라 **동시 로그인 시점의 DB 커넥션 점유와 전체 API의 커넥션 획득 경쟁**이다. Hikari를 20에서 30으로 늘린 설정은 배포됐지만, 130명이 10초 동안 진입하는 순간 부하에서는 여전히 풀이 포화된다.

## 7. 최종 판정

| 측정 대상 | 최대 통과 | 최초 실패 | 제한 요인 |
|---|---:|---:|---|
| 이미지 목록 60초 부하 | **400 RPS** | **450 RPS** | 약 420 RPS부터 지연·오류·dropped 증가 |
| 이미지 목록 30초 과부하 | **400 RPS** | **450 RPS** | DB 커넥션 경쟁과 응답 지연 |
| 2분 사용자 흐름 기능 안정성 | **125명** | **130명** | 동시 로그인 구간 Hikari timeout |
| 로그인 p95 300ms | 통과 단계 없음 | 125명부터 실패 | BCrypt·JWT·DB 접근이 겹치는 로그인 burst |

테스트 종료 후 `modera-api`는 재시작되지 않았고 Actuator health에서 DB, Redis, liveness, readiness가 모두 `UP`이었다.

## 8. 서버 결과 위치

```text
/home/ubuntu/k6/results/image-authopt-confirm
/home/ubuntu/k6/results/image-authopt-overload-confirm
/home/ubuntu/k6/results/user-flow-authopt-2m-confirm-20260803
/home/ubuntu/k6/results/user-flow-authopt-2m-level-130-20260803
```

재현용 스크립트는 `ops/k6/run-progressive-user-capacity.sh`와 서버의 `/home/ubuntu/k6` 디렉터리에 있다.
