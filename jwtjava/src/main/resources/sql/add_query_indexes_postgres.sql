-- Index cho PostgreSQL (Neon/Supabase trên Render). Idempotent: IF NOT EXISTS.

CREATE INDEX IF NOT EXISTS idx_products_category_created ON products (category_id, created_at);;
CREATE INDEX IF NOT EXISTS idx_products_brand_id ON products (brand_id);;
CREATE INDEX IF NOT EXISTS idx_products_seller_created ON products (seller_email, created_at);;

CREATE INDEX IF NOT EXISTS idx_orders_user_created ON customer_orders (user_id, created_at);;
CREATE INDEX IF NOT EXISTS idx_payments_order_created ON order_payments (order_id, created_at);;

CREATE INDEX IF NOT EXISTS idx_password_reset_email ON password_reset_tokens (email);;
CREATE INDEX IF NOT EXISTS idx_password_reset_expires ON password_reset_tokens (expires_at);;
CREATE INDEX IF NOT EXISTS idx_invalidated_tokens_expires ON invalidated_tokens (expires_at);;
