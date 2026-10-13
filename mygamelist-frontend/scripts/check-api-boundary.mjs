import assert from 'node:assert/strict';
import { isApiRequest } from '../src/app/interceptors/api-url.ts';
const origin = 'https://mygamelist.example';
const api = 'https://api.example/api';
const cases = [
  ['https://api.example/api/users', true],
  ['https://api.example/api', true],
  ['https://api.example/api?x=1', true],
  ['https://api.example/api2/users', false],
  ['https://api.example.attacker.test/api/users', false],
  ['https://api.example@attacker.test/api/users', false],
  ['http://api.example/api/users', false],
  ['https://api.example:8443/api/users', false],
  ['/api/users', false],
  ['https://api.example/api/../outside', false]
];
for (const [url, expected] of cases) assert.equal(isApiRequest(url, api, origin), expected, url);
assert.equal(isApiRequest('/api/users', '/api', origin), true);
assert.equal(isApiRequest('https://api.example/users', 'https://api.example/', origin), true);
console.log('12 verificações de destino do token passaram.');
