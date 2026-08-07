import http from 'k6/http';
import { check, fail } from 'k6';

export const BASE_URL = __ENV.BASE_URL || 'http://localhost:8090';
export const LOGIN_ID = __ENV.LOGIN_ID || 'k6tester';
export const PASSWORD = __ENV.PASSWORD || 'password123';
export const EMAIL = __ENV.EMAIL || 'k6tester@example.com';

export function authenticate() {
  const headers = { 'Content-Type': 'application/json' };
  const register = http.post(`${BASE_URL}/api/v1/auth/register`,
    JSON.stringify({ loginId: LOGIN_ID, password: PASSWORD, email: EMAIL }),
    { headers, responseCallback: http.expectedStatuses(200, 409) });
  if (register.status !== 200 && register.status !== 409) fail(`register failed: ${register.body}`);
  const login = http.post(`${BASE_URL}/api/v1/auth/login`, JSON.stringify({
    loginId: LOGIN_ID, password: PASSWORD, deviceId: `k6-pipeline-${Date.now()}`,
  }), { headers });
  if (!check(login, { 'login succeeded': r => r.status === 200 })) fail(`login failed: ${login.body}`);
  return { accessToken: login.json('data.accessToken') };
}
