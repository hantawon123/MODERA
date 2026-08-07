import http from 'k6/http';
import { check, fail } from 'k6';
import exec from 'k6/execution';

const baseUrl = __ENV.BASE_URL || 'http://modera-api:8080';
const rate = Number(__ENV.RATE || 200);
const duration = __ENV.DURATION || '60s';
const userOffset = Number(__ENV.USER_OFFSET || 10000000);
const userCount = Number(__ENV.USER_COUNT || 4000);
const tokenPrefix = __ENV.KAKAO_TOKEN_PREFIX || 'modera-perf-kakao';

if (!Number.isInteger(rate) || rate < 1) throw new Error('RATE must be a positive integer');
if (!Number.isInteger(userOffset) || userOffset < 0) throw new Error('USER_OFFSET must be a non-negative integer');
if (!Number.isInteger(userCount) || userCount < 1) throw new Error('USER_COUNT must be a positive integer');

const preAllocatedVUs = Number(
  __ENV.PRE_ALLOCATED_VUS || Math.max(100, Math.ceil(rate * 0.75)),
);
const maxVUs = Number(
  __ENV.MAX_VUS || Math.max(400, Math.ceil(rate * 2)),
);

export const options = {
  scenarios: {
    kakao_login: {
      executor: 'constant-arrival-rate',
      rate,
      timeUnit: '1s',
      duration,
      preAllocatedVUs,
      maxVUs,
      gracefulStop: '10s',
    },
  },
  thresholds: {
    'http_req_duration{kind:kakao_login}': ['p(95)<=300'],
    'http_req_failed{kind:kakao_login}': ['rate<0.01'],
    'checks{kind:kakao_login}': ['rate>=0.99'],
    dropped_iterations: ['count==0'],
  },
};

function login(userNumber, setup = false) {
  const response = http.post(`${baseUrl}/api/v1/auth/kakao/login`, JSON.stringify({
    kakaoAccessToken: `${tokenPrefix}-${userNumber}`,
    deviceId: `k6-kakao-device-${userNumber}`,
  }), {
    headers: { 'Content-Type': 'application/json' },
    tags: { kind: setup ? 'setup' : 'kakao_login' },
    timeout: '10s',
  });

  if (setup) return response;

  check(response, {
    'Kakao login status is 200': r => r.status === 200,
    'Kakao login envelope succeeded': r => r.json('result') === 'SUCCESS',
    'Kakao login returned access token': r => Boolean(r.json('data.accessToken')),
  }, { kind: 'kakao_login' });
  return response;
}

export function setup() {
  const response = login(userOffset + 1, true);
  if (response.status !== 200 || response.json('result') !== 'SUCCESS') {
    fail(`Kakao login setup failed: ${response.status} ${response.body}`);
  }
}

export default function () {
  // iterationInTest is scenario-wide, so concurrent VUs share neither a single
  // test account nor a single refresh-token row until all prepared users cycle.
  const index = Number(exec.scenario.iterationInTest) % userCount;
  login(userOffset + 1 + index);
}
