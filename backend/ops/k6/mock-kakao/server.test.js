'use strict';

const assert = require('node:assert/strict');
const { after, before, test } = require('node:test');
const { createMockKakaoServer } = require('./server');

let server;
let baseUrl;

before(async () => {
  server = createMockKakaoServer({ appId: 1525155, tokenPrefix: 'test-token' });
  await new Promise(resolve => server.listen(0, '127.0.0.1', resolve));
  baseUrl = `http://127.0.0.1:${server.address().port}`;
});

after(async () => {
  await new Promise((resolve, reject) => server.close(error => error ? reject(error) : resolve()));
});

test('returns configured Kakao app information for a valid performance token', async () => {
  const response = await fetch(`${baseUrl}/v1/user/access_token_info`, {
    headers: { Authorization: 'Bearer test-token-42' },
  });

  assert.equal(response.status, 200);
  const body = await response.json();
  assert.equal(body.app_id, 1525155);
  assert.equal(body.id, 7_000_000_000_042);
});

test('returns a stable and unique verified user', async () => {
  const response = await fetch(`${baseUrl}/v2/user/me`, {
    headers: { Authorization: 'Bearer test-token-42' },
  });

  assert.equal(response.status, 200);
  const body = await response.json();
  assert.equal(body.id, 7_000_000_000_042);
  assert.equal(body.kakao_account.email, 'perf-kakao-42@perf.modera.test');
  assert.equal(body.kakao_account.is_email_valid, true);
  assert.equal(body.kakao_account.is_email_verified, true);
});

test('rejects malformed or missing tokens', async () => {
  for (const authorization of [undefined, 'Bearer wrong-42', 'Bearer test-token-not-a-number']) {
    const headers = authorization ? { Authorization: authorization } : {};
    const response = await fetch(`${baseUrl}/v1/user/access_token_info`, { headers });
    assert.equal(response.status, 401);
  }
});
