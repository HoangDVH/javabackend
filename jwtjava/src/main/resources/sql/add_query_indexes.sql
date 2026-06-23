-- Index cho các query thường dùng (catalog, đơn hàng, token).
-- Chạy thủ công trên DB đã tồn tại nếu Hibernate ddl-auto không tự tạo.
-- Bỏ qua dòng nào báo lỗi "Duplicate key name" (index đã có).

-- USE jwtjava;

-- products: lọc catalog + seller dashboard
CREATE INDEX idx_products_category_created ON products (category_id, created_at);
CREATE INDEX idx_products_brand_id ON products (brand_id);
CREATE INDEX idx_products_seller_created ON products (seller_email, created_at);

-- customer_orders: lịch sử đơn theo user
CREATE INDEX idx_orders_user_created ON customer_orders (user_id, created_at);

-- order_payments: thanh toán theo đơn + sort theo thời gian
CREATE INDEX idx_payments_order_created ON order_payments (order_id, created_at);

-- password_reset_tokens: lookup theo email, cleanup theo hạn
CREATE INDEX idx_password_reset_email ON password_reset_tokens (email);
CREATE INDEX idx_password_reset_expires ON password_reset_tokens (expires_at);

-- invalidated_tokens: cleanup token hết hạn
CREATE INDEX idx_invalidated_tokens_expires ON invalidated_tokens (expires_at);
