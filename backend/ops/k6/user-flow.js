// E2E 사용자 여정 시나리오.
//
// 한 iteration = 실제 앱 사용자 한 세션:
//   로그인 → 홈 조회 → 스크린샷 업로드 → 분석 완료 대기(폴링) → 상세/연관 조회
//   → 시맨틱 검색 → 즐겨찾기 → 문서 생성/조회 → 일정 조회
//
// 전제 (로컬 기준):
//   - api-server:8080, analysis-worker(스트림 컨슈머), MinIO webhook → api-server 연결
//   - AI는 ops/mock-ai/server.py 로 대체 (worker의 analysis.client=mock 이면
//     분석 경로는 in-JVM mock, 문서 생성·시맨틱 검색만 HTTP mock을 탄다)
//   - 실제 이미지 파일 불필요: mock 경로는 업로드된 오브젝트를 읽지 않으므로
//     합성 바이트를 업로드한다 (fastapi 실연동 시 IMAGE_FILE 로 실제 이미지 지정)
//
// 사용 예:
//   스모크(1 VU 1회):  k6 run -e SMOKE=1 user-flow.js
//   여정 부하:          k6 run -e JOURNEY_VUS=20 -e HOLD=3m user-flow.js
//   여정 + 배경 조회:   k6 run -e JOURNEY_VUS=10 -e BROWSE_RATE=100 user-flow.js
//
// 주요 환경변수:
//   BASE_URL(http://localhost:8080) JOURNEY_VUS(5) RAMP(30s) HOLD(2m)
//   BROWSE_RATE(0=끔) UPLOADS_PER_ITER(1) PIPELINE_TIMEOUT_MS(15000)
//   POLL_SECONDS(0.2) THINK(1, 0이면 대기 없음) SKIP_SEMANTIC(0)
//   IMAGE_FILE(미지정 시 합성 바이트) BROWSE_LOGIN_ID(k6tester)

import http from 'k6/http';
import crypto from 'k6/crypto';
import { check, fail, group, sleep } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const SMOKE = __ENV.SMOKE === '1';
const JOURNEY_VUS = Number(__ENV.JOURNEY_VUS || 5);
const RAMP = __ENV.RAMP || '30s';
const HOLD = __ENV.HOLD || '2m';
const BROWSE_RATE = Number(__ENV.BROWSE_RATE || 0);
const UPLOADS_PER_ITER = Number(__ENV.UPLOADS_PER_ITER || 1);
const PIPELINE_TIMEOUT_MS = Number(__ENV.PIPELINE_TIMEOUT_MS || 15000);
const POLL_SECONDS = Number(__ENV.POLL_SECONDS || 0.2);
const THINK = Number(__ENV.THINK === undefined ? 1 : __ENV.THINK);
const SKIP_SEMANTIC = __ENV.SKIP_SEMANTIC === '1';
const PASSWORD = __ENV.PASSWORD || 'password123';

// 업로드할 바이트. mock 분석 경로는 오브젝트 내용을 읽지 않으므로 기본은 합성 바이트,
// 실연동(fastapi) 검증 시에만 IMAGE_FILE 로 실제 이미지를 준다.
const sourceBytes = (() => {
  if (__ENV.IMAGE_FILE) return new Uint8Array(open(__ENV.IMAGE_FILE, 'b'));
  const size = Number(__ENV.SYNTH_IMAGE_KB || 64) * 1024;
  const bytes = new Uint8Array(size);
  for (let i = 0; i < size; i += 1) bytes[i] = (i * 31 + 7) & 0xff;
  return bytes;
})();

const pipelineDuration = new Trend('pipeline_duration', true);
const pipelineSuccess = new Rate('pipeline_success');
const pipelineTimeouts = new Counter('pipeline_timeouts');

const journeyScenario = SMOKE
  ? { executor: 'per-vu-iterations', vus: 1, iterations: 1, maxDuration: '3m', exec: 'journey' }
  : {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: RAMP, target: JOURNEY_VUS },
        { duration: HOLD, target: JOURNEY_VUS },
        { duration: '15s', target: 0 },
      ],
      gracefulRampDown: '30s',
      exec: 'journey',
    };

export const options = {
  scenarios: {
    user_journey: journeyScenario,
    ...(BROWSE_RATE > 0 && !SMOKE
      ? {
          browse_load: {
            executor: 'constant-arrival-rate',
            rate: BROWSE_RATE,
            timeUnit: '1s',
            duration: HOLD,
            startTime: RAMP, // 여정 램프업이 끝난 뒤 배경 부하 시작
            preAllocatedVUs: Math.max(20, Math.ceil(BROWSE_RATE / 2)),
            maxVUs: Math.max(100, BROWSE_RATE * 2),
            exec: 'browse',
          },
        }
      : {}),
  },
  thresholds: {
    checks: ['rate>0.99'],
    'http_req_failed{scenario:user_journey}': ['rate<0.01'],
    pipeline_duration: ['p(95)<5000'],
    pipeline_success: ['rate>0.99'],
    'http_req_duration{name:POST /api/v1/documents}': ['p(95)<3000'],
    'http_req_duration{name:POST /api/v1/images/search/semantic}': ['p(95)<2500'],
    ...(BROWSE_RATE > 0 && !SMOKE
      ? {
          'http_req_failed{scenario:browse_load}': ['rate<0.01'],
          'http_req_duration{scenario:browse_load}': ['p(95)<300'],
          'dropped_iterations{scenario:browse_load}': ['count==0'],
        }
      : {}),
  },
};

function uuid() {
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, c => {
    const r = Math.floor(Math.random() * 16);
    return (c === 'x' ? r : (r & 3) | 8).toString(16);
  });
}

function authHeaders(token) {
  return { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` };
}

function okJson(res, label) {
  return check(res, {
    [`${label}: 200`]: r => r.status === 200,
    [`${label}: SUCCESS`]: r => r.json('result') === 'SUCCESS',
  });
}

function think() {
  if (THINK > 0) sleep(THINK * (0.5 + Math.random()));
}

// register(409 허용) + login. 반환: accessToken
function login(loginId, deviceId) {
  const headers = { 'Content-Type': 'application/json' };
  const register = http.post(
    `${BASE_URL}/api/v1/auth/register`,
    JSON.stringify({ loginId, password: PASSWORD, email: `${loginId}@example.com` }),
    { headers, tags: { name: 'POST /api/v1/auth/register' }, responseCallback: http.expectedStatuses(200, 409) },
  );
  if (register.status !== 200 && register.status !== 409) fail(`register failed: ${register.status} ${register.body}`);
  const res = http.post(
    `${BASE_URL}/api/v1/auth/login`,
    JSON.stringify({ loginId, password: PASSWORD, deviceId }),
    { headers, tags: { name: 'POST /api/v1/auth/login' } },
  );
  if (!check(res, { 'login: 200': r => r.status === 200 })) fail(`login failed: ${res.status} ${res.body}`);
  return res.json('data.accessToken');
}

// 이미지 1장 등록 → presigned PUT → 분석 완료 폴링. 성공 시 imageId, 실패 시 null.
function uploadAndWait(token) {
  const started = Date.now();
  const requestId = uuid();
  const marker = new Uint8Array(requestId.length);
  for (let i = 0; i < requestId.length; i += 1) marker[i] = requestId.charCodeAt(i) & 0xff;
  const bytes = new Uint8Array(sourceBytes.length + marker.length);
  bytes.set(sourceBytes);
  bytes.set(marker, sourceBytes.length);

  const registration = http.post(
    `${BASE_URL}/api/v1/images`,
    JSON.stringify({
      images: [{
        clientRequestId: requestId,
        fileName: `e2e-${requestId}.jpg`,
        contentHash: crypto.sha256(bytes.buffer, 'hex'),
        fileSize: bytes.byteLength,
        ocr: { rawText: 'GS25 영수증 총액 4,500원 e2e 성능 테스트' },
      }],
    }),
    { headers: authHeaders(token), tags: { name: 'POST /api/v1/images' }, timeout: '5s' },
  );
  const item = registration.json('data.registered.0');
  if (!check(registration, { 'image register: ok': r => r.status === 200 && Boolean(item) })) {
    pipelineSuccess.add(false);
    return null;
  }

  const upload = http.put(item.presignedURL, bytes.buffer, {
    headers: { 'Content-Type': 'image/jpeg' },
    tags: { name: 'PUT presigned-upload' },
    timeout: '10s',
  });
  if (!check(upload, { 'presigned upload: 2xx': r => r.status >= 200 && r.status < 300 })) {
    pipelineSuccess.add(false);
    return null;
  }

  const deadline = Date.now() + PIPELINE_TIMEOUT_MS;
  while (Date.now() < deadline) {
    const res = http.get(`${BASE_URL}/api/v1/images/${item.imageId}`, {
      headers: authHeaders(token),
      tags: { name: 'GET /api/v1/images/{id} (poll)' },
      timeout: '5s',
      responseCallback: http.expectedStatuses(200, 409),
    });
    if (res.status === 200) {
      pipelineDuration.add(Date.now() - started);
      pipelineSuccess.add(true);
      return item.imageId;
    }
    sleep(POLL_SECONDS);
  }
  pipelineTimeouts.add(1);
  pipelineDuration.add(Date.now() - started);
  pipelineSuccess.add(false);
  return null;
}

// ── 여정 시나리오 ──
let vuToken = null;

export function journey() {
  if (!vuToken) vuToken = login(`k6u-${__VU}`, `k6-vu-${__VU}`);
  const token = vuToken;

  group('01_home', () => {
    okJson(http.get(`${BASE_URL}/api/v1/images?page=0&size=20&sort=UPLOADED_DESC`, {
      headers: authHeaders(token), tags: { name: 'GET /api/v1/images' },
    }), 'image list');
    okJson(http.get(`${BASE_URL}/api/v1/categories?sort=NAME_ASC`, {
      headers: authHeaders(token), tags: { name: 'GET /api/v1/categories' },
    }), 'categories');
  });
  think();

  const uploaded = [];
  group('02_upload_pipeline', () => {
    for (let i = 0; i < UPLOADS_PER_ITER; i += 1) {
      const imageId = uploadAndWait(token);
      if (imageId) uploaded.push(imageId);
    }
  });
  think();

  if (uploaded.length > 0) {
    const imageId = uploaded[0];

    group('03_detail_similar', () => {
      okJson(http.get(`${BASE_URL}/api/v1/images/${imageId}`, {
        headers: authHeaders(token), tags: { name: 'GET /api/v1/images/{id}' },
      }), 'image detail');
      // worker 미가동/토큰 불일치 시에도 빈 목록으로 degrade되어 200이 온다.
      okJson(http.get(`${BASE_URL}/api/v1/images/${imageId}/similar?limit=10`, {
        headers: authHeaders(token), tags: { name: 'GET /api/v1/images/{id}/similar' },
      }), 'similar');
    });
    think();

    if (!SKIP_SEMANTIC) {
      group('04_semantic_search', () => {
        // 스트림 왕복(api→worker→AI mock→api). 서버 측 대기 상한 10s.
        okJson(http.post(
          `${BASE_URL}/api/v1/images/search/semantic`,
          JSON.stringify({ query: '편의점 영수증', page: 0, size: 20 }),
          { headers: authHeaders(token), tags: { name: 'POST /api/v1/images/search/semantic' }, timeout: '15s' },
        ), 'semantic search');
      });
      think();
    }

    group('05_favorite', () => {
      okJson(http.put(
        `${BASE_URL}/api/v1/images/${imageId}/favorite`,
        JSON.stringify({ favorite: true }),
        { headers: authHeaders(token), tags: { name: 'PUT /api/v1/images/{id}/favorite' } },
      ), 'favorite');
    });
    think();

    group('06_document', () => {
      // ai.base-url(→ mock)의 동기 문서 생성. 운영은 read timeout 90s인 경로.
      const created = http.post(
        `${BASE_URL}/api/v1/documents`,
        JSON.stringify({ clientRequestId: uuid(), imageIds: uploaded }),
        { headers: authHeaders(token), tags: { name: 'POST /api/v1/documents' }, timeout: '30s' },
      );
      const documentId = created.json('data.documentId');
      if (okJson(created, 'document create') && documentId) {
        okJson(http.get(`${BASE_URL}/api/v1/documents/${documentId}`, {
          headers: authHeaders(token), tags: { name: 'GET /api/v1/documents/{id}' },
        }), 'document detail');
        okJson(http.get(`${BASE_URL}/api/v1/documents/${documentId}/images`, {
          headers: authHeaders(token), tags: { name: 'GET /api/v1/documents/{id}/images' },
        }), 'document images');
      }
    });
    think();
  }

  group('07_schedules', () => {
    okJson(http.get(`${BASE_URL}/api/v1/schedules?page=0&size=20&sort=START_ASC`, {
      headers: authHeaders(token), tags: { name: 'GET /api/v1/schedules' },
    }), 'schedules');
  });
  think();
}

// ── 배경 조회 부하 (open model) ──
// 여정과 별개 계정으로 읽기 트래픽을 깐다. 목록이 비어 있어도 응답 규약/지연 측정은 유효하다.
export function setup() {
  const token = login(__ENV.BROWSE_LOGIN_ID || 'k6tester', 'k6-browse');
  return { browseToken: token };
}

export function browse(data) {
  const token = data.browseToken;
  const dice = Math.random();
  if (dice < 0.4) {
    okJson(http.get(`${BASE_URL}/api/v1/images?page=0&size=20&sort=UPLOADED_DESC`, {
      headers: authHeaders(token), tags: { name: 'GET /api/v1/images' },
    }), 'browse image list');
  } else if (dice < 0.6) {
    okJson(http.get(`${BASE_URL}/api/v1/categories?sort=NAME_ASC`, {
      headers: authHeaders(token), tags: { name: 'GET /api/v1/categories' },
    }), 'browse categories');
  } else if (dice < 0.8) {
    okJson(http.get(`${BASE_URL}/api/v1/documents?page=0&size=20&sort=UPDATED_DESC`, {
      headers: authHeaders(token), tags: { name: 'GET /api/v1/documents' },
    }), 'browse documents');
  } else {
    okJson(http.get(`${BASE_URL}/api/v1/schedules?page=0&size=20&sort=START_ASC`, {
      headers: authHeaders(token), tags: { name: 'GET /api/v1/schedules' },
    }), 'browse schedules');
  }
}
