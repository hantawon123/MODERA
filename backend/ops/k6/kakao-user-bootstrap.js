import http from 'k6/http';
import exec from 'k6/execution';
import { check } from 'k6';
import { Counter, Rate } from 'k6/metrics';

const baseUrl = __ENV.BASE_URL || 'http://modera-api:8080';
const userCount = Number(__ENV.USER_COUNT || 200);
const userOffset = Number(__ENV.USER_OFFSET || 1_000_000);
const bootstrapVus = Math.min(Number(__ENV.BOOTSTRAP_VUS || 20), userCount);
const tokenPrefix = __ENV.KAKAO_TOKEN_PREFIX || 'modera-perf-kakao';

const bootstrapSuccess = new Rate('kakao_bootstrap_success');
const bootstrappedUsers = new Counter('kakao_bootstrapped_users');

export const options = {
  scenarios: {
    bootstrap: {
      executor: 'shared-iterations',
      vus: bootstrapVus,
      iterations: userCount,
      maxDuration: __ENV.MAX_DURATION || '5m',
    },
  },
  thresholds: {
    kakao_bootstrap_success: ['rate==1'],
    'http_req_failed{kind:kakao_bootstrap}': ['rate==0'],
  },
};

export default function () {
  const userNumber = userOffset + exec.scenario.iterationInTest + 1;
  const response = http.post(`${baseUrl}/api/v1/auth/kakao/login`, JSON.stringify({
    kakaoAccessToken: `${tokenPrefix}-${userNumber}`,
    deviceId: `k6-kakao-device-${userNumber}`,
  }), {
    headers: { 'Content-Type': 'application/json' },
    tags: { kind: 'kakao_bootstrap' },
    timeout: '10s',
  });

  const ok = check(response, {
    'Kakao test user is ready': r => r.status === 200
      && r.json('result') === 'SUCCESS'
      && Boolean(r.json('data.accessToken')),
  }, { kind: 'kakao_bootstrap' });
  bootstrapSuccess.add(ok);
  if (ok) bootstrappedUsers.add(1);
}
