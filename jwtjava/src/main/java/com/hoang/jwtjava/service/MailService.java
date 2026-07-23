package com.hoang.jwtjava.service;

import com.hoang.jwtjava.config.MailProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class MailService {

    private static final String BREVO_SEND_URL = "https://api.brevo.com/v3/smtp/email";
    private static final Pattern FROM_PATTERN = Pattern.compile("^(.*)<([^>]+)>\\s*$");

    private final MailProperties mailProperties;
    private final RestClient restClient;

    public MailService(MailProperties mailProperties) {
        this.mailProperties = mailProperties;
        this.restClient = RestClient.create();
    }

    public void sendPasswordResetEmail(String toEmail, String rawToken) {
        String resetLink = buildResetLink(rawToken);
        MailProperties.Brevo brevo = mailProperties.getBrevo();

        if (!brevo.isEnabled()) {
            log.info("Mail disabled — password reset link for {}: {}", toEmail, resetLink);
            return;
        }

        if (isBlank(brevo.getApiKey())) {
            log.warn("BREVO_API_KEY missing — password reset link for {}: {}", toEmail, resetLink);
            return;
        }

        Sender sender = parseSender(brevo.getFrom());
        if (sender == null) {
            log.warn("MAIL_FROM missing/invalid — password reset link for {}: {}", toEmail, resetLink);
            return;
        }

        Map<String, Object> body = Map.of(
                "sender", Map.of(
                        "name", sender.name(),
                        "email", sender.email()),
                "to", List.of(Map.of("email", toEmail)),
                "subject", "Đặt lại mật khẩu Easy Mart",
                "htmlContent", buildResetEmailHtml(resetLink));

        try {
            restClient.post()
                    .uri(BREVO_SEND_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("api-key", brevo.getApiKey().trim())
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
            log.info("Password reset email sent to {}", toEmail);
        } catch (RestClientException ex) {
            log.error("Failed to send password reset email to {}: {}", toEmail, ex.getMessage());
            throw new IllegalStateException("Failed to send password reset email", ex);
        }
    }

    private String buildResetLink(String rawToken) {
        String base = mailProperties.getPasswordReset().getFrontendUrl();
        if (base.contains("?"))
            return base + "&token=" + rawToken;
        return base + "?token=" + rawToken;
    }

    static Sender parseSender(String from) {
        if (isBlank(from))
            return null;
        String trimmed = from.trim();
        Matcher matcher = FROM_PATTERN.matcher(trimmed);
        if (matcher.matches()) {
            String name = matcher.group(1).trim();
            String email = matcher.group(2).trim();
            if (isBlank(email))
                return null;
            return new Sender(isBlank(name) ? email : name, email);
        }
        if (!trimmed.contains("@"))
            return null;
        return new Sender(trimmed, trimmed);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String buildResetEmailHtml(String resetLink) {
        return """
                <p>Xin chào,</p>
                <p>Bạn vừa yêu cầu đặt lại mật khẩu cho tài khoản Easy Mart.</p>
                <p><a href="%s">Nhấn vào đây để đặt lại mật khẩu</a></p>
                <p>Link có hiệu lực trong 30 phút. Nếu bạn không yêu cầu, hãy bỏ qua email này.</p>
                """.formatted(resetLink);
    }

    record Sender(String name, String email) {
    }
}
