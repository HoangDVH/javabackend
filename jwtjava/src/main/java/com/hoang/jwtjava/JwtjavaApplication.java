package com.hoang.jwtjava;

import com.hoang.jwtjava.config.CloudinaryProperties;
import com.hoang.jwtjava.config.CorsProperties;
import com.hoang.jwtjava.config.StorageProperties;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import java.net.URI;
import java.net.URISyntaxException;

@SpringBootApplication
@EnableConfigurationProperties({StorageProperties.class, CorsProperties.class, CloudinaryProperties.class})
@OpenAPIDefinition(tags = {
        @Tag(name = "Authentication", description = "Register, login, JWT introspect and refresh"),
        @Tag(name = "Categories", description = "Product categories"),
        @Tag(name = "Products", description = "Products, pagination, admin image upload"),
        @Tag(name = "Orders", description = "Create and view your orders"),
        @Tag(name = "Payments", description = "Pay for your orders"),
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
