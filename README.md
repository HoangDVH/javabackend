# Java backend (Spring Boot + JWT) — Easy Mart API

API REST cho Easy Mart: JWT auth, Google Sign-In, danh mục/sản phẩm (Redis cache), đơn hàng, thanh toán COD/VNPay, profile & sổ địa chỉ, WebSocket realtime orders.

| | URL |
|--|--|
| **FE** | https://easy-mart-vert.vercel.app/ |
| **BE (Render)** | https://javabackend-olfp.onrender.com |
| **Swagger** | https://javabackend-olfp.onrender.com/swagger-ui/index.html |
| **GitHub** | https://github.com/HoangDVH/javabackend |

## Cấu trúc

- `pom.xml` (thư mục gốc) — aggregator Maven; IDE nên mở **repo gốc** rồi Reload Maven.
- `jwtjava/` — module Spring Boot 3 (build/run/test chủ yếu ở đây).
- `tools/k6/` — binary k6 + script smoke load test (không bắt buộc commit binary).
- `render.yaml` — Blueprint Render (web + Redis Key Value Free).

### IntelliJ: `ClassNotFoundException: JwtjavaApplication`

**Use classpath of module** = module Maven **`jwtjava`** (không chọn aggregator gốc). Có sẵn `.idea/runConfigurations/JwtjavaApplication.xml`.

### IntelliJ vẫn báo Cannot resolve symbol

1. Plugin **Lombok** + Enable annotation processing.
2. **Project SDK = JDK 17**.
3. Maven → **Reload All Projects**.
4. **Invalidate Caches → Restart** nếu cần.

## Yêu cầu

- JDK 17+
- MySQL 8 (local mặc định: DB `jwtjava`, port `3307`) **hoặc** Postgres (Render/`DATABASE_URL`)
- Redis (tùy chọn local; **bật trên Render** cho blacklist JWT, rate limit, catalog cache)

## Chạy nhanh

```bash
cd jwtjava
./mvnw spring-boot:run
```

Ứng dụng: `http://localhost:8080`.

- **Swagger UI**: http://localhost:8080/swagger-ui.html  
- **OpenAPI JSON**: http://localhost:8080/v3/api-docs  

### Try on Swagger (flow cơ bản)

1. **Login** — `POST /api/v1/auth/login` → copy `result.accessToken` (refresh qua HttpOnly cookie).
2. **Authorize** — scheme **bearer-jwt**, paste access token.
3. **Products** — `GET /api/v1/products` (`page=0`, `size=20`, `sort=createdAt,desc`).
4. **Orders** — `POST /api/v1/orders` với `items` + `receiverName` + `receiverPhone` + `shippingAddress`.
5. **COD** — `POST /api/v1/payments` `{ "orderId", "method": "CASH" }` (idempotent nếu đã SUCCESS).
6. **VNPay** — `POST /api/v1/payments/vnpay` → mở `paymentUrl`; IPN public cập nhật `PAID`.
7. **Refresh** — `POST /api/v1/auth/refresh` (không body, cần cookie).

## API tóm tắt

| Nhóm | Endpoints |
|------|-----------|
| Auth | `register`, `login`, `google` (`idToken`), `refresh`, `introspect`, `logout`, `forgot-password`, `reset-password` |
| Catalog (public GET) | `/api/v1/products/**`, `/api/v1/categories/**` — Redis cache TTL ~2–30 phút, chống stampede |
| Orders | CRUD buyer + cancel; seller fulfillment; shipping fee 30k nếu subtotal &lt; 500k |
| Payments | COD `/api/v1/payments`; VNPay `/api/v1/payments/vnpay` + `/ipn` |
| Users | `GET/PUT /api/v1/users/me`; addresses `/api/v1/users/me/addresses` |
| Realtime | WebSocket orders — xem `jwtjava/REALTIME_ORDERS.md` |

Upload ảnh: `POST /api/v1/products/images` (Cloudinary khi bật). Ảnh public: `GET /files/product-images/...`.

## Anti-lag (Phase 0–2) — đã làm trong code

Phase 3 (nâng Render/Postgres/Redis paid) **chưa làm** — chỉ khi Free không đủ.

| Phase | Nội dung |
|-------|----------|
| **0** | `SlowRequestLoggingFilter` (mặc định ≥ 1000ms); log catalog cache HIT/MISS |
| **1** | Hikari pool nhỏ (~8), `JPA_SHOW_SQL=false`, Tomcat threads; catalog **stampede lock**; soft rate limit order/payment |
| **2** | Trừ/hoàn kho **atomic** (`decrementStockIfAvailable` / `incrementStock`); COD payment **idempotent** |

### Soft rate limit

- Chỉ **POST**; key Redis theo IP.
- Auth (login/register/…) + **`POST /api/v1/orders`** (20/phút) + **`POST /api/v1/payments`** & `/payments/vnpay` (20/phút).
- **Không** rate-limit GET catalog.
- Redis tắt/lỗi → **fail-open** (cho qua).
- Vượt ngưỡng → **429** + header `Retry-After`. FE nên backoff + jitter.

### FE retry (gợi ý)

1. Đọc `Retry-After` khi 429.  
2. Exponential backoff + jitter (1s → 2s → 4s…).  
3. Cold start Render: retry nhẹ 5xx/network; không spam POST.  
Chi tiết thêm trong mô tả OpenAPI (Swagger).

## Load test (k6)

Script: `tools/k6/catalog-smoke.js`.

```powershell
# Warmup cold start trước
Invoke-WebRequest "https://javabackend-olfp.onrender.com/api/v1/products?page=0&size=1" -TimeoutSec 120

$env:VUS="3"; $env:DURATION="30s"
.\tools\k6\k6-v1.3.0-windows-amd64\k6.exe run .\tools\k6\catalog-smoke.js
```

Smoke (3 VU, GET products, sau warmup): ~100% 200, latency avg ~4s trên Free — phù hợp vài–chục user browse, **không** phải hàng trăm concurrent bền vững.

## Cấu hình & bảo mật

### Biến môi trường chính

| Biến | Mô tả |
|------|--------|
| `JWTJAVA_DATASOURCE_*` / `DATABASE_URL` | MySQL local hoặc Postgres (Render) |
| `JWT_SIGNER_KEY` | Base64 256-bit HS256 |
| `REDIS_ENABLED`, `REDIS_HOST`, `REDIS_PORT`, `REDIS_PASSWORD` | Cache / rate limit / JWT blacklist |
| `HIKARI_MAX_POOL_SIZE` | Mặc định 8 (Free Postgres) |
| `JPA_SHOW_SQL` | Mặc định `false` |
| `SLOW_REQUEST_MS` | Ngưỡng log slow request (ms) |
| `RATE_LIMIT_ENABLED` | Soft rate limit |
| `CATALOG_CACHE_ENABLED` | Redis catalog cache |
| `GOOGLE_AUTH_ENABLED`, `GOOGLE_CLIENT_ID` | Google Sign-In |
| `VNPAY_*` | VNPay sandbox/prod |
| `CLOUDINARY_*` | Upload ảnh |
| `MAIL_ENABLED`, `RESEND_API_KEY`, … | Quên mật khẩu |
| `APP_SEED_ADMIN_EMAIL` / `PASSWORD` | Admin seed |

Local: copy `jwtjava/src/main/resources/application-local.example.yaml` → `application-local.yaml` (đã gitignore).

## Frontend (CORS)

`app.cors.allowed-origins` gồm Vercel + localhost. Auth từ browser cần `credentials: "include"` cho refresh cookie.

```js
fetch("https://javabackend-olfp.onrender.com/api/v1/auth/refresh", {
  method: "POST",
  credentials: "include"
});
```

Production cookie: `JWT_REFRESH_COOKIE_SAME_SITE=None`, `JWT_REFRESH_COOKIE_SECURE=true`.

## Deploy Render

1. Push GitHub → Render **Blueprint** (`render.yaml`).
2. Postgres ngoài (Neon/Supabase/…) → set `DATABASE_URL`.
3. Set secrets: `JWT_SIGNER_KEY`, Cloudinary, Redis (Blueprint gắn Key Value), VNPay, Google, Resend, …
4. App convert `DATABASE_URL` → JDBC Postgres khi start.

### Free tier sleep (cold start)

- Free web **spin-down sau ~15 phút không traffic**; lần request sau ~30–60s wake.
- **Không tắt sleep bằng code trong app.**
- Giữ thức tạm: ping ngoài mỗi 10–14 phút (UptimeRobot / cron-job.org) — vẫn ăn **750 Free instance hours/tháng**.
- Không sleep thật sự: nâng **Starter** (hoặc VPS / Oracle Always Free tự host).

PaaS free giống Render mà always-on ổn định gần như không có; alternative free always-on thường là **VM tự quản** (Oracle Always Free + Docker/Coolify).

## License

Dự án mẫu / nội bộ — bổ sung `LICENSE` nếu public hóa repo.
