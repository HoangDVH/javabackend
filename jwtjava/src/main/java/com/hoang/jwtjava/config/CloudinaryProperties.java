package com.hoang.jwtjava.config;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Upload ảnh sản phẩm lên Cloudinary. Bật khi {@code enabled=true} và đã set đủ cloud-name / api-key / api-secret.
 */
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
@ConfigurationProperties(prefix = "app.cloudinary")
public class CloudinaryProperties {

    boolean enabled = false;

    String cloudName = "";

    String apiKey = "";

    String apiSecret = "";

    /** Thư mục trên Cloudinary (optional). */
    String folder = "jwtjava/products";
}
