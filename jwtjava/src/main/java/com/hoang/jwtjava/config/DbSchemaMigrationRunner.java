package com.hoang.jwtjava.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.db.run-sql-migrations-on-startup", havingValue = "true")
public class DbSchemaMigrationRunner implements ApplicationRunner {

    private final DataSource dataSource;
    private final DbMigrationProperties dbMigrationProperties;

    @Override
    public void run(ApplicationArguments args) {
        if (!dbMigrationProperties.isRunSqlMigrationsOnStartup())
            return;

        String dialect = detectDialect();
        log.info("Running SQL migrations for dialect: {}", dialect);

        if ("postgres".equals(dialect)) {
            runScript("sql/add_query_indexes_postgres.sql", dialect);
            runScript("sql/add_audit_triggers_postgres.sql", dialect);
        } else {
            runScript("sql/add_query_indexes.sql", dialect);
            log.info("MySQL triggers: chạy thủ công scripts/run-audit-triggers.ps1 (DELIMITER không chạy qua JDBC).");
        }

        log.info("SQL migrations finished");
    }

    private void runScript(String classpathLocation, String dialect) {
        try {
            var populator = new ResourceDatabasePopulator();
            populator.addScript(new ClassPathResource(classpathLocation));
            populator.setContinueOnError(true);
            if ("postgres".equals(dialect))
                populator.setSeparator(";;");
            populator.execute(dataSource);
            log.info("Applied script: {}", classpathLocation);
        } catch (Exception ex) {
            log.warn("Script {} had errors (may be idempotent re-run): {}", classpathLocation, ex.getMessage());
        }
    }

    private String detectDialect() {
        try (Connection connection = dataSource.getConnection()) {
            String product = connection.getMetaData().getDatabaseProductName();
            if (product != null && product.toLowerCase().contains("postgres"))
                return "postgres";
            return "mysql";
        } catch (Exception ex) {
            log.warn("Could not detect DB dialect, defaulting to mysql: {}", ex.getMessage());
            return "mysql";
        }
    }
}
