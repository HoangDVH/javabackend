package com.hoang.jwtjava.config;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
@ConfigurationProperties(prefix = "app.cache.catalog")
public class CatalogCacheProperties {

    boolean enabled = true;

    int productListTtlSeconds = 120;

    int productDetailTtlSeconds = 120;

    int categoryListTtlSeconds = 1800;

    int categoryDetailTtlSeconds = 1800;

    /** Redis lock TTL for cache stampede protection (seconds). */
    int stampedeLockSeconds = 5;

    /** How long waiters poll for a filled cache after missing the lock (milliseconds). */
    int stampedeWaitMs = 400;
}
