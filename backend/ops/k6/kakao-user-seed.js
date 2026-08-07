// 성능 테스트 사용자 데이터 시딩.
//
// 이미 bootstrap된 mock Kakao 사용자들에게 실제 업로드 파이프라인
// (등록 API → MinIO PUT → webhook → worker → mock AI 콜백)으로 이미지를
// 채운다. SQL 직접 INSERT 대신 실경로를 쓰는 이유: category_view.image_count
// 캐시(043), read model(JSONB·배열·벡터), 스키마 경계 등 불변식이 전부
// 실코드로 보장되기 때문이다. 데이터 분포(카테고리·키워드·일정 비율)는
// ops/mock-ai/server.py의 mock_analysis_result가 imageId 기반으로 결정한다.
//
// 사용자당 이미지 수는 userNumber로 결정적으로 차등 배정한다(재실행해도 동일):
//   userIndex % 10 < 5  → 라이트(IMAGES_LIGHT, 기본 50장)
//   userIndex % 10 < 8  → 미들(IMAGES_MEDIUM, 기본 200장)
//   나머지              → 헤비(IMAGES_HEAVY, 기본 500장)
//
// 유입 속도: 전역 목표 SEED_RATE(기본 25 jobs/s)를 SEED_VUS로 나눠 VU별로
// 페이싱한다. 원격 파이프라인 검증치(35 jobs/s) 아래에서 돌리고, 러너가
// 스트림 lag으로 밀림 여부를 검증한다.
//
// 각 사용자 마무리 단계에서 이미지 10%에 즐겨찾기를 켜고, 분석 완료가 확인된
// 앞쪽 이미지 3장으로 문서 1개를 만든다(DOCS_PER_USER, mock AI documents
// 엔드포인트 필요 — 확장된 ops/mock-ai/server.py).
//
// 사용 예 (서버):
//   k6 run -e USERS=200 -e USER_OFFSET=3000000 kakao-user-seed.js
// 사용 예 (로컬 스모크, mock Kakao 없이 로컬 계정으로):
//   k6 run -e AUTH_MODE=local -e USERS=3 -e IMAGES_LIGHT=5 -e IMAGES_MEDIUM=8 \
//          -e IMAGES_HEAVY=12 -e SEED_RATE=5 kakao-user-seed.js

import http from 'k6/http';
import crypto from 'k6/crypto';
import exec from 'k6/execution';
import { check, fail, sleep } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const AUTH_MODE = __ENV.AUTH_MODE || 'kakao';
const USERS = Number(__ENV.USERS || 200);
const USER_OFFSET = Number(__ENV.USER_OFFSET || 3_000_000);
const TOKEN_PREFIX = __ENV.KAKAO_TOKEN_PREFIX || 'modera-perf-kakao';
const SEED_VUS = Number(__ENV.SEED_VUS || 10);
const SEED_RATE = Number(__ENV.SEED_RATE || 25);
const IMAGES_LIGHT = Number(__ENV.IMAGES_LIGHT || 50);
const IMAGES_MEDIUM = Number(__ENV.IMAGES_MEDIUM || 200);
const IMAGES_HEAVY = Number(__ENV.IMAGES_HEAVY || 500);
const FAVORITE_RATIO = Number(__ENV.FAVORITE_RATIO || 0.1);
const DOCS_PER_USER = Number(__ENV.DOCS_PER_USER || 1);
const DOC_POLL_TIMEOUT_MS = Number(__ENV.DOC_POLL_TIMEOUT_MS || 20000);
const MAX_DURATION = __ENV.MAX_DURATION || '90m';
const PASSWORD = __ENV.PASSWORD || 'password123';

const uploadsOk = new Counter('seed_uploads_ok');
const uploadsFailed = new Counter('seed_uploads_failed');
const favoritesOk = new Counter('seed_favorites_ok');
const documentsOk = new Counter('seed_documents_ok');
const documentsSkipped = new Counter('seed_documents_skipped');
const userSuccess = new Rate('seed_user_success');
const userDuration = new Trend('seed_user_duration', true);

// VU별 페이싱: 전역 SEED_RATE를 병렬 VU 수로 나눈다.
const perImageIntervalMs = 1000 / (SEED_RATE / SEED_VUS);

// 업로드 바이트. mock 경로는 오브젝트 내용을 읽지 않으므로 합성 바이트로 충분하다.
const sourceBytes = (() => {
  const size = Number(__ENV.SYNTH_IMAGE_KB || 16) * 1024;
  const bytes = new Uint8Array(size);
  for (let i = 0; i < size; i += 1) bytes[i] = (i * 31 + 7) & 0xff;
  return bytes;
})();

export const options = {
  scenarios: {
    seed: {
      executor: 'shared-iterations',
      vus: Math.min(SEED_VUS, USERS),
      iterations: USERS,
      maxDuration: MAX_DURATION,
      exec: 'seedUser',
    },
  },
  thresholds: {
    seed_user_success: ['rate>=0.99'],
    'http_req_failed{kind:seed}': ['rate<0.01'],
  },
};

function uuid() {
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, c => {
    const r = Math.floor(Math.random() * 16);
    return (c === 'x' ? r : (r & 3) | 8).toString(16);
  });
}

function authHeaders(token) {
  return { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' };
}

function imagesFor(userIndex) {
  const bucket = userIndex % 10;
  if (bucket < 5) return IMAGES_LIGHT;
  if (bucket < 8) return IMAGES_MEDIUM;
  return IMAGES_HEAVY;
}

function login(userNumber) {
  const headers = { 'Content-Type': 'application/json' };
  if (AUTH_MODE === 'local') {
    const loginId = `k6seed-${userNumber}`;
    const register = http.post(`${BASE_URL}/api/v1/auth/register`,
      JSON.stringify({ loginId, password: PASSWORD, email: `${loginId}@example.com` }),
      { headers, tags: { kind: 'seed', step: 'register' }, responseCallback: http.expectedStatuses(200, 409) });
    if (register.status !== 200 && register.status !== 409) {
      fail(`seed register failed: ${register.status}`);
    }
    const res = http.post(`${BASE_URL}/api/v1/auth/login`, JSON.stringify({
      loginId, password: PASSWORD, deviceId: `k6-seed-${userNumber}`,
    }), { headers, tags: { kind: 'seed', step: 'login' } });
    if (res.status !== 200) fail(`seed login failed: ${res.status} ${res.body}`);
    return res.json('data.accessToken');
  }
  const res = http.post(`${BASE_URL}/api/v1/auth/kakao/login`, JSON.stringify({
    kakaoAccessToken: `${TOKEN_PREFIX}-${userNumber}`,
    deviceId: `k6-kakao-device-${userNumber}`,
  }), { headers, tags: { kind: 'seed', step: 'kakao_login' }, timeout: '10s' });
  if (res.status !== 200) fail(`kakao seed login failed: ${res.status} ${res.body}`);
  return res.json('data.accessToken');
}

/** 이미지 1장 등록 + MinIO 업로드. 분석 완료는 기다리지 않는다(파이프라인이 소화). */
function uploadOne(token) {
  const requestId = uuid();
  const marker = new Uint8Array(requestId.length);
  for (let i = 0; i < requestId.length; i += 1) marker[i] = requestId.charCodeAt(i) & 0xff;
  const bytes = new Uint8Array(sourceBytes.length + marker.length);
  bytes.set(sourceBytes);
  bytes.set(marker, sourceBytes.length);

  const registration = http.post(`${BASE_URL}/api/v1/images`, JSON.stringify({
    images: [{
      clientRequestId: requestId,
      fileName: `seed-${requestId}.jpg`,
      contentHash: crypto.sha256(bytes.buffer, 'hex'),
      fileSize: bytes.byteLength,
      ocr: { rawText: `seed performance ${requestId}` },
    }],
  }), { headers: authHeaders(token), tags: { kind: 'seed', step: 'register_image' }, timeout: '10s' });
  const item = registration.json('data.registered.0');
  if (registration.status !== 200 || !item) {
    uploadsFailed.add(1);
    return null;
  }
  const upload = http.put(item.presignedURL, bytes.buffer, {
    headers: { 'Content-Type': 'image/jpeg' },
    tags: { kind: 'seed', step: 'minio_upload' },
    timeout: '30s',
  });
  if (upload.status < 200 || upload.status >= 300) {
    uploadsFailed.add(1);
    return null;
  }
  uploadsOk.add(1);
  return item.imageId;
}

function markFavorites(token, imageIds) {
  const target = Math.floor(imageIds.length * FAVORITE_RATIO);
  for (let i = 0; i < target; i += 1) {
    const res = http.put(`${BASE_URL}/api/v1/images/${imageIds[i]}/favorite`,
      JSON.stringify({ favorite: true }),
      { headers: authHeaders(token), tags: { kind: 'seed', step: 'favorite' } });
    if (res.status === 200) favoritesOk.add(1);
  }
}

/** 앞쪽 이미지 3장이 분석 완료(상세 200)되면 문서를 만든다. 미완료면 스킵(시딩 실패 아님). */
function createDocuments(token, imageIds) {
  if (DOCS_PER_USER < 1 || imageIds.length < 3) return;
  const sources = imageIds.slice(0, 3);
  const deadline = Date.now() + DOC_POLL_TIMEOUT_MS;
  let ready = false;
  while (Date.now() < deadline && !ready) {
    ready = sources.every(id => http.get(`${BASE_URL}/api/v1/images/${id}`, {
      headers: authHeaders(token),
      tags: { kind: 'seed', step: 'analysis_check' },
      responseCallback: http.expectedStatuses(200, 409),
      timeout: '5s',
    }).status === 200);
    if (!ready) sleep(1);
  }
  if (!ready) {
    documentsSkipped.add(1);
    return;
  }
  for (let i = 0; i < DOCS_PER_USER; i += 1) {
    const res = http.post(`${BASE_URL}/api/v1/documents`, JSON.stringify({
      clientRequestId: uuid(),
      imageIds: sources,
    }), { headers: authHeaders(token), tags: { kind: 'seed', step: 'document_create' }, timeout: '60s' });
    if (res.status === 200) documentsOk.add(1);
    else documentsSkipped.add(1);
  }
}

export function seedUser() {
  const userIndex = exec.scenario.iterationInTest + 1;
  const userNumber = USER_OFFSET + userIndex;
  const quota = imagesFor(userIndex);
  const started = Date.now();

  const token = login(userNumber);
  const imageIds = [];
  let ok = true;

  for (let i = 0; i < quota; i += 1) {
    const callStarted = Date.now();
    const imageId = uploadOne(token);
    if (imageId) imageIds.push(imageId);
    else ok = false;
    const remaining = perImageIntervalMs - (Date.now() - callStarted);
    if (remaining > 0) sleep(remaining / 1000);
  }

  markFavorites(token, imageIds);
  createDocuments(token, imageIds);

  userDuration.add(Date.now() - started, { quota: String(quota) });
  userSuccess.add(ok && imageIds.length === quota);
}
