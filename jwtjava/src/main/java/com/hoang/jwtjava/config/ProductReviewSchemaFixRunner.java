package com.hoang.jwtjava.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Hibernate ddl-auto=update đôi khi không thêm được cột NOT NULL trên bảng đã có dữ liệu (Postgres).
 * Runner này luôn đảm bảo {@code products.review_count} + bảng {@code product_reviews} tồn tại.
 */
@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class ProductReviewSchemaFixRunner implements ApplicationRunner {

    private final DataSource dataSource;

    @Override
    public void run(ApplicationArguments args) {
        try (Connection connection = dataSource.getConnection()) {
            String product = connection.getMetaData().getDatabaseProductName();
            boolean postgres = product != null && product.toLowerCase().contains("postgres");
            ensureReviewCountColumn(connection, postgres);
            ensureProductReviewsTable(connection, postgres);
        } catch (Exception ex) {
            log.error("Failed to ensure product review schema: {}", ex.getMessage());
        }
    }

    private void ensureReviewCountColumn(Connection connection, boolean postgres) throws Exception {
        if (columnExists(connection, "products", "review_count")) {
            log.debug("products.review_count already exists");
            return;
        }
        try (Statement statement = connection.createStatement()) {
            if (postgres) {
                statement.execute("ALTER TABLE products ADD COLUMN IF NOT EXISTS review_count integer NOT NULL DEFAULT 0");
            } else {
                statement.execute("ALTER TABLE products ADD COLUMN review_count INT NOT NULL DEFAULT 0");
            }
            log.info("Added products.review_count column");
        }
    }

    private void ensureProductReviewsTable(Connection connection, boolean postgres) throws Exception {
        if (tableExists(connection, "product_reviews")) {
            log.debug("product_reviews already exists");
            return;
        }
        try (Statement statement = connection.createStatement()) {
            if (postgres) {
                statement.execute("""
                        CREATE TABLE IF NOT EXISTS product_reviews (
                            id              bigserial PRIMARY KEY,
                            product_id      bigint       NOT NULL REFERENCES products (id),
                            user_id         varchar(255) NOT NULL REFERENCES users (id),
                            rating          integer      NOT NULL,
                            comment         varchar(2000),
                            created_at      timestamp    NOT NULL,
                            updated_at      timestamp
                        )
                        """);
                statement.execute("""
                        CREATE UNIQUE INDEX IF NOT EXISTS uk_product_reviews_user_product
                            ON product_reviews (user_id, product_id)
                        """);
                statement.execute("""
                        CREATE INDEX IF NOT EXISTS idx_product_reviews_product_created
                            ON product_reviews (product_id, created_at)
                        """);
                statement.execute("""
                        CREATE INDEX IF NOT EXISTS idx_product_reviews_user
                            ON product_reviews (user_id)
                        """);
            } else {
                statement.execute("""
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
                        )
                        """);
            }
            log.info("Created product_reviews table");
        }
    }

    private static boolean columnExists(Connection connection, String table, String column) throws Exception {
        DatabaseMetaData meta = connection.getMetaData();
        String catalog = connection.getCatalog();
        String schema = connection.getSchema();
        try (ResultSet rs = meta.getColumns(catalog, schema, table, column)) {
            if (rs.next())
                return true;
        }
        // Postgres often lowercases identifiers; also try uppercase table for some drivers.
        try (ResultSet rs = meta.getColumns(catalog, schema, table.toLowerCase(), column.toLowerCase())) {
            if (rs.next())
                return true;
        }
        try (ResultSet rs = meta.getColumns(catalog, schema, table.toUpperCase(), column.toUpperCase())) {
            return rs.next();
        }
    }

    private static boolean tableExists(Connection connection, String table) throws Exception {
        DatabaseMetaData meta = connection.getMetaData();
        String catalog = connection.getCatalog();
        String schema = connection.getSchema();
        try (ResultSet rs = meta.getTables(catalog, schema, table, new String[]{"TABLE"})) {
            if (rs.next())
                return true;
        }
        try (ResultSet rs = meta.getTables(catalog, schema, table.toLowerCase(), new String[]{"TABLE"})) {
            if (rs.next())
                return true;
        }
        try (ResultSet rs = meta.getTables(catalog, schema, table.toUpperCase(), new String[]{"TABLE"})) {
            return rs.next();
        }
    }
}
