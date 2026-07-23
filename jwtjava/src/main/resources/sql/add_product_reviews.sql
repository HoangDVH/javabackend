-- Product reviews schema (MySQL). Idempotent-ish via continueOnError.

ALTER TABLE products
    ADD COLUMN review_count INT NOT NULL DEFAULT 0;

CREATE TABLE IF NOT EXISTS product_reviews (
    id              BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    product_id      BIGINT       NOT NULL,
    user_id         VARCHAR(255) NOT NULL,
    rating          INT          NOT NULL,
    comment         VARCHAR(2000),
    created_at      DATETIME(6)  NOT NULL,
    updated_at      DATETIME(6),
    CONSTRAINT fk_product_reviews_product FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT fk_product_reviews_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT uk_product_reviews_user_product UNIQUE (user_id, product_id)
);

CREATE INDEX idx_product_reviews_product_created ON product_reviews (product_id, created_at);
CREATE INDEX idx_product_reviews_user ON product_reviews (user_id);
