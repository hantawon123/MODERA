import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://modera-api-old:8080';
const ASSET_URL = __ENV.ASSET_URL || 'http://modera-test-assets:8080';
const USER_COUNT = Number(__ENV.USER_COUNT || 40);
const MIN_IMAGES = Number(__ENV.MIN_IMAGES || 1);
const MAX_IMAGES = Number(__ENV.MAX_IMAGES || 5);
const USER_OFFSET = Number(__ENV.USER_OFFSET || 100000);
const POLL_SECONDS = Number(__ENV.POLL_SECONDS || 3);
const ANALYSIS_TIMEOUT_MS = Number(__ENV.ANALYSIS_TIMEOUT_MS || 900000);
const requests = JSON.parse(open('/data/register-requests.json')).images;

export const options = {
  scenarios: {
    actual_users: {
      executor: 'per-vu-iterations',
      vus: USER_COUNT,
      iterations: 1,
      maxDuration: __ENV.MAX_DURATION || '20m',
    },
  },
  thresholds: {
    flow_success: ['rate==1'],
    analysis_failed: ['count==0'],
    analysis_timeout: ['count==0'],
  },
};

const flowSuccess = new Rate('flow_success');
const loginDuration = new Trend('flow_login_duration', true);
const registrationDuration = new Trend('flow_registration_duration', true);
const assetFetchDuration = new Trend('flow_asset_fetch_duration', true);
const uploadDuration = new Trend('flow_upload_duration', true);
const analysisDuration = new Trend('flow_analysis_duration', true);
const totalDuration = new Trend('flow_total_duration', true);
const registeredCount = new Counter('images_registered');
const duplicatedCount = new Counter('images_duplicated');
const uploadedCount = new Counter('images_uploaded');
const completedCount = new Counter('analysis_completed');
const failedCount = new Counter('analysis_failed');
const timeoutCount = new Counter('analysis_timeout');

function uuid(seed) {
  const suffix = `${Date.now().toString(16)}${seed.toString(16)}${Math.floor(Math.random() * 0xffffffff).toString(16)}`
    .padEnd(32, '0').slice(0, 32);
  return `${suffix.slice(0, 8)}-${suffix.slice(8, 12)}-4${suffix.slice(13, 16)}-a${suffix.slice(17, 20)}-${suffix.slice(20, 32)}`;
}

function authHeaders(token) {
  return { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` };
}

function mimeType(fileName) {
  const extension = fileName.toLowerCase().split('.').pop();
  if (extension === 'png') return 'image/png';
  if (extension === 'webp') return 'image/webp';
  return 'image/jpeg';
}

function pickUnique(count) {
  const indices = requests.map((_, index) => index);
  for (let index = indices.length - 1; index > 0; index -= 1) {
    const target = Math.floor(Math.random() * (index + 1));
    [indices[index], indices[target]] = [indices[target], indices[index]];
  }
  return indices.slice(0, count).map(index => requests[index]);
}

export default function () {
  const flowStarted = Date.now();
  let localFailures = 0;
  const userNumber = USER_OFFSET + __VU;
  const imageCount = MIN_IMAGES + Math.floor(Math.random() * (MAX_IMAGES - MIN_IMAGES + 1));

  const loginStarted = Date.now();
  const login = http.post(`${BASE_URL}/api/v1/auth/kakao/login`, JSON.stringify({
    kakaoAccessToken: `modera-perf-kakao-${userNumber}`,
    deviceId: `k6-real-ai-${userNumber}`,
  }), { headers: { 'Content-Type': 'application/json' }, tags: { kind: 'kakao_login' }, timeout: '10s' });
  loginDuration.add(Date.now() - loginStarted);
  const token = login.json('data.accessToken');
  if (!check(login, { 'Kakao login/signup succeeded': response => response.status === 200 && Boolean(token) })) {
    flowSuccess.add(false);
    totalDuration.add(Date.now() - flowStarted);
    return;
  }

  const selected = pickUnique(Math.min(imageCount, requests.length));
  const requestById = {};
  const registrationItems = selected.map((source, index) => {
    const clientRequestId = uuid(userNumber * 100 + index);
    requestById[clientRequestId] = source;
    return { ...source, clientRequestId };
  });
  const registrationStarted = Date.now();
  const registration = http.post(`${BASE_URL}/api/v1/images`, JSON.stringify({ images: registrationItems }), {
    headers: authHeaders(token), tags: { kind: 'image_registration' }, timeout: '30s',
  });
  registrationDuration.add(Date.now() - registrationStarted);
  const registered = registration.json('data.registered') || [];
  const duplicated = registration.json('data.duplicated') || [];
  const rejected = registration.json('data.failed') || [];
  registeredCount.add(registered.length);
  duplicatedCount.add(duplicated.length);
  if (!check(registration, {
    'image registration batch succeeded': response => response.status === 200,
    'all requested images accepted': () => registered.length + duplicated.length === selected.length && rejected.length === 0,
  })) {
    flowSuccess.add(false);
    totalDuration.add(Date.now() - flowStarted);
    return;
  }

  const pending = [];
  for (const item of registered) {
    const source = requestById[item.clientRequestId];
    const assetStarted = Date.now();
    const asset = http.get(`${ASSET_URL}/${encodeURIComponent(source.fileName)}`, {
      tags: { kind: 'test_asset_fetch' }, timeout: '30s', responseType: 'binary',
    });
    assetFetchDuration.add(Date.now() - assetStarted);
    if (!check(asset, { 'matching test image loaded': response => response.status === 200 && response.body.byteLength === source.fileSize })) {
      failedCount.add(1);
      localFailures += 1;
      continue;
    }
    const uploadStarted = Date.now();
    const upload = http.put(item.presignedURL, asset.body, {
      headers: { 'Content-Type': mimeType(source.fileName) }, tags: { kind: 'minio_upload' }, timeout: '60s',
    });
    uploadDuration.add(Date.now() - uploadStarted);
    if (check(upload, { 'presigned MinIO upload succeeded': response => response.status >= 200 && response.status < 300 })) {
      uploadedCount.add(1);
      pending.push({ imageId: item.imageId, startedAt: Date.now() });
    } else {
      failedCount.add(1);
      localFailures += 1;
    }
  }

  for (const item of duplicated) {
    pending.push({ imageId: item.imageId, startedAt: Date.now(), duplicated: true });
  }

  const deadline = Date.now() + ANALYSIS_TIMEOUT_MS;
  while (pending.length > 0 && Date.now() < deadline) {
    for (let index = pending.length - 1; index >= 0; index -= 1) {
      const item = pending[index];
      const detail = http.get(`${BASE_URL}/api/v1/images/${item.imageId}`, {
        headers: authHeaders(token), tags: { kind: 'analysis_poll' }, timeout: '10s',
        responseCallback: http.expectedStatuses(200, 404, 409),
      });
      if (detail.status === 200) {
        analysisDuration.add(Date.now() - item.startedAt, { duplicated: String(Boolean(item.duplicated)) });
        completedCount.add(1);
        pending.splice(index, 1);
      } else if (detail.status !== 409) {
        failedCount.add(1);
        localFailures += 1;
        pending.splice(index, 1);
      }
    }
    if (pending.length > 0) sleep(POLL_SECONDS);
  }
  if (pending.length > 0) timeoutCount.add(pending.length);

  const success = rejected.length === 0 && localFailures === 0 && pending.length === 0;
  const expected = registered.length + duplicated.length;
  const completedForFlow = expected - pending.length;
  flowSuccess.add(success && completedForFlow === expected);
  totalDuration.add(Date.now() - flowStarted);
}
