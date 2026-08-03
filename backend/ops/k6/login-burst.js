// 로그인 버스트 재현 스크립트.
//
// 혼합 시나리오 테스트에서 130명 유입 시 관측된 Hikari 커넥션 고갈
// (CannotCreateTransactionException)의 메커니즘 검증용이다. login()이
// @Transactional 안에서 bcrypt를 수행하므로, 로그인 유입률 × 커넥션 점유시간이
// 풀 크기를 넘는 순간 획득 대기 → 3초 타임아웃 → 오류로 이어진다.
//
// open model(constant-arrival-rate)로 유입률을 직접 제어한다. 응답이 늦어도
// 유입은 계속되므로 실제 "동시 접속 폭주"와 같은 압력이 걸린다.
//
// 사용 예:
//   k6 run -e RATE=250 -e DURATION=90s login-burst.js
//   k6 run -e RATE=50 login-burst.js            # 낮은 유입부터 단계 탐색
//
// 관측 포인트(Grafana):
//   hikaricp pending > 0        → 풀 대기 시작
//   획득 실패/분 > 0            → CannotCreateTransactionException 발생 지점
//   커넥션 점유시간(usage) avg  → bcrypt-in-tx 가설의 직접 증거

import http from 'k6/http';
import { check, fail } from 'k6';
import { Trend } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const RATE = Number(__ENV.RATE || 200);
const DURATION = __ENV.DURATION || '90s';
const LOGIN_ID = __ENV.LOGIN_ID || 'k6-burst-user';
const PASSWORD = __ENV.PASSWORD || 'password123';

const loginDuration = new Trend('login_duration', true);

export const options = {
  scenarios: {
    login_burst: {
      executor: 'constant-arrival-rate',
      rate: RATE,
      timeUnit: '1s',
      duration: DURATION,
      preAllocatedVUs: Math.min(RATE * 2, 500),
      maxVUs: Math.min(RATE * 4, 1000),
    },
  },
  thresholds: {
    // 관찰용 실험이므로 통과/실패 게이트가 아니라 요약 지표 확인용이다.
    http_req_failed: ['rate<0.01'],
    login_duration: ['p(95)<1000'],
  },
};

export function setup() {
  const headers = { 'Content-Type': 'application/json' };
  const register = http.post(`${BASE_URL}/api/v1/auth/register`,
    JSON.stringify({ loginId: LOGIN_ID, password: PASSWORD, email: `${LOGIN_ID}@example.com` }),
    { headers, responseCallback: http.expectedStatuses(200, 409) });
  if (register.status !== 200 && register.status !== 409) {
    fail(`burst 계정 준비 실패: ${register.status} ${register.body}`);
  }
}

export default function () {
  const res = http.post(`${BASE_URL}/api/v1/auth/login`, JSON.stringify({
    loginId: LOGIN_ID,
    password: PASSWORD,
    deviceId: `k6-burst-${__VU}`,
  }), {
    headers: { 'Content-Type': 'application/json' },
    tags: { name: 'POST /api/v1/auth/login' },
    timeout: '10s',
  });
  loginDuration.add(res.timings.duration);
  check(res, {
    'login 200': r => r.status === 200,
    'no pool timeout(500)': r => r.status !== 500,
  });
}
