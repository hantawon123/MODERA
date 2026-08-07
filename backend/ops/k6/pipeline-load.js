import { Counter, Rate, Trend } from 'k6/metrics';
import { authenticate } from './common.js';
import { executePipeline } from './pipeline-common.js';

const rate = Number(__ENV.RATE || 1);
const duration = __ENV.DURATION || '30s';
const metrics = { duration: new Trend('pipeline_duration', true), success: new Rate('pipeline_success'),
  completed: new Counter('pipeline_completed'), timeouts: new Counter('pipeline_timeouts') };
export const options = { scenarios: { pipeline: { executor: 'constant-arrival-rate', rate, timeUnit: '1s', duration,
  preAllocatedVUs: Math.max(20, rate * 2), maxVUs: Math.max(100, rate * 10), gracefulStop: '15s' } },
  thresholds: { pipeline_duration: ['p(95)<=300'], pipeline_success: ['rate>=0.99'],
    pipeline_timeouts: ['count==0'], dropped_iterations: ['count==0'] } };
export function setup() { return authenticate(); }
export default function(data) { executePipeline(data, metrics); }
