# 실제 사용자 흐름 기반 동시 사용자 성능 테스트

## 구성 파일

- `common.js`: 테스트 계정 가입·로그인과 공통 환경변수
- `mixed-user-scenarios.js`: 40개 사용자 유형과 실제 화면 이동 흐름
- `run-progressive-user-capacity.sh`: 동시 사용자 단계를 순서대로 실행하고 최초 기능 오류에서 중단

## 테스트 동작

각 가상 사용자는 다음과 같이 동작한다.

1. 10초 유입 구간 안에서 앱에 접속한다.
2. 로그인 API를 정확히 한 번 호출하고 발급받은 Access Token을 계속 사용한다.
3. 5분 동안 비즈니스 API를 200회 호출한다.
4. 평균 호출 간격은 약 1.5초다.
5. 응답이 느려져 호출 일정이 밀려도 누락된 요청을 한꺼번에 보내지 않는다.

화면 이동 묶음은 다음 API를 실제 앱 탐색 순서로 사용한다.

- 사용자 정보 → 카테고리 → 최근 이미지 → 이미지 상세
- 카테고리 → 이미지 1·2페이지 → 이미지 상세
- 키워드 검색 → 검색 결과 상세 → 유사 이미지 → 즐겨찾기 목록
- 최신·과거·제목순 갤러리 → 이미지 상세
- 일정 목록 → 캘린더 미등록 일정 → 이미지 목록
- 문서 목록 → 문서 상세 → 문서 구성 이미지

40개 사용자 프로필은 위 이동 묶음의 시작 위치와 전용 행동을 다르게 배정받는다. 동일 사용자가 반복해서 로그인하거나 AI 분석을 요청하지 않는다.

## 제외 범위

- 이미지 등록 및 MinIO 업로드
- Redis 이미지 분석 이벤트
- Analysis Worker
- 실제 AI 호출
- 문서 생성·재생성처럼 AI를 실행하는 API

## 사전 조건

- Docker가 설치되어 있어야 한다.
- 테스트 대상과 같은 Docker 네트워크에서 `grafana/k6` 이미지를 실행할 수 있어야 한다.
- 결과 요약을 위해 호스트에 `jq`가 설치되어 있어야 한다.
- 테스트 계정에 조회 가능한 카테고리와 분석 완료 이미지가 최소 한 건 필요하다.
- 문서 데이터가 없으면 문서 상세 흐름은 문서 목록 조회로 안전하게 대체된다.

## 서버 배치

세 파일을 같은 디렉터리에 둔다. 서버의 기본 위치는 `/home/ubuntu/k6`다.

```bash
chmod +x /home/ubuntu/k6/run-progressive-user-capacity.sh
```

## 단일 사용자 단계 실행

60명을 한 번만 측정한다.

```bash
LEVELS="60" \
BASE_URL="http://modera-api-old:8080" \
RESULT_DIR="/home/ubuntu/k6/results/user-flow-60" \
/home/ubuntu/k6/run-progressive-user-capacity.sh
```

## 2분 빠른 점진 테스트

기본 5분·200회 대신 단계별 2분만 확인하려면 사용자당 100회로 실행한다. 시나리오는 `CALLS_PER_SESSION`을 최소 100으로 제한한다.

```bash
LEVELS="125 130 150 175 200" \
BASE_URL="http://modera-api:8080" \
RESULT_DIR="/home/ubuntu/k6/results/user-flow-latest-2m" \
CALLS_PER_SESSION=100 \
SESSION_SECONDS=120 \
STARTUP_SPREAD_SECONDS=10 \
LIVENESS_DURATION="2m20s" \
MAX_DURATION="3m" \
/home/ubuntu/k6/run-progressive-user-capacity.sh
```

이 조건의 평균 호출 간격은 약 1.2초이므로 기본 5분·200회 조건의 약 1.5초보다 25% 더 강하다.

## 점진적 용량 탐색

각 단계는 5분 동안 실행된다. HTTP 오류, 실패 사용자 플로우 또는 미완료 사용자가 처음 발생한 단계에서 자동 중단한다.

```bash
LEVELS="60 80 100 120 125 130 150" \
BASE_URL="http://modera-api-old:8080" \
RESULT_DIR="/home/ubuntu/k6/results/user-flow-progressive" \
/home/ubuntu/k6/run-progressive-user-capacity.sh
```

Docker 네트워크나 스크립트 위치가 다르면 환경변수로 지정한다.

```bash
NETWORK="infra_default" \
SCRIPT_DIR="/home/ubuntu/k6" \
LEVELS="40 60 80" \
BASE_URL="http://modera-api:8080" \
RESULT_DIR="/home/ubuntu/k6/results/custom" \
/home/ubuntu/k6/run-progressive-user-capacity.sh
```

## 테스트 계정 변경

기본값은 다음과 같다.

```text
LOGIN_ID=k6tester
PASSWORD=password123
EMAIL=k6tester@example.com
```

다른 계정은 `common.js`를 수정하지 않고 실행 환경에서 주입한다.

```bash
LOGIN_ID="performance-user" \
PASSWORD="change-me" \
EMAIL="performance-user@example.com" \
LEVELS="60" \
/home/ubuntu/k6/run-progressive-user-capacity.sh
```

## 결과 파일

`RESULT_DIR` 아래에 다음 파일이 생성된다.

- `capacity.tsv`: 단계별 핵심 결과
- `vus-{사용자수}.json`: k6 전체 요약 지표
- `vus-{사용자수}.log`: 사람이 읽을 수 있는 상세 결과

`capacity.tsv` 열은 다음 의미다.

| 열 | 의미 |
|---|---|
| `vus` | 동시 사용자 수 |
| `planned_calls` | 사용자 수 × 200회 |
| `business_requests` | 로그인까지 포함한 실제 비즈니스 요청 수 |
| `error_requests` | HTTP 오류 요청 수 |
| `completed_flows` | 200회 호출을 끝까지 실행한 사용자 수 |
| `failed_flows` | 한 번 이상 실패한 사용자 수 |
| `business_p95_ms` | 전체 비즈니스 API p95 |
| `login_p95_ms` | 로그인 API p95 |
| `rps` | 전체 평균 HTTP 처리량 |
| `k6_exit` | k6 종료 코드. 로그인 300ms 기준만 초과해도 99가 될 수 있음 |

기능 용량 경계는 `k6_exit`만으로 판단하지 않는다. 실행 스크립트는 실제 오류 요청, 실패 플로우, 미완료 플로우를 기준으로 다음 단계 진행 여부를 판단한다.

## 현재 측정 결과

동일 조건에서 확인된 결과는 다음과 같다.

- 최대 무오류 확인 단계: 125명
- 최초 기능 오류 단계: 130명
- 125명: 시나리오 API 25,000회, 오류 0, p95 25.96ms
- 130명: 실제 시나리오 API 24,400회, 오류 요청 31회, 실패 사용자 31명
- 주된 오류: Hikari DB 커넥션 획득 실패에 따른 `CannotCreateTransactionException`
