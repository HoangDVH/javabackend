package com.hoang.jwtjava.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final StorageProperties storageProperties;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String path = storageProperties.getPublicUrlPath();
        if (!path.startsWith("/"))
            path = "/" + path;
        if (!path.endsWith("/"))
            path = path + "/";

        Path dir = Path.of(storageProperties.getRoot(), "product-images").toAbsolutePath().normalize();
        registry.addResourceHandler(path + "**")
                .addResourceLocations(dir.toUri().toString());
    }
}
