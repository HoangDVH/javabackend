package com.hoang.jwtjava.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hoang.jwtjava.config.GoogleAuthProperties;
import com.hoang.jwtjava.exception.AppException;
import com.hoang.jwtjava.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Locale;

@Slf4j
@Component
@RequiredArgsConstructor
public class GoogleTokenVerifier {

    private static final String TOKEN_INFO_URL = "https://oauth2.googleapis.com/tokeninfo";

    private final GoogleAuthProperties googleAuthProperties;
    private final ObjectMapper objectMapper;

    public GoogleProfile verify(String idToken) {
        if (idToken == null || idToken.isBlank())
            throw new AppException(ErrorCode.GOOGLE_TOKEN_INVALID);

        if (googleAuthProperties.getClientIds() == null || googleAuthProperties.getClientIds().isEmpty())
            throw new AppException(ErrorCode.GOOGLE_AUTH_DISABLED);

        try {
            URI uri = UriComponentsBuilder
                    .fromUriString(TOKEN_INFO_URL)
                    .queryParam("id_token", idToken.trim())
                    .encode()
                    .build()
                    .toUri();

            String raw = restClient().get()
                    .uri(uri)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(raw);
            String audience = text(root, "aud");
            if (!googleAuthProperties.getClientIds().contains(audience)) {
                log.warn("Google token audience mismatch: {}", audience);
                throw new AppException(ErrorCode.GOOGLE_TOKEN_INVALID);
            }

            String email = text(root, "email");
            if (email == null || email.isBlank())
                throw new AppException(ErrorCode.GOOGLE_TOKEN_INVALID);

            String verified = text(root, "email_verified");
            if (verified != null && !"true".equalsIgnoreCase(verified))
                throw new AppException(ErrorCode.GOOGLE_TOKEN_INVALID);

            return new GoogleProfile(
                    email.trim().toLowerCase(Locale.ROOT),
                    text(root, "name"),
                    text(root, "picture"),
                    text(root, "sub"));
        } catch (AppException ex) {
            throw ex;
        } catch (RestClientException | java.io.IOException ex) {
            log.warn("Google token verification failed: {}", ex.getMessage());
            throw new AppException(ErrorCode.GOOGLE_TOKEN_INVALID);
        }
    }

    private RestClient restClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10_000);
        factory.setReadTimeout(10_000);
        return RestClient.builder().requestFactory(factory).build();
    }

    private static String text(JsonNode root, String field) {
        JsonNode node = root.get(field);
        return node == null || node.isNull() ? null : node.asText();
    }

    public record GoogleProfile(String email, String name, String pictureUrl, String subject) {
    }
}
