import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';
import { authenticate, BASE_URL, LOGIN_ID, PASSWORD } from './common.js';

const vus = Number(__ENV.VUS || 40);
const callsPerSession = Number(__ENV.CALLS_PER_SESSION || 200);
const sessionSeconds = Number(__ENV.SESSION_SECONDS || 300);
const startupSpreadSeconds = Number(__ENV.STARTUP_SPREAD_SECONDS || 10);
const maxDuration = __ENV.MAX_DURATION || '7m';
const livenessDuration = __ENV.LIVENESS_DURATION || '5m20s';
const thinkTime = Number(__ENV.THINK_TIME || 0);

if (callsPerSession < 100) {
  throw new Error('CALLS_PER_SESSION must be at least 100');
}

const flowSuccess = new Rate('mixed_flow_success');
const flowDuration = new Trend('mixed_flow_duration', true);
const flowExecutions = new Counter('mixed_flow_executions');
const sessionSteps = new Trend('mixed_session_steps');
let vuAccessToken = null;

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
    'http_req_duration{kind:business}': ['p(95)<=300'],
    'http_req_failed{kind:business}': ['rate<0.01'],
    'http_req_duration{step:session_login}': ['p(95)<=300'],
    'http_req_failed{step:session_login}': ['rate<0.01'],
    'checks{kind:business}': ['rate>=0.99'],
    'mixed_flow_success': ['rate>=0.99'],
    'http_req_failed{kind:health}': ['rate==0'],
    'checks{kind:health}': ['rate==1'],
    mixed_session_steps: [`min>=${callsPerSession}`],
  },
};

function authHeaders(token) {
  return { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' };
}

function request(token, method, path, body, flowName, stepName) {
  const response = http.request(method, `${BASE_URL}${path}`, body, {
    headers: authHeaders(token),
    tags: { kind: 'business', flow: flowName, step: stepName },
    timeout: '30s',
  });
  const ok = check(response, {
    'business status is 200': (r) => r.status === 200,
    'business envelope succeeded': (r) => r.json('result') === 'SUCCESS',
  }, { kind: 'business', flow: flowName, step: stepName });
  sleep(thinkTime * (0.5 + Math.random() * 1.5));
  return { response, ok };
}

function loginAsVirtualUser(flowName) {
  if (vuAccessToken) return vuAccessToken;
  const response = http.post(`${BASE_URL}/api/v1/auth/login`, JSON.stringify({
    loginId: LOGIN_ID,
    password: PASSWORD,
    deviceId: `k6-mixed-vu-${__VU}`,
  }), {
    headers: { 'Content-Type': 'application/json' },
    tags: { kind: 'business', flow: flowName, step: 'session_login' },
    timeout: '10s',
  });
  const ok = check(response, {
    'virtual user login succeeded': (r) => r.status === 200 && r.json('result') === 'SUCCESS',
  }, { kind: 'business', flow: flowName, step: 'session_login' });
  if (!ok) return null;
  vuAccessToken = response.json('data.accessToken');
  sleep(thinkTime * (0.5 + Math.random()));
  return vuAccessToken;
}

function getJson(token, path) {
  const response = http.get(`${BASE_URL}${path}`, {
    headers: authHeaders(token),
    tags: { kind: 'setup' },
  });
  return response.status === 200 ? response.json() : null;
}

export function setup() {
  const auth = authenticate();
  const token = auth.accessToken;
  const categories = getJson(token, '/api/v1/categories?sort=IMAGE_COUNT_DESC');
  const categoryId = categories?.data?.list?.[0]?.categoryId;
  const categoryQuery = categoryId ? `&categoryId=${categoryId}` : '';
  const images = getJson(token, `/api/v1/images?page=0&size=20${categoryQuery}`);
  const imageIds = (images?.data?.list || []).map((item) => item.imageId).slice(0, 5);
  const documents = getJson(token, '/api/v1/documents?page=0&size=20&sort=UPDATED_DESC');
  const documentIds = (documents?.data?.list || []).map((item) => item.documentId).slice(0, 3);
  const schedules = getJson(token, '/api/v1/schedules?page=0&size=20&sort=START_ASC');
  const scheduleIds = (schedules?.data?.list || []).map((item) => item.scheduleId).slice(0, 3);

  if (!categoryId || imageIds.length === 0) {
    throw new Error('mixed scenario fixture requires at least one category and image');
  }
  return { token, categoryId, imageIds, documentIds, scheduleIds };
}

const I = (index = 0) => (data) => data.imageIds[index % data.imageIds.length];
const D = (index = 0) => (data) => data.documentIds[index % Math.max(1, data.documentIds.length)];
const S = (index = 0) => (data) => data.scheduleIds[index % Math.max(1, data.scheduleIds.length)];
const C = (data) => data.categoryId;

const get = (name, path) => ({ name, method: 'GET', path });
const put = (name, path, body) => ({ name, method: 'PUT', path, body });

const sessionPrelude = [
  get('session_profile', () => '/api/v1/user'),
  get('session_categories', (_, index) =>
    `/api/v1/categories?sort=${['NAME_ASC', 'UPDATED_DESC', 'IMAGE_COUNT_DESC'][index % 3]}`),
  get('session_category_images', (d, index) =>
    `/api/v1/images?page=${index % 2}&size=20&categoryId=${C(d)}`),
];

// Coherent in-app navigation cycles. A virtual user logs in once, then revisits
// screens as a real user would; authentication and AI-triggering APIs are never
// repeated as filler calls.
const journeyCycles = [
  [
    get('home_profile', () => '/api/v1/user'),
    get('home_categories', () => '/api/v1/categories?sort=IMAGE_COUNT_DESC'),
    get('home_recent_images', (d) => `/api/v1/images?page=0&size=20&categoryId=${C(d)}`),
    get('home_image_detail', (d) => `/api/v1/images/${I(0)(d)}`),
  ],
  [
    get('category_list', () => '/api/v1/categories?sort=NAME_ASC'),
    get('category_first_page', (d) => `/api/v1/images?page=0&size=20&categoryId=${C(d)}`),
    get('category_second_page', (d) => `/api/v1/images?page=1&size=20&categoryId=${C(d)}`),
    get('category_image_detail', (d) => `/api/v1/images/${I(1)(d)}`),
  ],
  [
    get('search_keyword', (d) => `/api/v1/images?keyword=test&page=0&size=20&categoryId=${C(d)}`),
    get('search_result_detail', (d) => `/api/v1/images/${I(2)(d)}`),
    get('search_related_images', (d) => `/api/v1/images/${I(2)(d)}/similar`),
    get('search_favorite_filter', (d) => `/api/v1/images?favorite=true&page=0&size=20&categoryId=${C(d)}`),
  ],
  [
    get('gallery_recent', (d) => `/api/v1/images?page=0&size=20&sort=UPLOADED_DESC&categoryId=${C(d)}`),
    get('gallery_oldest', (d) => `/api/v1/images?page=0&size=20&sort=UPLOADED_ASC&categoryId=${C(d)}`),
    get('gallery_title_sort', (d) => `/api/v1/images?page=0&size=20&sort=TITLE_ASC&categoryId=${C(d)}`),
    get('gallery_image_detail', (d) => `/api/v1/images/${I(3)(d)}`),
  ],
  [
    get('schedule_upcoming', () => '/api/v1/schedules?page=0&size=20&sort=START_ASC'),
    get('schedule_recent', () => '/api/v1/schedules?page=0&size=20&sort=START_DESC'),
    get('schedule_uncalendared', () => '/api/v1/schedules?calendared=false&page=0&size=20&sort=START_ASC'),
    get('schedule_back_to_images', (d) => `/api/v1/images?page=0&size=10&categoryId=${C(d)}`),
  ],
  [
    get('document_recent', () => '/api/v1/documents?page=0&size=20&sort=UPDATED_DESC'),
    get('document_name_sort', () => '/api/v1/documents?page=0&size=20&sort=NAME_ASC'),
    get('document_detail', (d) => d.documentIds.length ? `/api/v1/documents/${D(0)(d)}` : '/api/v1/documents?page=0&size=20'),
    get('document_images', (d) => d.documentIds.length ? `/api/v1/documents/${D(0)(d)}/images?page=0&size=20` : '/api/v1/documents?page=0&size=20'),
  ],
];

function expandToMinimumCalls(baseSteps, flowIndex) {
  const steps = [...baseSteps];
  let cycleIndex = flowIndex;
  while (steps.length < callsPerSession) {
    const cycle = journeyCycles[cycleIndex % journeyCycles.length];
    for (const step of cycle) {
      if (steps.length >= callsPerSession) break;
      steps.push(step);
    }
    cycleIndex += 1;
  }
  return steps.slice(0, callsPerSession);
}

function sessionEpilogue(index) {
  switch (index % 5) {
    case 0:
      return [
        get('session_schedules', () => '/api/v1/schedules?page=0&size=10&sort=START_ASC'),
        get('session_documents', () => '/api/v1/documents?page=0&size=10&sort=UPDATED_DESC'),
      ];
    case 1:
      return [
        get('session_detail', (d) => `/api/v1/images/${I(0)(d)}`),
        get('session_favorites', (d) => `/api/v1/images?favorite=true&page=0&size=10&categoryId=${C(d)}`),
        get('session_documents', () => '/api/v1/documents?page=0&size=10&sort=NAME_ASC'),
      ];
    case 2:
      return [
        get('session_detail', (d) => `/api/v1/images/${I(1)(d)}`),
        get('session_similar', (d) => `/api/v1/images/${I(1)(d)}/similar`),
        get('session_schedules', () => '/api/v1/schedules?calendared=true&page=0&size=10'),
      ];
    case 3:
      return [
        get('session_next_page', (d) => `/api/v1/images?page=1&size=20&categoryId=${C(d)}`),
        get('session_schedule', () => '/api/v1/schedules?calendared=false&page=0&size=10'),
        get('session_profile_again', () => '/api/v1/user'),
      ];
    default:
      return [
        get('session_keyword', (d) => `/api/v1/images?keyword=test&page=0&size=10&categoryId=${C(d)}`),
        get('session_documents', () => '/api/v1/documents?page=0&size=10&sort=UPDATED_ASC'),
        get('session_categories_again', () => '/api/v1/categories?sort=IMAGE_COUNT_DESC'),
      ];
  }
}

// Forty intentionally different app journeys. They reuse stable fixture data so
// a capacity run measures backend concurrency rather than test-data creation.
const flows = [
  ['01_app_open', [get('profile', () => '/api/v1/user'), get('categories', () => '/api/v1/categories')]],
  ['02_home_recent', [get('profile', () => '/api/v1/user'), get('recent_images', () => '/api/v1/images?page=0&size=20')]],
  ['03_category_open', [get('categories', () => '/api/v1/categories'), get('category_images', (d) => `/api/v1/images?page=0&size=20&categoryId=${C(d)}`)]],
  ['04_category_count_sort', [get('categories_count', () => '/api/v1/categories?sort=IMAGE_COUNT_DESC'), get('category_images', (d) => `/api/v1/images?page=0&size=20&categoryId=${C(d)}`)]],
  ['05_category_updated_sort', [get('categories_updated', () => '/api/v1/categories?sort=UPDATED_DESC'), get('category_images', (d) => `/api/v1/images?page=0&size=20&categoryId=${C(d)}`)]],
  ['06_recent_detail', [get('recent', () => '/api/v1/images?page=0&size=20'), get('detail', (d) => `/api/v1/images/${I(0)(d)}`)]],
  ['07_category_detail', [get('category', (d) => `/api/v1/images?page=0&size=20&categoryId=${C(d)}`), get('detail', (d) => `/api/v1/images/${I(1)(d)}`)]],
  ['08_oldest_detail', [get('oldest', (d) => `/api/v1/images?page=0&size=20&sort=UPLOADED_ASC&categoryId=${C(d)}`), get('detail', (d) => `/api/v1/images/${I(2)(d)}`)]],
  ['09_title_sort_detail', [get('title_sort', (d) => `/api/v1/images?page=0&size=20&sort=TITLE_ASC&categoryId=${C(d)}`), get('detail', (d) => `/api/v1/images/${I(3)(d)}`)]],
  ['10_second_page', [get('page_0', (d) => `/api/v1/images?page=0&size=20&categoryId=${C(d)}`), get('page_1', (d) => `/api/v1/images?page=1&size=20&categoryId=${C(d)}`)]],
  ['11_small_page_browse', [get('page_0', (d) => `/api/v1/images?page=0&size=10&categoryId=${C(d)}`), get('page_1', (d) => `/api/v1/images?page=1&size=10&categoryId=${C(d)}`)]],
  ['12_large_page_browse', [get('large_page', (d) => `/api/v1/images?page=0&size=50&categoryId=${C(d)}`)]],
  ['13_favorites', [get('favorites', (d) => `/api/v1/images?favorite=true&page=0&size=20&categoryId=${C(d)}`)]],
  ['14_non_favorites', [get('non_favorites', (d) => `/api/v1/images?favorite=false&page=0&size=20&categoryId=${C(d)}`)]],
  ['15_keyword_search', [get('keyword', (d) => `/api/v1/images?keyword=performance&page=0&size=20&categoryId=${C(d)}`)]],
  ['16_search_then_detail', [get('keyword', (d) => `/api/v1/images?keyword=test&page=0&size=20&categoryId=${C(d)}`), get('detail', (d) => `/api/v1/images/${I(0)(d)}`)]],
  ['17_detail_then_related', [get('detail', (d) => `/api/v1/images/${I(0)(d)}`), get('similar', (d) => `/api/v1/images/${I(0)(d)}/similar`)]],
  ['18_related_from_second', [get('detail', (d) => `/api/v1/images/${I(1)(d)}`), get('similar', (d) => `/api/v1/images/${I(1)(d)}/similar`)]],
  ['19_favorite_on', [get('detail', (d) => `/api/v1/images/${I(2)(d)}`), put('favorite_on', (d) => `/api/v1/images/${I(2)(d)}/favorite`, () => ({ favorite: true }))]],
  ['20_favorite_off', [get('detail', (d) => `/api/v1/images/${I(3)(d)}`), put('favorite_off', (d) => `/api/v1/images/${I(3)(d)}/favorite`, () => ({ favorite: false }))]],
  ['21_favorite_roundtrip', [put('favorite_on', (d) => `/api/v1/images/${I(4)(d)}/favorite`, () => ({ favorite: true })), get('favorites', (d) => `/api/v1/images?favorite=true&page=0&size=20&categoryId=${C(d)}`)]],
  ['22_schedule_home', [get('schedules', () => '/api/v1/schedules?page=0&size=20&sort=START_ASC')]],
  ['23_schedule_desc', [get('schedules_desc', () => '/api/v1/schedules?page=0&size=20&sort=START_DESC')]],
  ['24_schedule_calendared', [get('calendared', () => '/api/v1/schedules?calendared=true&page=0&size=20&sort=START_ASC')]],
  ['25_schedule_uncalendared', [get('uncalendared', () => '/api/v1/schedules?calendared=false&page=0&size=20&sort=START_ASC')]],
  ['26_schedule_date_range', [get('date_range', () => '/api/v1/schedules?from=2025-01-01T00%3A00%3A00Z&to=2030-12-31T23%3A59%3A59Z&page=0&size=20&sort=START_ASC')]],
  ['27_calendar_then_images', [get('schedules', () => '/api/v1/schedules?page=0&size=10'), get('images', (d) => `/api/v1/images?page=0&size=10&categoryId=${C(d)}`)]],
  ['28_document_home', [get('documents', () => '/api/v1/documents?page=0&size=20&sort=UPDATED_DESC')]],
  ['29_document_name_sort', [get('documents_name', () => '/api/v1/documents?page=0&size=20&sort=NAME_ASC')]],
  ['30_document_oldest', [get('documents_oldest', () => '/api/v1/documents?page=0&size=20&sort=UPDATED_ASC')]],
  ['31_document_detail', [get('documents', () => '/api/v1/documents?page=0&size=20'), get('document_detail', (d) => d.documentIds.length ? `/api/v1/documents/${D(0)(d)}` : '/api/v1/documents?page=0&size=20')]],
  ['32_document_images', [get('document_detail', (d) => d.documentIds.length ? `/api/v1/documents/${D(0)(d)}` : '/api/v1/documents?page=0&size=20'), get('document_images', (d) => d.documentIds.length ? `/api/v1/documents/${D(0)(d)}/images?page=0&size=20` : '/api/v1/documents?page=0&size=20')]],
  ['33_document_images_oldest', [get('document_images', (d) => d.documentIds.length ? `/api/v1/documents/${D(1)(d)}/images?page=0&size=20&sort=ADDED_ASC` : '/api/v1/documents?page=0&size=20')]],
  ['34_document_images_title', [get('document_images', (d) => d.documentIds.length ? `/api/v1/documents/${D(2)(d)}/images?page=0&size=20&sort=TITLE_ASC` : '/api/v1/documents?page=0&size=20')]],
  ['35_documents_then_category', [get('documents', () => '/api/v1/documents?page=0&size=10'), get('categories', () => '/api/v1/categories?sort=IMAGE_COUNT_DESC'), get('images', (d) => `/api/v1/images?page=0&size=10&categoryId=${C(d)}`)]],
  ['36_profile_then_schedule', [get('profile', () => '/api/v1/user'), get('schedules', () => '/api/v1/schedules?page=0&size=10')]],
  ['37_profile_then_documents', [get('profile', () => '/api/v1/user'), get('documents', () => '/api/v1/documents?page=0&size=10')]],
  ['38_full_browse', [get('profile', () => '/api/v1/user'), get('categories', () => '/api/v1/categories'), get('images', (d) => `/api/v1/images?page=0&size=20&categoryId=${C(d)}`), get('detail', (d) => `/api/v1/images/${I(0)(d)}`)]],
  ['39_organizer_browse', [get('schedules', () => '/api/v1/schedules?page=0&size=10'), get('documents', () => '/api/v1/documents?page=0&size=10'), get('favorites', (d) => `/api/v1/images?favorite=true&page=0&size=10&categoryId=${C(d)}`)]],
  ['40_power_user', [get('profile', () => '/api/v1/user'), get('categories', () => '/api/v1/categories?sort=UPDATED_DESC'), get('images', (d) => `/api/v1/images?page=0&size=20&categoryId=${C(d)}`), get('detail', (d) => `/api/v1/images/${I(1)(d)}`), get('schedules', () => '/api/v1/schedules?page=0&size=10'), get('documents', () => '/api/v1/documents?page=0&size=10')]],
];

export function userJourney(data) {
  const flowIndex = (__VU - 1 + __ITER) % flows.length;
  const [flowName, coreSteps] = flows[flowIndex];
  // Users enter over a short window instead of producing an artificial
  // millisecond-level login spike; all remain active together for five minutes.
  sleep(Math.random() * startupSpreadSeconds);
  const token = loginAsVirtualUser(flowName);
  const started = Date.now();
  let ok = token !== null;
  if (!token) {
    flowSuccess.add(false, { flow: flowName });
    return;
  }
  const sessionData = { ...data, token };
  const steps = expandToMinimumCalls(
    [...sessionPrelude, ...coreSteps, ...sessionEpilogue(flowIndex)],
    flowIndex,
  );
  const intervalMs = sessionSeconds * 1000 / steps.length;
  for (let index = 0; index < steps.length; index += 1) {
    const step = steps[index];
    const path = step.path(sessionData, flowIndex);
    const body = step.body ? JSON.stringify(step.body(sessionData, flowIndex)) : null;
    ok = request(token, step.method, path, body, flowName, step.name).ok && ok;
    const nextCallAt = started + ((index + 1) * intervalMs);
    const remainingMs = nextCallAt - Date.now();
    if (remainingMs > 0) sleep(remainingMs / 1000);
  }
  flowExecutions.add(1, { flow: flowName });
  flowDuration.add(Date.now() - started, { flow: flowName });
  sessionSteps.add(steps.length, { flow: flowName });
  flowSuccess.add(ok, { flow: flowName });
}

export function liveness(data) {
  const response = http.get(`${BASE_URL}/api/v1/categories`, {
    headers: authHeaders(data.token),
    tags: { kind: 'health' },
    timeout: '5s',
  });
  check(response, {
    'server remains alive': (r) => r.status === 200 && r.json('result') === 'SUCCESS',
  }, { kind: 'health' });
  sleep(1);
}
