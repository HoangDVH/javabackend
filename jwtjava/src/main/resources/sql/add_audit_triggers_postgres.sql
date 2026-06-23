-- Audit + triggers cho PostgreSQL (Render). Statement separator: ;;

CREATE TABLE IF NOT EXISTS order_status_history (
    id          BIGSERIAL PRIMARY KEY,
    order_id    BIGINT       NOT NULL REFERENCES customer_orders (id) ON DELETE CASCADE,
    old_status  VARCHAR(32),
    new_status  VARCHAR(32)  NOT NULL,
    changed_at  TIMESTAMP(6) NOT NULL DEFAULT NOW(),
    changed_by  VARCHAR(255)
);;
CREATE INDEX IF NOT EXISTS idx_order_status_history_order_changed
    ON order_status_history (order_id, changed_at);;

CREATE TABLE IF NOT EXISTS product_stock_history (
    id          BIGSERIAL PRIMARY KEY,
    product_id  BIGINT       NOT NULL REFERENCES products (id) ON DELETE CASCADE,
    old_stock   INT          NOT NULL,
    new_stock   INT          NOT NULL,
    delta       INT          NOT NULL,
    changed_at  TIMESTAMP(6) NOT NULL DEFAULT NOW(),
    source      VARCHAR(32)
);;
CREATE INDEX IF NOT EXISTS idx_product_stock_history_product_changed
    ON product_stock_history (product_id, changed_at);;

ALTER TABLE products ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP(6);;
ALTER TABLE customer_orders ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP(6);;

CREATE OR REPLACE FUNCTION trg_products_before_update_fn()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.stock < 0 THEN
        RAISE EXCEPTION 'stock cannot be negative';
    END IF;
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;;

DROP TRIGGER IF EXISTS trg_products_before_update ON products;;
CREATE TRIGGER trg_products_before_update
    BEFORE UPDATE ON products
    FOR EACH ROW EXECUTE FUNCTION trg_products_before_update_fn();;

CREATE OR REPLACE FUNCTION trg_products_stock_audit_fn()
RETURNS TRIGGER AS $$
BEGIN
    IF OLD.stock IS DISTINCT FROM NEW.stock THEN
        INSERT INTO product_stock_history (product_id, old_stock, new_stock, delta, changed_at)
        VALUES (NEW.id, OLD.stock, NEW.stock, NEW.stock - OLD.stock, NOW());
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;;

DROP TRIGGER IF EXISTS trg_products_stock_audit ON products;;
CREATE TRIGGER trg_products_stock_audit
    AFTER UPDATE ON products
    FOR EACH ROW EXECUTE FUNCTION trg_products_stock_audit_fn();;

CREATE OR REPLACE FUNCTION trg_orders_before_update_fn()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;;

DROP TRIGGER IF EXISTS trg_orders_before_update ON customer_orders;;
CREATE TRIGGER trg_orders_before_update
    BEFORE UPDATE ON customer_orders
    FOR EACH ROW EXECUTE FUNCTION trg_orders_before_update_fn();;

CREATE OR REPLACE FUNCTION trg_orders_status_audit_insert_fn()
RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO order_status_history (order_id, old_status, new_status, changed_at)
    VALUES (NEW.id, NULL, NEW.status, NOW());
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;;

DROP TRIGGER IF EXISTS trg_orders_status_audit_insert ON customer_orders;;
CREATE TRIGGER trg_orders_status_audit_insert
    AFTER INSERT ON customer_orders
    FOR EACH ROW EXECUTE FUNCTION trg_orders_status_audit_insert_fn();;

CREATE OR REPLACE FUNCTION trg_orders_status_audit_update_fn()
RETURNS TRIGGER AS $$
BEGIN
    IF OLD.status IS DISTINCT FROM NEW.status THEN
        INSERT INTO order_status_history (order_id, old_status, new_status, changed_at)
        VALUES (NEW.id, OLD.status, NEW.status, NOW());
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;;

DROP TRIGGER IF EXISTS trg_orders_status_audit_update ON customer_orders;;
CREATE TRIGGER trg_orders_status_audit_update
    AFTER UPDATE ON customer_orders
    FOR EACH ROW EXECUTE FUNCTION trg_orders_status_audit_update_fn();;
