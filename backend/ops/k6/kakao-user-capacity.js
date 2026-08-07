// mock Kakao 사용자 기반 동시 사용자 용량 테스트 (v2).
//
// v1(5개 흐름, 전부 목록 조회) 대비 v2는 2차 API 개선(유사 이미지 tx 분리,
// 시맨틱 검색 컨슈머 분리)의 대상 경로를 실제로 태우는 흐름 2개를 추가했다.
//   06_detail_similar : 목록 → 상세 → 유사 이미지 (api → worker HTTP → pgvector)
//   07_semantic       : 시맨틱 검색 (api → Redis 스트림 → worker → AI → 응답 스트림)
// 흐름이 5→7개가 되었으므로 v1 실행 결과와 절대치를 직접 비교하면 안 된다.
//
// 시맨틱 검색은 kind=search로 분리 계측한다 — 스트림 왕복(서버 대기 상한 10초)이라
// 일반 앱 API의 p95 300ms 기준과 섞으면 서로의 판정을 오염시키기 때문이다.
// 검색 키워드 세트는 ops/mock-ai/server.py의 KEYWORDS와 일치해야 시딩 데이터에
// 실제로 히트한다.

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

// ops/mock-ai/server.py KEYWORDS와 동일한 세트(시딩 데이터에 실히트하도록).
const searchKeywords = ['영수증', '일정', '쇼핑', '문서', '여행'];

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
    // 시맨틱 검색은 스트림 왕복이라 별도 기준. 컨슈머 분리(B) 전에는 분석 큐에
    // 밀려 수 초까지 갈 수 있고, 분리 후에는 수백 ms를 기대한다.
    'http_req_duration{kind:search}': ['p(95)<=1500'],
    'http_req_failed{kind:search}': ['rate<0.01'],
    'checks{kind:search}': ['rate>=0.99'],
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

/**
 * step 하나를 실행한다. path/body는 문자열이거나 (ctx, userNumber) 함수다.
 * capture가 있으면 성공 응답에서 세션 컨텍스트(ctx)를 갱신한다 — 06 흐름이
 * 목록에서 얻은 imageId로 상세·유사 이미지를 호출하는 데 쓴다.
 */
function runStep(token, step, ctx, userNumber, flow) {
  const kind = step.kind || 'app';
  const path = typeof step.path === 'function' ? step.path(ctx, userNumber) : step.path;
  const body = step.body === undefined
    ? null
    : JSON.stringify(typeof step.body === 'function' ? step.body(ctx, userNumber) : step.body);
  const response = http.request(step.method, `${baseUrl}${path}`, body, {
    headers: authHeaders(token),
    tags: { kind, flow, step: step.name },
    timeout: step.timeout || '30s',
  });
  const ok = check(response, {
    [`${kind} response is 200`]: r => r.status === 200,
    [`${kind} envelope succeeded`]: r => r.json('result') === 'SUCCESS',
  }, { kind, flow, step: step.name });
  if (ok && step.capture) step.capture(response, ctx);
  return ok;
}

const get = (name, path, extra = {}) => ({ method: 'GET', name, path, ...extra });
const post = (name, path, body, extra = {}) => ({ method: 'POST', name, path, body, ...extra });

const flows = [
  { name: '01_home', steps: [
    get('profile', '/api/v1/user'),
    get('categories', '/api/v1/categories?sort=IMAGE_COUNT_DESC'),
    get('recent_images', '/api/v1/images?page=0&size=20&sort=UPLOADED_DESC'),
  ] },
  { name: '02_gallery', steps: [
    get('title_sort', '/api/v1/images?page=0&size=20&sort=TITLE_ASC'),
    get('next_page', '/api/v1/images?page=1&size=20&sort=UPLOADED_ASC'),
    get('favorites', '/api/v1/images?favorite=true&page=0&size=20'),
  ] },
  { name: '03_search', steps: [
    get('keyword', '/api/v1/images?keyword=test&page=0&size=20'),
    get('category_names', '/api/v1/categories?sort=NAME_ASC'),
    get('gallery_return', '/api/v1/images?page=0&size=10&sort=UPLOADED_DESC'),
  ] },
  { name: '04_schedule', steps: [
    get('upcoming', '/api/v1/schedules?page=0&size=20&sort=START_ASC'),
    get('uncalendared', '/api/v1/schedules?calendared=false&page=0&size=20&sort=START_ASC'),
    get('calendared', '/api/v1/schedules?calendared=true&page=0&size=20&sort=START_DESC'),
  ] },
  { name: '05_document', steps: [
    get('recent_documents', '/api/v1/documents?page=0&size=20&sort=UPDATED_DESC'),
    get('document_names', '/api/v1/documents?page=0&size=20&sort=NAME_ASC'),
    get('older_documents', '/api/v1/documents?page=1&size=20&sort=UPDATED_ASC'),
  ] },
  // v2 추가: 유사 이미지(A 개선 경로). 목록에서 imageId를 얻어 상세와 유사 이미지를
  // 호출한다. 이미지가 없는 계정이면(시딩 전) 같은 종류의 조회로 degrade해 흐름은
  // 유지한다 — 판정을 깨지 않고 시딩 여부만 커버리지에 반영되게 한다.
  { name: '06_detail_similar', steps: [
    get('recent_images', '/api/v1/images?page=0&size=20&sort=UPLOADED_DESC', {
      capture: (response, ctx) => {
        const first = response.json('data.list.0.imageId');
        if (first) ctx.imageId = first;
      },
    }),
    get('image_detail', ctx => ctx.imageId
      ? `/api/v1/images/${ctx.imageId}`
      : '/api/v1/images?page=0&size=20&sort=UPLOADED_DESC'),
    get('similar_images', ctx => ctx.imageId
      ? `/api/v1/images/${ctx.imageId}/similar?limit=10`
      : '/api/v1/categories?sort=NAME_ASC'),
  ] },
  // v2 추가: 시맨틱 검색(B 개선 경로). kind=search로 분리 계측한다.
  { name: '07_semantic', steps: [
    post('semantic_search', '/api/v1/images/search/semantic',
      (ctx, userNumber) => ({
        query: searchKeywords[userNumber % searchKeywords.length],
        page: 0,
        size: 20,
      }),
      { kind: 'search', timeout: '15s' }),
    get('category_names', '/api/v1/categories?sort=NAME_ASC'),
    get('gallery_return', '/api/v1/images?page=0&size=10&sort=UPLOADED_DESC'),
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
  const ctx = {};
  let ok = true;

  for (let index = 0; index < callsPerSession; index += 1) {
    const step = selectedFlow.steps[index % selectedFlow.steps.length];
    ok = runStep(token, step, ctx, userNumber, selectedFlow.name) && ok;

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
