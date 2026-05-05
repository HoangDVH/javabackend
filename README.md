# Java backend (Spring Boot + JWT)

API REST cho auth JWT, danh mục, sản phẩm, upload/lưu ảnh local.

## Cấu trúc

- `jwtjava/` — module Maven Spring Boot 3 (`./mvnw` trong thư mục đó).

## Yêu cầu

- JDK 17+
- MySQL 8 (mặc định cấu hình: DB `jwtjava`, port `3307` — đổi qua biến môi trường hoặc `application-local.yaml`).

## Chạy nhanh

```bash
cd jwtjava
./mvnw spring-boot:run
```

Ứng dụng: `http://localhost:8080`.

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

## License

Dự án mẫu / nội bộ — bổ sung file `LICENSE` nếu public hóa repo.
