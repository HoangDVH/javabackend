package com.hoang.jwtjava.config;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
@ConfigurationProperties(prefix = "app.rate-limit")
public class RateLimitProperties {

    boolean enabled = true;

    EndpointLimit login = new EndpointLimit(10, 60);

    EndpointLimit loginByEmail = new EndpointLimit(5, 60);

    EndpointLimit refresh = new EndpointLimit(20, 60);

    EndpointLimit register = new EndpointLimit(5, 300);

    EndpointLimit forgotPassword = new EndpointLimit(5, 900);

    EndpointLimit forgotPasswordByEmail = new EndpointLimit(3, 900);

    EndpointLimit resetPassword = new EndpointLimit(10, 60);

    @Getter
    @Setter
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class EndpointLimit {
        int maxRequests = 10;
        int windowSeconds = 60;

        public EndpointLimit() {
        }

        public EndpointLimit(int maxRequests, int windowSeconds) {
            this.maxRequests = maxRequests;
            this.windowSeconds = windowSeconds;
        }
    }
}
