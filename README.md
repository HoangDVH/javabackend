# Java backend (Spring Boot + JWT)

API REST cho auth JWT, danh mục, sản phẩm, upload/lưu ảnh local.

## Cấu trúc

- `pom.xml` (thư mục gốc repo) — **aggregator Maven**; IntelliJ/Cursor nên **mở cả repo gốc** hoặc *Reload Maven Project* để IDE nạp `jwtjava/` làm module → hết lỗi «Cannot resolve symbol» hàng loạt.
- `jwtjava/` — module Spring Boot 3 (`./mvnw` trong thư mục đó; chạy build/test chủ yếu ở đây).

### IntelliJ: `ClassNotFoundException: JwtjavaApplication`

Cấu hình chạy (**Run → Edit Configurations**) phải có **Use classpath of module** = module Maven **`jwtjava`** (thư mục con), **không** chọn module aggregator gốc (`javabackend-aggregator`). Repo có sẵn `.idea/runConfigurations/JwtjavaApplication.xml` — sau **Reload Maven**, chọn Run **JwtjavaApplication**.

### IntelliJ vẫn báo lỗi (Cannot resolve symbol)

1. Cài **plugin Lombok** và bật **Settings → Build, Execution, Deployment → Compiler → Annotation Processors → Enable annotation processing**.
2. **Project SDK = JDK 17** (`File → Project Structure → Project`).
3. Cửa sổ **Maven** → **Reload All Projects** (hoặc chuột phải `pom.xml` gốc → **Add as Maven Project**).
4. **File → Invalidate Caches → Invalidate and Restart**.

## Yêu cầu

- JDK 17+
- MySQL 8 (mặc định cấu hình: DB `jwtjava`, port `3307` — đổi qua biến môi trường hoặc `application-local.yaml`).

## Chạy nhanh

```bash
cd jwtjava
./mvnw spring-boot:run
```

Ứng dụng: `http://localhost:8080`.

### Swagger / OpenAPI

- **Swagger UI**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- **OpenAPI JSON**: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

Tag order: **Authentication** first, then Categories, Products, Users. Request bodies show **Example Value** from `@Schema(example=…)`.

#### Try on Swagger (typical flow)

1. **Login** — open **Authentication** → `POST /api/v1/auth/login` → **Try it out**. Defaults suggest `admin@gmail.com` / `Admin@123456` (seeded admin). **Execute** → copy `result.accessToken`.
2. **Authorize** — **Authorize** (lock icon) → scheme **bearer-jwt** → paste **only** the access token (Swagger adds the `Bearer` prefix).
3. **List products** — **Products** → `GET /api/v1/products` → **Try it out**:
   - Do **not** set `isFeatured=true` if you want *all* products (omit the param when possible).
   - Pagination: `page=0`, `size=20`, `sort=createdAt,desc` (do not use `string` as sort property).
4. **Create product (ADMIN)** — `POST /api/v1/products`: use the example body (`categoryId` e.g. `1`, image https URL). Requires admin token in **Authorize**.
5. **Upload images** — `POST /api/v1/products/images`: form field **`files`**, ADMIN only.

## Cấu hình & bảo mật

### Biến môi trường (khuyến nghị cho môi trường thật)

| Biến | Mô tả |
|--------|--------|
| `JWTJAVA_DATASOURCE_URL` | JDBC URL MySQL |
| `JWTJAVA_DATASOURCE_USERNAME` | User DB |
| `JWTJAVA_DATASOURCE_PASSWORD` | Mật khẩu DB |
| `JWT_SIGNER_KEY` | Base64 **256 bit** cho ký JWT HS256 |
| `APP_SEED_ADMIN_EMAIL` | Email tài khoản admin seed |
| `APP_SEED_ADMIN_PASSWORD` | Mật khẩu admin seed |

Trên Windows (PowerShell) ví dụ:

```powershell
$env:JWT_SIGNER_KEY="..."; $env:JWTJAVA_DATASOURCE_PASSWORD="..."; cd jwtjava; ./mvnw spring-boot:run
```

### File local (không commit)

Tạo `jwtjava/src/main/resources/application-local.yaml` (đã nằm trong `.gitignore`) để ghi đè URL DB, JWT, v.v. File này được import tùy chọn từ `application.yaml`.

Tham chiếu: `jwtjava/src/main/resources/application-local.example.yaml`.

## API tóm tắt

- Auth: `POST /api/v1/auth/register`, `/login`, `/refresh`, `/introspect`
- Sản phẩm (cần JWT; admin cho ghi): `/api/v1/products`
- Upload ảnh (admin): `POST /api/v1/products/images` (`multipart`, field `files`)
- Ảnh tĩnh: `GET /files/product-images/...` (public)

## Frontend (CORS)

Chỉnh `app.cors.allowed-origins` trong `application.yaml` / `application-local.yaml` cho đúng origin SPA (ví dụ `http://localhost:5173`).

## Deploy Render

1. Push code lên GitHub (repo này đã có sẵn `render.yaml` ở thư mục gốc).
2. Trên Render: **New +** → **Blueprint** → chọn repo.
3. Render sẽ tạo:
   - Web service `jwtjava-api` (build bằng `./mvnw ... package`, start bằng `java -jar ...`).
   - PostgreSQL database `jwtjava-postgres`.
4. Bắt buộc set secret:
   - `JWT_SIGNER_KEY` (Base64 256-bit)
   - `APP_SEED_ADMIN_PASSWORD`
5. App tự đọc `DATABASE_URL` của Render và convert sang JDBC PostgreSQL khi khởi động.

## License

Dự án mẫu / nội bộ — bổ sung file `LICENSE` nếu public hóa repo.
