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

    Smtp smtp = new Smtp();

    PasswordReset passwordReset = new PasswordReset();

    @Getter
    @Setter
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class Smtp {
        boolean enabled = false;
        String host = "smtp.gmail.com";
        int port = 587;
        String username = "";
        String password = "";
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
