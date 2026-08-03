import http from 'k6/http';
import exec from 'k6/execution';
import { check, sleep } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';

const baseUrl = __ENV.BASE_URL || 'http://modera-api:8080';
const vus = Number(__ENV.VUS || 125);
const callsPerSession = Number(__ENV.CALLS_PER_SESSION || 100);
const sessionSeconds = Number(__ENV.SESSION_SECONDS || 120);
const startupSpreadSeconds = Number(__ENV.STARTUP_SPREAD_SECONDS || 10);
const userOffset = Number(__ENV.USER_OFFSET || 1_000_000);
const tokenPrefix = __ENV.KAKAO_TOKEN_PREFIX || 'modera-perf-kakao';
const livenessDuration = __ENV.LIVENESS_DURATION || '2m20s';
const maxDuration = __ENV.MAX_DURATION || '3m';

if (callsPerSession < 1) throw new Error('CALLS_PER_SESSION must be positive');

const flowSuccess = new Rate('kakao_flow_success');
const flowExecutions = new Counter('kakao_flow_executions');
const sessionSteps = new Trend('kakao_session_steps');
const flowDuration = new Trend('kakao_flow_duration', true);

export const options = {
  scenarios: {
    users: {
      executor: 'per-vu-iterations',
      exec: 'userJourney',
      vus,
      iterations: 1,
      maxDuration,
      gracefulStop: '15s',
    },
    liveness: {
      executor: 'constant-vus',
      exec: 'liveness',
      vus: 1,
      duration: livenessDuration,
      gracefulStop: '5s',
    },
  },
  thresholds: {
    'http_req_duration{kind:app}': ['p(95)<=300'],
    'http_req_failed{kind:app}': ['rate<0.01'],
    'checks{kind:app}': ['rate>=0.99'],
    'http_req_duration{kind:kakao_auth}': ['p(95)<=1000'],
    'http_req_failed{kind:kakao_auth}': ['rate<0.01'],
    kakao_flow_success: ['rate>=0.99'],
    'http_req_failed{kind:health}': ['rate==0'],
    'checks{kind:health}': ['rate==1'],
    kakao_session_steps: [`min>=${callsPerSession}`],
  },
};

function authHeaders(token) {
  return { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' };
}

function kakaoLogin(userNumber, deviceId, flow) {
  const response = http.post(`${baseUrl}/api/v1/auth/kakao/login`, JSON.stringify({
    kakaoAccessToken: `${tokenPrefix}-${userNumber}`,
    deviceId,
  }), {
    headers: { 'Content-Type': 'application/json' },
    tags: { kind: 'kakao_auth', flow, step: 'kakao_login' },
    timeout: '10s',
  });
  const ok = check(response, {
    'Kakao login succeeded': r => r.status === 200
      && r.json('result') === 'SUCCESS'
      && Boolean(r.json('data.accessToken')),
  }, { kind: 'kakao_auth', flow, step: 'kakao_login' });
  return { response, ok };
}

function appRequest(token, method, path, body, flow, step) {
  const response = http.request(method, `${baseUrl}${path}`, body ? JSON.stringify(body) : null, {
    headers: authHeaders(token),
    tags: { kind: 'app', flow, step },
    timeout: '30s',
  });
  const ok = check(response, {
    'app response is 200': r => r.status === 200,
    'app envelope succeeded': r => r.json('result') === 'SUCCESS',
  }, { kind: 'app', flow, step });
  return ok;
}

const flows = [
  { name: '01_home', steps: [
    ['GET', '/api/v1/user', null, 'profile'],
    ['GET', '/api/v1/categories?sort=IMAGE_COUNT_DESC', null, 'categories'],
    ['GET', '/api/v1/images?page=0&size=20&sort=UPLOADED_DESC', null, 'recent_images'],
  ] },
  { name: '02_gallery', steps: [
    ['GET', '/api/v1/images?page=0&size=20&sort=TITLE_ASC', null, 'title_sort'],
    ['GET', '/api/v1/images?page=1&size=20&sort=UPLOADED_ASC', null, 'next_page'],
    ['GET', '/api/v1/images?favorite=true&page=0&size=20', null, 'favorites'],
  ] },
  { name: '03_search', steps: [
    ['GET', '/api/v1/images?keyword=test&page=0&size=20', null, 'keyword'],
    ['GET', '/api/v1/categories?sort=NAME_ASC', null, 'category_names'],
    ['GET', '/api/v1/images?page=0&size=10&sort=UPLOADED_DESC', null, 'gallery_return'],
  ] },
  { name: '04_schedule', steps: [
    ['GET', '/api/v1/schedules?page=0&size=20&sort=START_ASC', null, 'upcoming'],
    ['GET', '/api/v1/schedules?calendared=false&page=0&size=20&sort=START_ASC', null, 'uncalendared'],
    ['GET', '/api/v1/schedules?calendared=true&page=0&size=20&sort=START_DESC', null, 'calendared'],
  ] },
  { name: '05_document', steps: [
    ['GET', '/api/v1/documents?page=0&size=20&sort=UPDATED_DESC', null, 'recent_documents'],
    ['GET', '/api/v1/documents?page=0&size=20&sort=NAME_ASC', null, 'document_names'],
    ['GET', '/api/v1/documents?page=1&size=20&sort=UPDATED_ASC', null, 'older_documents'],
  ] },
];

export function setup() {
  const healthUser = userOffset;
  const login = kakaoLogin(healthUser, `k6-kakao-health-${healthUser}`, 'health_setup');
  if (!login.ok) throw new Error('mock Kakao health user login failed');
  return { healthToken: login.response.json('data.accessToken') };
}

export function userJourney() {
  // liveness 시나리오도 별도 VU를 사용하므로 전역 __VU 값에 의존하지 않는다.
  // per-vu-iterations의 고유 iteration 번호로 1..VUS 계정을 정확히 배정한다.
  const userIndex = exec.scenario.iterationInTest + 1;
  const userNumber = userOffset + userIndex;
  const deviceId = `k6-kakao-device-${userNumber}`;
  const selectedFlow = flows[(userIndex - 1) % flows.length];
  sleep(Math.random() * startupSpreadSeconds);

  const login = kakaoLogin(userNumber, deviceId, selectedFlow.name);
  if (!login.ok) {
    flowSuccess.add(false, { flow: selectedFlow.name });
    return;
  }

  const token = login.response.json('data.accessToken');
  const started = Date.now();
  const intervalMs = sessionSeconds * 1000 / callsPerSession;
  let ok = true;

  for (let index = 0; index < callsPerSession; index += 1) {
    const [method, path, body, step] = selectedFlow.steps[index % selectedFlow.steps.length];
    ok = appRequest(token, method, path, body, selectedFlow.name, step) && ok;

    const jitterMs = (Math.random() - 0.5) * intervalMs * 0.6;
    const nextCallAt = started + ((index + 1) * intervalMs) + jitterMs;
    const remainingMs = nextCallAt - Date.now();
    if (remainingMs > 0) sleep(remainingMs / 1000);
  }

  flowExecutions.add(1, { flow: selectedFlow.name });
  sessionSteps.add(callsPerSession, { flow: selectedFlow.name });
  flowDuration.add(Date.now() - started, { flow: selectedFlow.name });
  flowSuccess.add(ok, { flow: selectedFlow.name });
}

export function liveness(data) {
  const response = http.get(`${baseUrl}/api/v1/categories?sort=NAME_ASC`, {
    headers: authHeaders(data.healthToken),
    tags: { kind: 'health' },
    timeout: '5s',
  });
  check(response, {
    'server remains alive': r => r.status === 200 && r.json('result') === 'SUCCESS',
  }, { kind: 'health' });
  sleep(1);
}
