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

    Resend resend = new Resend();

    PasswordReset passwordReset = new PasswordReset();

    @Getter
    @Setter
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class Resend {
        boolean enabled = false;
        String apiKey = "";
        String from = "Easy Mart <onboarding@resend.dev>";
    }

    @Getter
    @Setter
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class PasswordReset {
        String frontendUrl = "https://easy-mart-vert.vercel.app/reset-password";
        int tokenTtlSeconds = 1800;
    }
}
