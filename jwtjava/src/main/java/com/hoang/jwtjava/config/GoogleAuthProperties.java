package com.hoang.jwtjava.config;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
@ConfigurationProperties(prefix = "app.google")
public class GoogleAuthProperties {

    boolean enabled = false;

    /**
     * Google OAuth Web Client ID(s). Comma-separated env {@code GOOGLE_CLIENT_ID} is split here.
     */
    List<String> clientIds = new ArrayList<>();

    public void setClientId(String clientId) {
        if (clientId == null || clientId.isBlank()) {
            this.clientIds = new ArrayList<>();
            return;
        }
        List<String> parsed = new ArrayList<>();
        for (String part : clientId.split(",")) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty())
                parsed.add(trimmed);
        }
        this.clientIds = parsed;
    }

    public void setClientIds(List<String> clientIds) {
        this.clientIds = clientIds != null ? new ArrayList<>(clientIds) : new ArrayList<>();
    }
}
