// 시맨틱 검색 단독 부하 (E3 실험의 검색 축).
//
// 파이프라인 업로드 부하(pipeline-load.js)와 동시에 돌려서, 분석 이벤트가 쌓일 때
// 검색 응답이 함께 밀리는지(head-of-line blocking)를 계측한다. 컨슈머 분리(B)
// 전에는 검색 p95가 분석 큐 길이에 비례해 수 초~10초(504)까지 밀리고, 분리 후에는
// 업로드 부하와 무관하게 유지되는 것이 기대 결과다.
//
// open model(constant-arrival-rate)이라 응답이 늦어도 유입은 계속된다.
//
// 사용 예 (서버):
//   k6 run -e RATE=5 -e DURATION=90s -e USER_NUMBER=3000009 semantic-search-load.js
// 로컬 스모크:
//   k6 run -e AUTH_MODE=local -e LOGIN_ID=k6seed-3000001 -e RATE=3 -e DURATION=20s \
//          -e BASE_URL=http://localhost:8080 semantic-search-load.js

import http from 'k6/http';
import { check, fail } from 'k6';
import { Trend, Counter } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://modera-api:8080';
const AUTH_MODE = __ENV.AUTH_MODE || 'kakao';
const RATE = Number(__ENV.RATE || 5);
const DURATION = __ENV.DURATION || '90s';
// 시딩된 헤비 사용자(9, 19, ... 구간)를 기본 대상으로 잡는다 — 검색은 사용자
// 스코프라 대상 계정의 인덱스 크기가 검색 비용을 결정한다.
const USER_NUMBER = Number(__ENV.USER_NUMBER || 3_000_009);
const TOKEN_PREFIX = __ENV.KAKAO_TOKEN_PREFIX || 'modera-perf-kakao';
const LOGIN_ID = __ENV.LOGIN_ID || 'k6tester';
const PASSWORD = __ENV.PASSWORD || 'password123';

const searchKeywords = ['영수증', '일정', '쇼핑', '문서', '여행'];
const searchDuration = new Trend('semantic_search_duration', true);
const searchTimeouts = new Counter('semantic_search_timeouts');

export const options = {
  scenarios: {
    search: {
      executor: 'constant-arrival-rate',
      rate: RATE,
      timeUnit: '1s',
      duration: DURATION,
      preAllocatedVUs: Math.max(10, RATE * 4),
      maxVUs: Math.max(50, RATE * 12),
    },
  },
  thresholds: {
    // 관찰용 기준: B 적용 후 목표. before 실행에서는 깨지는 것이 정상(그게 증거다).
    semantic_search_duration: ['p(95)<1500'],
    http_req_failed: ['rate<0.01'],
  },
};

export function setup() {
  const headers = { 'Content-Type': 'application/json' };
  if (AUTH_MODE === 'local') {
    const res = http.post(`${BASE_URL}/api/v1/auth/login`, JSON.stringify({
      loginId: LOGIN_ID, password: PASSWORD, deviceId: 'k6-search-load',
    }), { headers });
    if (res.status !== 200) fail(`local login failed: ${res.status} ${res.body}`);
    return { token: res.json('data.accessToken') };
  }
  const res = http.post(`${BASE_URL}/api/v1/auth/kakao/login`, JSON.stringify({
    kakaoAccessToken: `${TOKEN_PREFIX}-${USER_NUMBER}`,
    deviceId: `k6-kakao-device-${USER_NUMBER}`,
  }), { headers, timeout: '10s' });
  if (res.status !== 200) fail(`kakao login failed: ${res.status} ${res.body}`);
  return { token: res.json('data.accessToken') };
}

export default function (data) {
  const res = http.post(`${BASE_URL}/api/v1/images/search/semantic`, JSON.stringify({
    query: searchKeywords[Math.floor(Math.random() * searchKeywords.length)],
    page: 0,
    size: 20,
  }), {
    headers: { Authorization: `Bearer ${data.token}`, 'Content-Type': 'application/json' },
    tags: { name: 'POST /api/v1/images/search/semantic' },
    timeout: '15s',
  });
  searchDuration.add(res.timings.duration);
  if (res.status === 504) searchTimeouts.add(1);
  check(res, {
    'semantic search 200': r => r.status === 200,
    'semantic search SUCCESS': r => r.status === 200 && r.json('result') === 'SUCCESS',
  });
}
