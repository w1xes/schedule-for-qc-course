import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  stages: [
    { duration: '10s', target: 5 },   // розігрів: 0 → 5 користувачів
    { duration: '30s', target: 10 },  // тримаємо 10 користувачів
    { duration: '10s', target: 0 },   // завершення
  ],
  thresholds: {
    http_req_duration: ['p(95)<500'], // 95% запитів < 500ms
    http_req_failed: ['rate<0.05'],   // менше 5% помилок
  },
};

export default function () {
  // Тест 1: Головна сторінка
  const main = http.get('https://test.k6.io/');
  check(main, {
    'main: status 200': (r) => r.status === 200,
    'main: response < 500ms': (r) => r.timings.duration < 500,
  });

  // Тест 2: Сторінка контактів
  const contacts = http.get('https://test.k6.io/contacts.php');
  check(contacts, {
    'contacts: status 200': (r) => r.status === 200,
    'contacts: response < 500ms': (r) => r.timings.duration < 500,
  });

  // Тест 3: Сторінка новин
  const news = http.get('https://test.k6.io/news.php');
  check(news, {
    'news: status 200': (r) => r.status === 200,
    'news: response < 500ms': (r) => r.timings.duration < 500,
  });

  sleep(1);
}
