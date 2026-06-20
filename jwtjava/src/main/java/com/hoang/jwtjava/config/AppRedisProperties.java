package com.hoang.jwtjava.config;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
@ConfigurationProperties(prefix = "app.redis")
public class AppRedisProperties {

    boolean enabled = true;

    String host = "localhost";

    int port = 6379;

    String password = "";

    /** When Redis is unavailable, treat tokens as not revoked (still falls back to DB when possible). */
    boolean blacklistFailOpen = true;
}
