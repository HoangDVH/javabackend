import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE = __ENV.BASE_URL || 'https://javabackend-olfp.onrender.com';

export const options = {
  vus: Number(__ENV.VUS || 5),
  duration: __ENV.DURATION || '20s',
  thresholds: {
    http_req_failed: ['rate<0.2'],
    http_req_duration: ['p(95)<15000'],
  },
};

export default function () {
  const res = http.get(`${BASE}/api/v1/products?page=0&size=10`);
  check(res, {
    'status is 200': (r) => r.status === 200,
  });
  sleep(1);
}
