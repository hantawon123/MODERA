import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';
import { BASE_URL } from './common.js';

const vus = Number(__ENV.VUS || 40);
const pollSeconds = Number(__ENV.ANALYSIS_POLL_SECONDS || 2);
const actionThinkSeconds = Number(__ENV.ACTION_THINK_SECONDS || 0.8);
const assetUrl = __ENV.ASSET_URL || 'http://modera-test-assets:8080';
const analysisTimeoutMs = Number(__ENV.ANALYSIS_TIMEOUT_MS || 900000);
const livenessDuration = __ENV.LIVENESS_DURATION || '15m';
const requests = JSON.parse(open('/data/register-requests.json')).images;

const flowSuccess = new Rate('real_flow_success');
const flowDuration = new Trend('real_flow_duration', true);
const flowExecutions = new Counter('real_flow_executions');
const analysisImages = new Counter('actual_analysis_images');
const analysisWait = new Trend('actual_analysis_wait', true);
const analysisCompleted = new Counter('actual_analysis_completed');
const analysisFailed = new Counter('actual_analysis_failed');
const analysisTimeout = new Counter('actual_analysis_timeout');
const minioUploads = new Counter('actual_minio_uploads');
const sessionSteps = new Trend('real_session_steps');

export const options = {
  scenarios: {
    users: { executor: 'per-vu-iterations', exec: 'userJourney', vus, iterations: 1, maxDuration: '20m' },
    liveness: { executor: 'constant-vus', exec: 'liveness', vus: 1, duration: livenessDuration, gracefulStop: '5s' },
  },
  thresholds: {
    'http_req_duration{kind:business}': ['p(95)<=300'],
    'http_req_failed{kind:business}': ['rate<0.01'],
    'checks{kind:business}': ['rate>=0.99'],
    real_flow_success: ['rate>=0.99'],
    'http_req_failed{kind:health}': ['rate==0'],
    'checks{kind:health}': ['rate==1'],
    actual_analysis_failed: ['count==0'],
    actual_analysis_timeout: ['count==0'],
  },
};

// Forty distinct session profiles. imageCount controls the simulated batch,
// while the other fields vary navigation, polling, filtering, and account actions.
const profiles = [
  ['01_new_light',1,'NAME_ASC','UPLOADED_DESC','all','schedule',false,false],
  ['02_new_two',2,'UPDATED_DESC','UPLOADED_DESC','all','document',true,false],
  ['03_new_three',3,'IMAGE_COUNT_DESC','TITLE_ASC','favorite','organize',false,true],
  ['04_receipt_pair',2,'NAME_ASC','UPLOADED_ASC','keyword','schedule',true,true],
  ['05_document_five',5,'UPDATED_DESC','TITLE_ASC','all','document',false,false],
  ['06_capture_eight',8,'IMAGE_COUNT_DESC','UPLOADED_DESC','all','organize',true,false],
  ['07_single_search',1,'NAME_ASC','UPLOADED_DESC','keyword','browse',false,true],
  ['08_three_favorite',3,'UPDATED_DESC','TITLE_ASC','favorite','browse',true,false],
  ['09_five_schedule',5,'IMAGE_COUNT_DESC','UPLOADED_ASC','all','schedule',false,true],
  ['10_ten_power',10,'UPDATED_DESC','UPLOADED_DESC','keyword','organize',true,true],
  ['11_single_document',1,'NAME_ASC','TITLE_ASC','all','document',false,false],
  ['12_pair_browse',2,'IMAGE_COUNT_DESC','UPLOADED_DESC','favorite','browse',true,false],
  ['13_three_schedule',3,'UPDATED_DESC','UPLOADED_ASC','all','schedule',false,true],
  ['14_five_keyword',5,'NAME_ASC','TITLE_ASC','keyword','document',true,true],
  ['15_eight_organize',8,'IMAGE_COUNT_DESC','UPLOADED_DESC','all','organize',false,false],
  ['16_single_favorite',1,'UPDATED_DESC','UPLOADED_DESC','favorite','browse',true,false],
  ['17_pair_document',2,'NAME_ASC','UPLOADED_ASC','all','document',false,true],
  ['18_three_keyword',3,'IMAGE_COUNT_DESC','TITLE_ASC','keyword','schedule',true,false],
  ['19_five_browse',5,'UPDATED_DESC','UPLOADED_DESC','all','browse',false,true],
  ['20_ten_organize',10,'NAME_ASC','UPLOADED_ASC','favorite','organize',true,true],
  ['21_single_schedule',1,'IMAGE_COUNT_DESC','TITLE_ASC','all','schedule',false,false],
  ['22_pair_keyword',2,'UPDATED_DESC','UPLOADED_DESC','keyword','browse',true,true],
  ['23_three_document',3,'NAME_ASC','UPLOADED_ASC','all','document',false,false],
  ['24_five_favorite',5,'IMAGE_COUNT_DESC','TITLE_ASC','favorite','organize',true,false],
  ['25_eight_schedule',8,'UPDATED_DESC','UPLOADED_DESC','all','schedule',false,true],
  ['26_single_organize',1,'NAME_ASC','UPLOADED_ASC','keyword','organize',true,true],
  ['27_pair_schedule',2,'IMAGE_COUNT_DESC','TITLE_ASC','all','schedule',false,false],
  ['28_three_browse',3,'UPDATED_DESC','UPLOADED_DESC','favorite','browse',true,false],
  ['29_five_document',5,'NAME_ASC','UPLOADED_ASC','all','document',false,true],
  ['30_ten_keyword',10,'IMAGE_COUNT_DESC','TITLE_ASC','keyword','organize',true,true],
  ['31_single_browse',1,'UPDATED_DESC','UPLOADED_DESC','all','browse',false,false],
  ['32_pair_favorite',2,'NAME_ASC','UPLOADED_ASC','favorite','document',true,false],
  ['33_three_organize',3,'IMAGE_COUNT_DESC','TITLE_ASC','all','organize',false,true],
  ['34_five_schedule_alt',5,'UPDATED_DESC','UPLOADED_DESC','keyword','schedule',true,true],
  ['35_eight_document',8,'NAME_ASC','UPLOADED_ASC','all','document',false,false],
  ['36_single_keyword_alt',1,'IMAGE_COUNT_DESC','TITLE_ASC','keyword','browse',true,false],
  ['37_pair_organize',2,'UPDATED_DESC','UPLOADED_DESC','all','organize',false,true],
  ['38_three_favorite_alt',3,'NAME_ASC','UPLOADED_ASC','favorite','schedule',true,true],
  ['39_five_power',5,'IMAGE_COUNT_DESC','TITLE_ASC','all','organize',false,false],
  ['40_ten_power_alt',10,'UPDATED_DESC','UPLOADED_DESC','keyword','document',true,true],
].map(([name,imageCount,categorySort,imageSort,filter,postMode,pushToken,refresh]) => ({
  name,imageCount,categorySort,imageSort,filter,postMode,pushToken,refresh,
}));

function headers(token) {
  return { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' };
}

function business(method, path, token, body, flow, step) {
  const response = http.request(method, `${BASE_URL}${path}`, body ? JSON.stringify(body) : null, {
    headers: headers(token), tags: { kind: 'business', flow, step }, timeout: '30s',
  });
  const ok = check(response, {
    'business response is 200': (r) => r.status === 200,
    'business envelope succeeded': (r) => r.json('result') === 'SUCCESS',
  }, { kind: 'business', flow, step });
  sleep(actionThinkSeconds * (0.5 + Math.random()));
  return { response, ok };
}

function kakaoLogin(profile, userNumber) {
  return business('POST', '/api/v1/auth/kakao/login', null, {
    kakaoAccessToken: `modera-perf-kakao-${userNumber}`,
    deviceId: `k6-kakao-device-${userNumber}`,
  }, profile.name, 'kakao_signup_or_login');
}

function uuid() {
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, character => {
    const random = Math.floor(Math.random() * 16);
    return (character === 'x' ? random : (random & 3) | 8).toString(16);
  });
}

function mimeType(fileName) {
  const extension = fileName.toLowerCase().split('.').pop();
  if (extension === 'png') return 'image/png';
  if (extension === 'webp') return 'image/webp';
  return 'image/jpeg';
}

function randomImages(count) {
  const indices = requests.map((_, index) => index);
  for (let index = indices.length - 1; index > 0; index -= 1) {
    const target = Math.floor(Math.random() * (index + 1));
    [indices[index], indices[target]] = [indices[target], indices[index]];
  }
  return indices.slice(0, count).map(index => requests[index]);
}

function imagePath(profile, page = 0) {
  let path = `/api/v1/images?page=${page}&size=20&sort=${profile.imageSort}`;
  if (profile.filter === 'favorite') path += '&favorite=true';
  if (profile.filter === 'keyword') path += '&keyword=test';
  return path;
}

function executeActualAnalysis(profile, token) {
  const started = Date.now();
  let ok = true;
  let polls = 0;
  const selected = randomImages(profile.imageCount);
  const requestById = {};
  const images = selected.map(source => {
    const clientRequestId = uuid();
    requestById[clientRequestId] = source;
    return { ...source, clientRequestId };
  });
  analysisImages.add(profile.imageCount, { flow: profile.name });

  const registration = business('POST', '/api/v1/images', token, { images }, profile.name, 'actual_image_registration');
  if (!registration.ok) return { ok: false, polls: 0 };
  const registered = registration.response.json('data.registered') || [];
  const duplicated = registration.response.json('data.duplicated') || [];
  const failed = registration.response.json('data.failed') || [];
  if (registered.length + duplicated.length !== selected.length || failed.length > 0) {
    analysisFailed.add(Math.max(failed.length, selected.length - registered.length - duplicated.length), { flow: profile.name });
    return { ok: false, polls: 0 };
  }

  const pending = [];
  for (const item of registered) {
    const source = requestById[item.clientRequestId];
    const asset = http.get(`${assetUrl}/${encodeURIComponent(source.fileName)}`, {
      tags: { kind: 'test_asset', flow: profile.name }, timeout: '30s', responseType: 'binary',
    });
    const assetOk = check(asset, {
      'real test image fetched': response => response.status === 200 && response.body.byteLength === source.fileSize,
    }, { kind: 'pipeline', flow: profile.name, step: 'asset_fetch' });
    if (!assetOk) {
      analysisFailed.add(1, { flow: profile.name });
      ok = false;
      continue;
    }
    const upload = http.put(item.presignedURL, asset.body, {
      headers: { 'Content-Type': mimeType(source.fileName) },
      tags: { kind: 'pipeline', flow: profile.name, step: 'minio_upload' }, timeout: '60s',
    });
    const uploadOk = check(upload, {
      'real image uploaded to MinIO': response => response.status >= 200 && response.status < 300,
    }, { kind: 'pipeline', flow: profile.name, step: 'minio_upload' });
    if (uploadOk) {
      minioUploads.add(1, { flow: profile.name });
      pending.push({ imageId: item.imageId });
    } else {
      analysisFailed.add(1, { flow: profile.name });
      ok = false;
    }
  }
  for (const item of duplicated) pending.push({ imageId: item.imageId });

  const deadline = Date.now() + analysisTimeoutMs;
  while (pending.length > 0 && Date.now() < deadline) {
    sleep(pollSeconds);
    const progressPath = polls % 2 === 0
      ? '/api/v1/images?page=0&size=20&sort=UPLOADED_DESC'
      : `/api/v1/categories?sort=${profile.categorySort}`;
    const progress = business('GET', progressPath, token, null, profile.name, 'analysis_progress_poll');
    ok = progress.ok && ok;
    polls += 1;
    for (let index = pending.length - 1; index >= 0; index -= 1) {
      const item = pending[index];
      const detail = http.get(`${BASE_URL}/api/v1/images/${item.imageId}`, {
        headers: headers(token), tags: { kind: 'pipeline', flow: profile.name, step: 'analysis_result_poll' },
        timeout: '10s', responseCallback: http.expectedStatuses(200, 409),
      });
      if (detail.status === 200) {
        analysisCompleted.add(1, { flow: profile.name });
        pending.splice(index, 1);
      } else if (detail.status !== 409) {
        analysisFailed.add(1, { flow: profile.name });
        pending.splice(index, 1);
        ok = false;
      }
    }
  }
  if (pending.length > 0) {
    analysisTimeout.add(pending.length, { flow: profile.name });
    ok = false;
  }
  analysisWait.add((Date.now() - started), {
    flow: profile.name, image_count: String(profile.imageCount),
  });
  return { ok, polls };
}

function afterAnalysis(profile, token) {
  let ok = true;
  let steps = 0;
  const call = (method, path, body, step) => {
    const result = business(method, path, token, body, profile.name, step);
    ok = result.ok && ok;
    steps += 1;
    return result.response;
  };
  call('GET', imagePath(profile), null, 'analysis_completed_images');
  call('GET', `/api/v1/categories?sort=${profile.categorySort}`, null, 'analysis_completed_categories');
  if (profile.imageCount >= 3) call('GET', imagePath(profile, 1), null, 'batch_second_page');
  if (profile.postMode === 'schedule' || profile.postMode === 'organize') {
    call('GET', '/api/v1/schedules?page=0&size=20&sort=START_ASC', null, 'schedule_list');
    call('GET', '/api/v1/schedules?calendared=false&page=0&size=10', null, 'schedule_candidates');
  }
  if (profile.postMode === 'document' || profile.postMode === 'organize') {
    call('GET', '/api/v1/documents?page=0&size=20&sort=UPDATED_DESC', null, 'document_list');
    call('GET', '/api/v1/documents?page=0&size=10&sort=NAME_ASC', null, 'document_name_sort');
  }
  if (profile.postMode === 'browse') {
    call('GET', '/api/v1/images?page=0&size=50&sort=TITLE_ASC', null, 'large_gallery');
    call('GET', '/api/v1/user', null, 'profile_revisit');
  }
  return { ok, steps };
}

export function setup() {
  // Liveness uses one stable mock Kakao account. The first call creates it.
  const login = kakaoLogin(profiles[0], 0);
  if (!login.ok) throw new Error('mock Kakao fixture login failed');
  return { healthToken: login.response.json('data.accessToken') };
}

export function userJourney() {
  const profileIndex = (__VU - 1 + __ITER) % profiles.length;
  const profile = profiles[profileIndex];
  // Stable per VU: first capacity run creates the Kakao member; subsequent
  // sessions and capacity levels execute the existing-member login branch.
  const userNumber = __VU;
  const started = Date.now();
  let steps = 0;
  let ok = true;

  const login = kakaoLogin(profile, userNumber);
  ok = login.ok && ok;
  steps += 1;
  if (!login.ok) {
    flowSuccess.add(false, { flow: profile.name });
    return;
  }
  let token = login.response.json('data.accessToken');
  let refreshToken = login.response.json('data.refreshToken');
  const deviceId = `k6-kakao-device-${userNumber}`;

  for (const [path, step] of [
    ['/api/v1/user', 'profile'],
    [`/api/v1/categories?sort=${profile.categorySort}`, 'categories'],
    [imagePath(profile), 'gallery_before_analysis'],
  ]) {
    ok = business('GET', path, token, null, profile.name, step).ok && ok;
    steps += 1;
  }

  if (profile.pushToken) {
    ok = business('PUT', '/api/v1/user/devices/push-token', token, {
      deviceId, fcmToken: `mock-fcm-${userNumber}`,
    }, profile.name, 'push_token_register').ok && ok;
    steps += 1;
  }

  const analysis = executeActualAnalysis(profile, token);
  ok = analysis.ok && ok;
  steps += analysis.polls + 1 + profile.imageCount;
  const after = afterAnalysis(profile, token);
  ok = after.ok && ok;
  steps += after.steps;

  if (profile.refresh) {
    const refreshed = business('POST', '/api/v1/auth/refresh', null, {
      refreshToken, deviceId,
    }, profile.name, 'token_refresh');
    ok = refreshed.ok && ok;
    steps += 1;
    if (refreshed.ok) {
      token = refreshed.response.json('data.accessToken');
      refreshToken = refreshed.response.json('data.refreshToken');
    }
  }

  flowExecutions.add(1, { flow: profile.name });
  flowDuration.add(Date.now() - started, { flow: profile.name });
  sessionSteps.add(steps, { flow: profile.name });
  flowSuccess.add(ok, { flow: profile.name });
}

export function liveness(data) {
  const response = http.get(`${BASE_URL}/api/v1/categories`, {
    headers: headers(data.healthToken), tags: { kind: 'health' }, timeout: '5s',
  });
  check(response, {
    'server remains alive': (r) => r.status === 200 && r.json('result') === 'SUCCESS',
  }, { kind: 'health' });
  sleep(1);
}
