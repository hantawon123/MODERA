import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';
import { authenticate, BASE_URL } from './common.js';
import { executePipeline } from './pipeline-common.js';

const rate = Number(__ENV.RATE || 1);
const metrics = { duration: new Trend('pipeline_duration', true), success: new Rate('pipeline_success'),
  completed: new Counter('pipeline_completed'), timeouts: new Counter('pipeline_timeouts') };
export const options = { scenarios: {
  pipeline: { executor: 'constant-arrival-rate', exec: 'pipeline', rate, timeUnit: '1s', duration: '30s',
    preAllocatedVUs: Math.max(20, rate * 2), maxVUs: Math.max(100, rate * 10), gracefulStop: '15s' },
  health: { executor: 'constant-vus', exec: 'health', vus: 1, duration: '45s' } },
  thresholds: { 'checks{kind:health}': ['rate==1'], 'http_req_failed{kind:health}': ['rate==0'],
    dropped_iterations: ['count==0'] } };
export function setup() { return authenticate(); }
export function pipeline(data) { executePipeline(data, metrics); }
export function health(data) {
  const r = http.get(`${BASE_URL}/api/v1/categories`, { headers: { Authorization: `Bearer ${data.accessToken}` },
    tags: { kind: 'health' }, timeout: '5s' });
  check(r, { 'whole backend remains alive': x => x.status === 200 && x.json('result') === 'SUCCESS' }, { kind: 'health' });
  sleep(1);
}
