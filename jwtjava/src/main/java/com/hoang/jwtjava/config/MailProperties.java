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

    Brevo brevo = new Brevo();

    PasswordReset passwordReset = new PasswordReset();

    @Getter
    @Setter
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class Brevo {
        boolean enabled = false;
        String apiKey = "";
        /** Ví dụ: {@code Easy Mart <you@gmail.com>} hoặc chỉ {@code you@gmail.com}. */
        String from = "";
    }

    @Getter
    @Setter
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class PasswordReset {
        String frontendUrl = "https://easy-mart-vert.vercel.app/reset-password";
        int tokenTtlSeconds = 1800;
    }
}
