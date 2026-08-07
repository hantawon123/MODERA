import http from 'k6/http';
import { check } from 'k6';
import { sleep } from 'k6';
import { authenticate, BASE_URL } from './common.js';

const rate = Number(__ENV.RATE || 100);
const target = __ENV.TARGET || '/api/v1/categories';

export const options = {
  scenarios: {
    overload: {
      executor: 'constant-arrival-rate', exec: 'business', rate, timeUnit: '1s', duration: '30s',
      preAllocatedVUs: Number(__ENV.PRE_ALLOCATED_VUS || Math.max(100, Math.ceil(rate * 0.1))),
      maxVUs: Number(__ENV.MAX_VUS || Math.max(500, Math.ceil(rate * 0.5))), gracefulStop: '5s',
    },
    liveness: { executor: 'constant-vus', exec: 'health', vus: 1, duration: '45s', gracefulStop: '5s' },
  },
  thresholds: {
    'http_req_failed{kind:health}': ['rate==0'],
    'checks{kind:health}': ['rate==1'],
    dropped_iterations: ['count==0'],
  },
};

export function setup() { return authenticate(); }

export function business(data) {
  const response = http.get(`${BASE_URL}${target}`, {
    headers: { Authorization: `Bearer ${data.accessToken}` },
    tags: { kind: 'endpoint', endpoint: target.split('?')[0] }, timeout: '30s',
  });
  check(response, { 'business request completed': (res) => res.status > 0 });
}

export function health(data) {
  const response = http.get(`${BASE_URL}/api/v1/categories`, {
    headers: { Authorization: `Bearer ${data.accessToken}` },
    tags: { kind: 'health' }, timeout: '5s',
  });
  check(response, {
    'server remains alive': (res) => res.status === 200 && res.json('result') === 'SUCCESS',
  }, { kind: 'health' });
  sleep(1);
}
