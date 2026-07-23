-- Product reviews schema (PostgreSQL). Idempotent.

ALTER TABLE products
    ADD COLUMN IF NOT EXISTS review_count integer NOT NULL DEFAULT 0;;

CREATE TABLE IF NOT EXISTS product_reviews (
    id              bigserial PRIMARY KEY,
    product_id      bigint       NOT NULL REFERENCES products (id),
    user_id         varchar(255) NOT NULL REFERENCES users (id),
    rating          integer      NOT NULL,
    comment         varchar(2000),
    created_at      timestamp    NOT NULL,
    updated_at      timestamp
);;

CREATE UNIQUE INDEX IF NOT EXISTS uk_product_reviews_user_product
    ON product_reviews (user_id, product_id);;

CREATE INDEX IF NOT EXISTS idx_product_reviews_product_created
    ON product_reviews (product_id, created_at);;

CREATE INDEX IF NOT EXISTS idx_product_reviews_user
    ON product_reviews (user_id);;
