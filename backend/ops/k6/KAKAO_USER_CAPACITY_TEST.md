# Mock Kakao 기반 사용자 용량 테스트

## 목적

실제 Kakao 계정과 Access Token을 사용하지 않으면서 다음 백엔드 경로를 그대로 측정한다.

1. `POST /api/v1/auth/kakao/login`
2. Kakao 토큰 앱 ID 검증
3. Kakao 사용자 정보 조회
4. 최초 사용자 생성 또는 기존 사용자 조회
5. JWT 발급과 Refresh Token UPSERT
6. 사용자별 Spring API 100회 호출

mock 서버가 대체하는 것은 Kakao의 두 HTTP 응답뿐이다. MODERA API, PostgreSQL, Redis와 JWT 코드는 실제 배포 코드를 사용한다.

## 파일

- `mock-kakao/`: Kakao 토큰 정보·사용자 정보 API의 테스트 전용 구현
- `docker-compose.mock-kakao.yml`: 기존 Docker 네트워크에 mock 서버를 올리는 구성
- `kakao-user-bootstrap.js`: 서로 다른 Kakao 사용자를 미리 가입시키는 k6 스크립트
- `kakao-user-capacity.js`: VU별 고유 사용자로 로그인하고 앱 API를 호출하는 스크립트
- `run-kakao-user-capacity.sh`: 사용자 사전 생성과 점진 테스트 실행기

## 사용자 식별 규칙

기본 `USER_OFFSET=1000000`일 때 VU 1은 다음 값을 사용한다.

```text
mock token: modera-perf-kakao-1000001
Kakao ID:   7000001000001
email:      perf-kakao-1000001@perf.modera.test
device ID:  k6-kakao-device-1000001
```

VU마다 Kakao ID, 이메일, device ID가 다르므로 동일 사용자 행과 Refresh Token 행을 공유하지 않는다.

## 서버 배치

`ops/k6`의 내용을 서버 `/home/ubuntu/k6`에 복사했다고 가정한다.

```bash
cd /home/ubuntu/k6
K6_NETWORK=infra_default docker compose -f docker-compose.mock-kakao.yml up -d --build
docker inspect --format '{{json .State.Health.Status}}' modera-mock-kakao
```

성능 테스트 대상 `modera-api`에는 다음 환경변수를 주입하고 API 컨테이너만 재생성한다.

```text
KAKAO_API_BASE_URL=http://modera-mock-kakao:8080
KAKAO_ALLOWED_APP_IDS=1525155
```

운영 기본값은 `https://kapi.kakao.com`이다. 위 base URL은 성능 테스트를 실행할 때만 사용하고 실제 서비스 배포에서는 설정하지 않는다.

## 점진 테스트

실행기는 가장 큰 단계만큼 사용자를 먼저 생성한다. 따라서 측정 단계에서는 모두 기존 사용자의 재로그인 경로를 탄다.

```bash
chmod +x /home/ubuntu/k6/run-kakao-user-capacity.sh

LEVELS="125 130 150 175 200" \
BASE_URL="http://modera-api:8080" \
NETWORK="infra_default" \
SCRIPT_DIR="/home/ubuntu/k6" \
RESULT_DIR="/home/ubuntu/k6/results/kakao-users-2m" \
USER_OFFSET=1000000 \
CALLS_PER_SESSION=100 \
SESSION_SECONDS=120 \
STARTUP_SPREAD_SECONDS=10 \
LIVENESS_DURATION="2m20s" \
MAX_DURATION="3m" \
/home/ubuntu/k6/run-kakao-user-capacity.sh
```

각 사용자는 로그인 1회 후 다음 화면 계열 중 하나를 반복 탐색한다.

- 홈: 프로필, 카테고리, 최근 이미지
- 갤러리: 정렬, 페이지 이동, 즐겨찾기 필터
- 검색: 키워드 목록과 카테고리
- 일정: 전체, 캘린더 등록·미등록 목록
- 문서: 최신순, 이름순, 과거순 목록

신규 계정에는 분석 완료 이미지가 없으므로 이미지 상세·즐겨찾기 변경·AI 분석은 이 시나리오에 포함하지 않는다. 사용자별 이미지 데이터가 준비되면 별도 데이터 보유 사용자 시나리오로 확장한다.

## 최초 가입 경로만 측정

사용한 적 없는 `USER_OFFSET`을 선택하고 bootstrap만 직접 실행한다.

```bash
docker run --rm --network infra_default \
  -v /home/ubuntu/k6:/scripts:ro \
  grafana/k6 run \
  -e BASE_URL=http://modera-api:8080 \
  -e USER_COUNT=200 \
  -e USER_OFFSET=2000000 \
  -e BOOTSTRAP_VUS=20 \
  /scripts/kakao-user-bootstrap.js
```

## 결과

`RESULT_DIR`에 다음 파일이 생성된다.

- `bootstrap.log`, `bootstrap.json`: 사용자 준비 결과
- `vus-{인원}.log`, `vus-{인원}.json`: 단계별 k6 결과
- `capacity.tsv`: 단계별 핵심 지표

기능 오류, 로그인 오류 또는 미완료 사용자가 처음 발생한 단계에서 자동 중단한다. 일반 앱 API는 p95 300ms, mock Kakao를 포함한 인증은 p95 1초를 기본 threshold로 사용한다.
