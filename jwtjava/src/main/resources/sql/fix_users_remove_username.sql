-- Lỗi: Field 'username' doesn't have a default value
-- Nguyên nhân: bảng `users` vẫn có cột `username` NOT NULL nhưng app chỉ INSERT `email`.
-- Chạy script này trên đúng database (đổi tên schema nếu cần).

-- USE jwtjava;

-- 1) Nếu có dòng cũ: email trống nhưng còn username — copy sang email (chỉ khi cột username tồn tại)
-- UPDATE users SET email = username
-- WHERE (email IS NULL OR TRIM(email) = '') AND username IS NOT NULL;

-- 2) Bỏ cột gây lỗi INSERT (bắt buộc chạy dòng này)
ALTER TABLE users DROP COLUMN username;

-- 3) Các cột profile cũ — chỉ bỏ comment và chạy nếu DESCRIBE users còn thấy cột đó
-- ALTER TABLE users DROP COLUMN first_name;
-- ALTER TABLE users DROP COLUMN last_name;
-- ALTER TABLE users DROP COLUMN dob;

-- 4) Đảm bảo email bắt buộc + unique (điều chỉnh nếu đã đúng)
-- ALTER TABLE users MODIFY COLUMN email VARCHAR(255) NOT NULL;
-- CREATE UNIQUE INDEX uk_users_email ON users (email);  -- bỏ qua nếu đã có UNIQUE
