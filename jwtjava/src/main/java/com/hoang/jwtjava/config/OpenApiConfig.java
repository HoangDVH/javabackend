package com.hoang.jwtjava.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Configuration
public class OpenApiConfig {

    public static final String BEARER_SCHEME = "bearer-jwt";

    private static final Map<String, Integer> TAG_ORDER = Map.of(
            "Authentication", 0,
            "Categories", 1,
            "Products", 2,
            "Orders", 3,
            "Payments", 4,
            "Users", 5
    );

    @Bean
    public OpenApiCustomizer tagOrderCustomizer() {
        return openApi -> {
            if (openApi.getTags() == null || openApi.getTags().isEmpty())
                return;
            List<Tag> tags = new ArrayList<>(openApi.getTags());
            tags.sort(Comparator.comparingInt(t -> TAG_ORDER.getOrDefault(t.getName(), 99)));
            openApi.setTags(tags);
        };
    }

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("JwtJava API")
                        .description("REST API: JWT auth, categories, products, local image storage.")
                        .version("v1"))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME))
                .components(new Components()
                        .addSecuritySchemes(BEARER_SCHEME,
                                new SecurityScheme()
                                        .name(BEARER_SCHEME)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Paste the access token from POST /api/v1/auth/login (Swagger adds the Bearer prefix).")));
    }
}
