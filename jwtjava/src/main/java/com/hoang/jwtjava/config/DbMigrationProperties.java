package com.hoang.jwtjava.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.db")
public class DbMigrationProperties {
    private boolean runSqlMigrationsOnStartup = false;
}
