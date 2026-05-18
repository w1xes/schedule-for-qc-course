# Security Report

## Цільовий додаток
- **Назва:** OWASP Juice Shop
- **URL:** http://localhost:3000 (Docker: `bkimminich/juice-shop:latest`)
- **Метод:** Ручні перевірки (Manual Security Testing)
- **Дата:** 18 травня 2026

---

## Знайдені проблеми

| # | Рівень | Опис | Де знайдено |
|---|--------|------|-------------|
| 1 | 🔴 Critical | SQL Injection на формі логіну — вхід без пароля | `POST /rest/user/login` |
| 2 | 🔴 High | SQL Injection у пошуку — синтаксична помилка БД | `GET /rest/products/search?q=` |
| 3 | 🔴 High | Sensitive Data Exposure — FTP-директорія відкрита публічно | `GET /ftp` |
| 4 | 🟠 Medium | CORS: `Access-Control-Allow-Origin: *` — будь-який сайт може робити запити | Всі `/api/*` ендпоінти |
| 5 | 🟠 Medium | Відсутній заголовок `Content-Security-Policy` (CSP) | Всі сторінки |
| 6 | 🟠 Medium | Немає rate limiting на логін — brute force можливий | `POST /rest/user/login` |
| 7 | 🟡 Low | Відсутній заголовок `Strict-Transport-Security` (HSTS) | Всі сторінки |
| 8 | 🟡 Low | Витік деталей помилок у HTTP-відповідях (назва БД, тип помилки) | `GET /rest/products/search` |

---

## Деталі

### Проблема 1: SQL Injection на формі логіну (Critical)
- **Що зроблено:** відправлено POST-запит на `/rest/user/login` з email `' OR 1=1--` та довільним паролем
- **Команда:**
  ```bash
  curl -X POST http://localhost:3000/rest/user/login \
    -H "Content-Type: application/json" \
    -d '{"email":"'\'' OR 1=1--","password":"test"}'
  ```
- **Результат:** HTTP 200, отримано JWT-токен адміністратора (`admin@juice-sh.op`) без знання пароля
- **Ризик:** Critical — повний доступ до облікового запису адміна, можливість захоплення будь-якого акаунту
- **Рекомендація:** використовувати параметризовані запити (prepared statements) замість конкатенації SQL

---

### Проблема 2: SQL Injection у пошуку (High)
- **Що зроблено:** передано XSS-payload у параметр `q` пошукового ендпоінту
- **Команда:**
  ```bash
  curl "http://localhost:3000/rest/products/search?q=<img%20src=x%20onerror=alert('XSS')>"
  ```
- **Результат:** HTTP 500 з повідомленням `SQLITE_ERROR: near "XSS": syntax error` — payload інтерпретується як SQL
- **Ризик:** High — можлива витяжка даних з бази, маніпуляція запитами
- **Рекомендація:** параметризовані запити, ORM з безпечним escaping

---

### Проблема 3: Sensitive Data Exposure — FTP директорія (High)
- **Що зроблено:** відкрито URL `/ftp` без авторизації
- **Команда:**
  ```bash
  curl http://localhost:3000/ftp
  ```
- **Результат:** HTTP 200, публічно доступний файловий менеджер з файлами:
  - `acquisitions.md` — конфіденційна бізнес-інформація про поглинання
  - `coupons_2013.md.bak` — резервна копія з кодами купонів
  - `incident-support.kdbx` — база даних паролів KeePass
  - `package-lock.json.bak` — резервна копія dependency-файлу (розкриває версії залежностей)
  - `encrypt.pyc` — скомпільований Python-файл шифрування
- **Ризик:** High — витік конфіденційних даних, можливий доступ до паролів
- **Рекомендація:** обмежити доступ до `/ftp` авторизацією, прибрати резервні копії з web-root

---

### Проблема 4: Небезпечна CORS-конфігурація (Medium)
- **Що зроблено:** відправлено запит з заголовком `Origin: http://evil.com`
- **Команда:**
  ```bash
  curl -I -H "Origin: http://evil.com" http://localhost:3000/api/Products
  ```
- **Результат:** у відповіді `Access-Control-Allow-Origin: *` — сервер приймає запити з будь-якого домену
- **Ризик:** Medium — вразливість до CSRF-атак, cross-origin запити з будь-якого сайту
- **Рекомендація:** обмежити CORS лише довіреними доменами (`Access-Control-Allow-Origin: https://yourdomain.com`)

---

### Проблема 5: Відсутній Content-Security-Policy (Medium)
- **Що зроблено:** перевірено HTTP-заголовки відповіді головної сторінки
- **Команда:**
  ```bash
  curl -I http://localhost:3000/
  ```
- **Результат:** заголовок `Content-Security-Policy` відсутній у відповіді
- **Присутні заголовки безпеки:** `X-Content-Type-Options: nosniff`, `X-Frame-Options: SAMEORIGIN`
- **Ризик:** Medium — без CSP браузер не обмежує джерела скриптів, що полегшує XSS-атаки
- **Рекомендація:** додати CSP заголовок: `Content-Security-Policy: default-src 'self'`

---

### Проблема 6: Відсутній rate limiting на логін (Medium)
- **Що зроблено:** 10 послідовних спроб логіну з невірним паролем
- **Команда:**
  ```bash
  for i in {1..10}; do
    curl -s -o /dev/null -w "%{http_code}\n" \
      -X POST http://localhost:3000/rest/user/login \
      -H "Content-Type: application/json" \
      -d '{"email":"user@test.com","password":"wrong'$i'"}'
  done
  ```
- **Результат:** всі 10 спроб повернули HTTP 401 без жодного блокування, без CAPTCHA, без затримок
- **Ризик:** Medium — можлива автоматизована brute force атака на акаунти
- **Рекомендація:** додати rate limiting (наприклад, блокування після 5 невдалих спроб), CAPTCHA або exponential backoff

---

### Проблема 7: Відсутній HSTS (Low)
- **Що зроблено:** перевірено HTTP-заголовки
- **Результат:** заголовок `Strict-Transport-Security` відсутній
- **Ризик:** Low — без HSTS браузери не примусово використовують HTTPS, можливі downgrade-атаки
- **Рекомендація:** додати `Strict-Transport-Security: max-age=31536000; includeSubDomains`

---

### Проблема 8: Витік деталей помилок (Low)
- **Що зроблено:** введено некоректний SQL-payload у пошук
- **Результат:** HTTP 500 відповідь містить: `SQLITE_ERROR: near "XSS": syntax error` та назву фреймворку (`OWASP Juice Shop (Express ^4.22.1)`)
- **Ризик:** Low — розкриває тип бази даних (SQLite), версію Express, що допомагає зловмисникам у плануванні атак
- **Рекомендація:** замість детальних повідомлень повертати загальне `500 Internal Server Error`, логувати деталі лише на сервері

---

## Підсумок перевірок

| Перевірка | Результат |
|-----------|-----------|
| SQL Injection (логін) | ❌ Вразливий |
| SQL Injection (пошук) | ❌ Вразливий |
| XSS (пошук, відображення) | ⚠️ Payload не відображається в HTML, але викликає SQLi |
| Broken Access Control (IDOR) | ✅ Захищено (потребує авторизації) |
| Sensitive Data Exposure (FTP) | ❌ Вразливий |
| CORS | ❌ Небезпечна конфігурація |
| Security Headers (CSP) | ❌ Відсутній |
| Security Headers (HSTS) | ❌ Відсутній |
| Security Headers (X-Frame-Options) | ✅ Присутній (`SAMEORIGIN`) |
| Security Headers (X-Content-Type-Options) | ✅ Присутній (`nosniff`) |
| Rate Limiting на логін | ❌ Відсутній |
| Error Information Leakage | ❌ Вразливий |

---

## Висновок

**Знайдено 8 проблем:** 2 Critical/High, 3 Medium, 3 Low.

**Найкритичніші:**
1. **SQL Injection на логіні** — дозволяє отримати доступ адміна без пароля. Найнебезпечніша вразливість.
2. **SQL Injection у пошуку** — потенційна витяжка всієї бази даних.
3. **Відкрита FTP-директорія** — публічний доступ до конфіденційних файлів, включаючи базу паролів KeePass.

**Що виправити в першу чергу:**
1. Впровадити параметризовані SQL-запити у всіх ендпоінтах
2. Закрити `/ftp` від публічного доступу, перенести чутливі файли
3. Налаштувати CORS лише на довірені домени
4. Додати rate limiting та lockout-механізм на форму логіну
5. Додати заголовки CSP та HSTS
