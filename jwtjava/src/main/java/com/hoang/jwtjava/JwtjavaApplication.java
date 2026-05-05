package com.hoang.jwtjava;

import com.hoang.jwtjava.config.CorsProperties;
import com.hoang.jwtjava.config.StorageProperties;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({StorageProperties.class, CorsProperties.class})
@OpenAPIDefinition(tags = {
        @Tag(name = "Authentication", description = "Register, login, JWT introspect and refresh"),
        @Tag(name = "Categories", description = "Product categories"),
        @Tag(name = "Products", description = "Products, pagination, admin image upload"),
        @Tag(name = "Users", description = "User management (admin)")
})
public class JwtjavaApplication {

	public static void main(String[] args) {
		SpringApplication.run(JwtjavaApplication.class, args);
	}

}
