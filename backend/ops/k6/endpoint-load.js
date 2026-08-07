import http from 'k6/http';
import { check } from 'k6';
import { authenticate, BASE_URL } from './common.js';

const rate = Number(__ENV.RATE || 50);
const duration = __ENV.DURATION || '60s';
const target = __ENV.TARGET || '/api/v1/categories';
const expectedStatus = Number(__ENV.EXPECTED_STATUS || 200);

export const options = {
  scenarios: {
    endpoint: {
      executor: 'constant-arrival-rate',
      rate,
      timeUnit: '1s',
      duration,
      preAllocatedVUs: Number(__ENV.PRE_ALLOCATED_VUS || Math.max(50, Math.ceil(rate * 0.1))),
      maxVUs: Number(__ENV.MAX_VUS || Math.max(200, Math.ceil(rate * 0.5))),
      gracefulStop: '10s',
    },
  },
  thresholds: {
    'http_req_duration{kind:endpoint}': ['p(95)<=300'],
    'http_req_failed{kind:endpoint}': ['rate<0.01'],
    'checks{kind:endpoint}': ['rate>=0.99'],
    dropped_iterations: ['count==0'],
  },
};

export function setup() {
  return authenticate();
}

export default function (data) {
  const response = http.get(`${BASE_URL}${target}`, {
    headers: { Authorization: `Bearer ${data.accessToken}` },
    tags: { kind: 'endpoint', endpoint: target.split('?')[0] },
    timeout: '30s',
  });
  if (__ENV.DEBUG === 'true' && __ITER === 0) {
    console.log(`DEBUG target=${target} status=${response.status} body=${response.body}`);
  }
  check(response, {
    'endpoint status is expected': (res) => res.status === expectedStatus,
    'endpoint envelope succeeded': (res) => res.json('result') === 'SUCCESS',
  }, { kind: 'endpoint' });
}
