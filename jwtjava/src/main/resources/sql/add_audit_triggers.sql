-- Audit tables + triggers (Phase 1): stock guard, order status history, stock history, updated_at.
-- Chạy thủ công trên MySQL (dev/Render). Bỏ qua lỗi "already exists" / "Duplicate column".
-- Trigger có BEGIN...END — nên chạy bằng mysql client hoặc Workbench (hỗ trợ DELIMITER).

-- USE jwtjava;

-- ---------------------------------------------------------------------------
-- 1) Bảng audit
-- ---------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS order_status_history (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    order_id    BIGINT       NOT NULL,
    old_status  VARCHAR(32)  NULL,
    new_status  VARCHAR(32)  NOT NULL,
    changed_at  DATETIME(6)  NOT NULL,
    changed_by  VARCHAR(255) NULL,
    PRIMARY KEY (id),
    INDEX idx_order_status_history_order_changed (order_id, changed_at),
    CONSTRAINT fk_order_status_history_order
        FOREIGN KEY (order_id) REFERENCES customer_orders (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS product_stock_history (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    product_id  BIGINT       NOT NULL,
    old_stock   INT          NOT NULL,
    new_stock   INT          NOT NULL,
    delta       INT          NOT NULL,
    changed_at  DATETIME(6)  NOT NULL,
    source      VARCHAR(32)  NULL,
    PRIMARY KEY (id),
    INDEX idx_product_stock_history_product_changed (product_id, changed_at),
    CONSTRAINT fk_product_stock_history_product
        FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE CASCADE
);

-- ---------------------------------------------------------------------------
-- 2) Cột updated_at (trigger tự gán khi UPDATE)
-- ---------------------------------------------------------------------------

ALTER TABLE products ADD COLUMN updated_at DATETIME(6) NULL;
ALTER TABLE customer_orders ADD COLUMN updated_at DATETIME(6) NULL;

-- ---------------------------------------------------------------------------
-- 3) Triggers — drop trước khi tạo lại (idempotent khi chạy lại script)
-- ---------------------------------------------------------------------------

DROP TRIGGER IF EXISTS trg_products_before_update;
DROP TRIGGER IF EXISTS trg_products_stock_audit;
DROP TRIGGER IF EXISTS trg_orders_before_update;
DROP TRIGGER IF EXISTS trg_orders_status_audit_insert;
DROP TRIGGER IF EXISTS trg_orders_status_audit_update;

DELIMITER //

-- products: cấm stock âm + set updated_at
CREATE TRIGGER trg_products_before_update
BEFORE UPDATE ON products
FOR EACH ROW
BEGIN
    IF NEW.stock < 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'stock cannot be negative';
    END IF;
    SET NEW.updated_at = NOW(6);
END//

-- products: log mỗi lần stock thay đổi
CREATE TRIGGER trg_products_stock_audit
AFTER UPDATE ON products
FOR EACH ROW
BEGIN
    IF OLD.stock <> NEW.stock THEN
        INSERT INTO product_stock_history (product_id, old_stock, new_stock, delta, changed_at)
        VALUES (NEW.id, OLD.stock, NEW.stock, NEW.stock - OLD.stock, NOW(6));
    END IF;
END//

-- customer_orders: set updated_at
CREATE TRIGGER trg_orders_before_update
BEFORE UPDATE ON customer_orders
FOR EACH ROW
BEGIN
    SET NEW.updated_at = NOW(6);
END//

-- customer_orders: log status khi tạo đơn
CREATE TRIGGER trg_orders_status_audit_insert
AFTER INSERT ON customer_orders
FOR EACH ROW
BEGIN
    INSERT INTO order_status_history (order_id, old_status, new_status, changed_at)
    VALUES (NEW.id, NULL, NEW.status, NOW(6));
END//

-- customer_orders: log khi đổi status (vd. PENDING_PAYMENT → PAID)
CREATE TRIGGER trg_orders_status_audit_update
AFTER UPDATE ON customer_orders
FOR EACH ROW
BEGIN
    IF OLD.status <> NEW.status THEN
        INSERT INTO order_status_history (order_id, old_status, new_status, changed_at)
        VALUES (NEW.id, OLD.status, NEW.status, NOW(6));
    END IF;
END//

DELIMITER ;
