package com.hoang.jwtjava.config;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
@ConfigurationProperties(prefix = "app.storage")
public class StorageProperties {

    /**
     * Thư mục gốc trên đĩa (chứa thư mục con product-images).
     */
    String root = "uploads";

    /**
     * Đường dẫn URL công khai (ví dụ /files/product-images) khớp với ResourceHandler.
     */
    String publicUrlPath = "/files/product-images";

    /**
     * Nếu đặt (ví dụ http://localhost:8080), chuỗi lưu DB là URL đầy đủ; để trống thì chỉ lưu path bắt đầu bằng /.
     */
    String publicBaseUrl = "";

    long maxBytes = 5L * 1024 * 1024;

    int downloadTimeoutSeconds = 30;
}
