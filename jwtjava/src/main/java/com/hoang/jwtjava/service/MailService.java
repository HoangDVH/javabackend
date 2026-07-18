package com.hoang.jwtjava.service;

import com.hoang.jwtjava.config.MailProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class MailService {

    private static final String RESEND_API_URL = "https://api.resend.com/emails";

    private final MailProperties mailProperties;
    private final RestClient restClient;

    public MailService(MailProperties mailProperties) {
        this.mailProperties = mailProperties;
        this.restClient = RestClient.create();
    }

    public void sendPasswordResetEmail(String toEmail, String rawToken) {
        String resetLink = buildResetLink(rawToken);
        if (!mailProperties.getResend().isEnabled()) {
            log.info("Mail disabled — password reset link for {}: {}", toEmail, resetLink);
            return;
        }

        String apiKey = mailProperties.getResend().getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("RESEND_API_KEY missing — password reset link for {}: {}", toEmail, resetLink);
            return;
        }

        Map<String, Object> body = Map.of(
                "from", mailProperties.getResend().getFrom(),
                "to", List.of(toEmail),
                "subject", "Đặt lại mật khẩu Easy Mart",
                "html", buildResetEmailHtml(resetLink));

        try {
            restClient.post()
                    .uri(RESEND_API_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + apiKey)
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

    private static String buildResetEmailHtml(String resetLink) {
        return """
                <p>Xin chào,</p>
                <p>Bạn vừa yêu cầu đặt lại mật khẩu cho tài khoản Easy Mart.</p>
                <p><a href="%s">Nhấn vào đây để đặt lại mật khẩu</a></p>
                <p>Link có hiệu lực trong 30 phút. Nếu bạn không yêu cầu, hãy bỏ qua email này.</p>
                """.formatted(resetLink);
    }
}
