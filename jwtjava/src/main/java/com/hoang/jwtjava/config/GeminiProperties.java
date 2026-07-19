package com.hoang.jwtjava.config;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
@ConfigurationProperties(prefix = "app.gemini")
public class GeminiProperties {

    boolean enabled = false;
    String apiKey = "";
    String model = "gemini-2.5-flash";
    String apiBaseUrl = "https://generativelanguage.googleapis.com/v1beta";
    int timeoutSeconds = 20;
    int maxCatalogProducts = 12;
    int descriptionMaxChars = 160;
    int maxOutputTokens = 1024;
    History history = new History();

    @Getter
    @Setter
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class History {
        int maxTurns = 3;
        int ttlSeconds = 1800;
    }
}
