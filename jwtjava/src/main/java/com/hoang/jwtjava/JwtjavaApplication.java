package com.hoang.jwtjava;

import com.hoang.jwtjava.config.AppRedisProperties;
import com.hoang.jwtjava.config.CatalogCacheProperties;
import com.hoang.jwtjava.config.CloudinaryProperties;
import com.hoang.jwtjava.config.CorsProperties;
import com.hoang.jwtjava.config.DbMigrationProperties;
import com.hoang.jwtjava.config.MailProperties;
import com.hoang.jwtjava.config.VnpayProperties;
import com.hoang.jwtjava.config.RateLimitProperties;
import com.hoang.jwtjava.config.StorageProperties;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import java.net.URI;
import java.net.URISyntaxException;

@SpringBootApplication
@EnableConfigurationProperties({
        StorageProperties.class,
        CorsProperties.class,
        CloudinaryProperties.class,
        AppRedisProperties.class,
        CatalogCacheProperties.class,
        RateLimitProperties.class,
        MailProperties.class,
        VnpayProperties.class,
        DbMigrationProperties.class
})
@OpenAPIDefinition(tags = {
        @Tag(name = "Authentication", description = "Register, login, introspect, refresh (cookie), logout, forgot/reset password. Rate limit qua Redis."),
        @Tag(name = "Categories", description = "Danh mục sản phẩm (public). Redis cache list/detail TTL 30 phút."),
        @Tag(name = "Products", description = "Sản phẩm: list/filter/pagination, detail, CRUD seller/admin, upload ảnh. Redis cache list/detail TTL 2 phút."),
        @Tag(name = "Orders", description = "Create and view your orders"),
        @Tag(name = "Payments", description = "Pay for your orders (mock cash/COD or VNPay gateway)"),
        @Tag(name = "Users", description = "User management (admin)")
})
public class JwtjavaApplication {

	public static void main(String[] args) {
        configureDatasourceFromRenderDatabaseUrl();
		SpringApplication.run(JwtjavaApplication.class, args);
	}

    private static void configureDatasourceFromRenderDatabaseUrl() {
        if (System.getenv("JWTJAVA_DATASOURCE_URL") != null || System.getProperty("JWTJAVA_DATASOURCE_URL") != null)
            return;

        String databaseUrl = System.getenv("DATABASE_URL");
        if (databaseUrl == null || databaseUrl.isBlank())
            return;

        try {
            URI uri = new URI(databaseUrl);
            String scheme = uri.getScheme();
            if (!"postgres".equalsIgnoreCase(scheme) && !"postgresql".equalsIgnoreCase(scheme))
                return;

            String host = uri.getHost();
            int port = uri.getPort() > 0 ? uri.getPort() : 5432;
            String database = uri.getPath() != null ? uri.getPath().replaceFirst("^/", "") : "";
            String jdbcUrl = "jdbc:postgresql://" + host + ":" + port + "/" + database;
            if (uri.getQuery() != null && !uri.getQuery().isBlank())
                jdbcUrl += "?" + uri.getQuery();

            System.setProperty("JWTJAVA_DATASOURCE_URL", jdbcUrl);

            String userInfo = uri.getUserInfo();
            if (userInfo != null && !userInfo.isBlank()) {
                String[] parts = userInfo.split(":", 2);
                if (parts.length > 0 && !parts[0].isBlank())
                    System.setProperty("JWTJAVA_DATASOURCE_USERNAME", parts[0]);
                if (parts.length == 2 && !parts[1].isBlank())
                    System.setProperty("JWTJAVA_DATASOURCE_PASSWORD", parts[1]);
            }
        } catch (URISyntaxException ignored) {
            // Keep default datasource config if DATABASE_URL is malformed.
        }
    }
}
