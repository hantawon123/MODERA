'use strict';

const http = require('node:http');

const DEFAULT_PORT = 8080;
const DEFAULT_APP_ID = 1525155;
const DEFAULT_TOKEN_PREFIX = 'modera-perf-kakao';
const KAKAO_ID_BASE = 7_000_000_000_000;

function escapeRegExp(value) {
  return value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}

function parseUserNumber(authorization, tokenPrefix) {
  if (typeof authorization !== 'string' || !authorization.startsWith('Bearer ')) {
    return null;
  }

  const token = authorization.slice('Bearer '.length);
  const match = token.match(new RegExp(`^${escapeRegExp(tokenPrefix)}-(\\d+)$`));
  if (!match) return null;

  const userNumber = Number(match[1]);
  if (!Number.isSafeInteger(userNumber) || userNumber < 0) return null;
  if (!Number.isSafeInteger(KAKAO_ID_BASE + userNumber)) return null;
  return userNumber;
}

function json(response, status, body) {
  const payload = JSON.stringify(body);
  response.writeHead(status, {
    'Content-Type': 'application/json; charset=utf-8',
    'Content-Length': Buffer.byteLength(payload),
    'Cache-Control': 'no-store',
  });
  response.end(payload);
}

function createMockKakaoServer(options = {}) {
  const appId = Number(options.appId ?? process.env.MOCK_KAKAO_APP_ID ?? DEFAULT_APP_ID);
  const tokenPrefix = options.tokenPrefix
    ?? process.env.MOCK_KAKAO_TOKEN_PREFIX
    ?? DEFAULT_TOKEN_PREFIX;
  const delayMs = Number(options.delayMs ?? process.env.MOCK_KAKAO_DELAY_MS ?? 0);

  if (!Number.isSafeInteger(appId) || appId <= 0) {
    throw new Error('MOCK_KAKAO_APP_ID must be a positive integer');
  }
  if (!tokenPrefix) {
    throw new Error('MOCK_KAKAO_TOKEN_PREFIX must not be empty');
  }
  if (!Number.isFinite(delayMs) || delayMs < 0) {
    throw new Error('MOCK_KAKAO_DELAY_MS must be zero or a positive number');
  }

  const server = http.createServer((request, response) => {
    const respond = () => {
      const url = new URL(request.url, 'http://mock-kakao');
      if (request.method === 'GET' && url.pathname === '/health') {
        json(response, 200, { status: 'UP' });
        return;
      }

      if (request.method !== 'GET') {
        json(response, 405, { code: -405, msg: 'method not allowed' });
        return;
      }

      const userNumber = parseUserNumber(request.headers.authorization, tokenPrefix);
      if (userNumber === null) {
        json(response, 401, { code: -401, msg: 'invalid access token' });
        return;
      }

      if (url.pathname === '/v1/user/access_token_info') {
        json(response, 200, {
          id: KAKAO_ID_BASE + userNumber,
          app_id: appId,
          expires_in: 3600,
        });
        return;
      }

      if (url.pathname === '/v2/user/me') {
        json(response, 200, {
          id: KAKAO_ID_BASE + userNumber,
          kakao_account: {
            email: `perf-kakao-${userNumber}@perf.modera.test`,
            is_email_valid: true,
            is_email_verified: true,
          },
        });
        return;
      }

      json(response, 404, { code: -404, msg: 'not found' });
    };

    if (delayMs === 0) respond();
    else setTimeout(respond, delayMs);
  });

  server.keepAliveTimeout = 5_000;
  server.headersTimeout = 6_000;
  return server;
}

if (require.main === module) {
  const port = Number(process.env.PORT ?? DEFAULT_PORT);
  const server = createMockKakaoServer();
  server.listen(port, '0.0.0.0', () => {
    process.stdout.write(`mock-kakao listening on ${port}\n`);
  });
}

module.exports = { createMockKakaoServer, parseUserNumber };
