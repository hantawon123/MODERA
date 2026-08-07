import http from 'k6/http';
import crypto from 'k6/crypto';
import { check, sleep } from 'k6';
import { BASE_URL } from './common.js';

const imagePath = __ENV.IMAGE_FILE || '/images/KakaoTalk_20260731_100637911_01.jpg';
const sourceImage = open(imagePath, 'b');

function uuid() {
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, c => {
    const r = Math.floor(Math.random() * 16); return (c === 'x' ? r : (r & 3) | 8).toString(16);
  });
}

function headers(token) { return { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` }; }

export function executePipeline(data, metrics) {
  const started = Date.now();
  const requestId = uuid();
  const original = new Uint8Array(sourceImage);
  const marker = new Uint8Array(requestId.length);
  for (let index = 0; index < requestId.length; index += 1) marker[index] = requestId.charCodeAt(index) & 0xff;
  const bytes = new Uint8Array(original.length + marker.length);
  bytes.set(original); bytes.set(marker, original.length);
  const body = { images: [{ clientRequestId: requestId, fileName: `perf-${requestId}.jpg`,
    contentHash: crypto.sha256(bytes.buffer, 'hex'), fileSize: bytes.byteLength,
    ocr: { rawText: 'performance test' } }] };
  const registration = http.post(`${BASE_URL}/api/v1/images`, JSON.stringify(body), {
    headers: headers(data.accessToken), tags: { kind: 'pipeline_registration' }, timeout: '5s',
  });
  const item = registration.json('data.registered.0');
  if (!check(registration, { 'registration succeeded': r => r.status === 200 && Boolean(item) })) {
    metrics.success.add(false); return;
  }
  const upload = http.put(item.presignedURL, bytes.buffer, {
    headers: { 'Content-Type': 'image/jpeg' }, tags: { kind: 'pipeline_upload' }, timeout: '10s',
  });
  if (!check(upload, { 'upload succeeded': r => r.status >= 200 && r.status < 300 })) {
    metrics.success.add(false); return;
  }
  const deadline = Date.now() + Number(__ENV.PIPELINE_TIMEOUT_MS || 10000);
  while (Date.now() < deadline) {
    const result = http.get(`${BASE_URL}/api/v1/images/${item.imageId}`, {
      headers: headers(data.accessToken), tags: { kind: 'pipeline_poll' }, timeout: '5s',
      responseCallback: http.expectedStatuses(200, 409),
    });
    if (result.status === 200) {
      metrics.duration.add(Date.now() - started); metrics.success.add(true); metrics.completed.add(1); return;
    }
    sleep(Number(__ENV.POLL_SECONDS || 0.05));
  }
  metrics.timeouts.add(1); metrics.duration.add(Date.now() - started); metrics.success.add(false);
}
