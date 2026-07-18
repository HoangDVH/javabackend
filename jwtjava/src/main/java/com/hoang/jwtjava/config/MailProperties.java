package com.hoang.jwtjava.config;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
@ConfigurationProperties(prefix = "app.mail")
public class MailProperties {

    /** Bật gửi email thật qua SMTP (Gmail). Khi false chỉ log link reset. */
    boolean enabled = false;

    /** Địa chỉ hiển thị người gửi, ví dụ: Easy Mart &lt;shop@gmail.com&gt; */
    String from = "Easy Mart <noreply@gmail.com>";

    PasswordReset passwordReset = new PasswordReset();

    @Getter
    @Setter
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class PasswordReset {
        String frontendUrl = "http://localhost:5173/reset-password";
        int tokenTtlSeconds = 1800;
    }
}
