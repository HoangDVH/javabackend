package com.hoang.jwtjava.service;

import com.hoang.jwtjava.config.MailProperties;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MailService {

    private final MailProperties mailProperties;
    private final JavaMailSender mailSender;

    public void sendPasswordResetEmail(String toEmail, String rawToken) {
        String resetLink = buildResetLink(rawToken);
        MailProperties.Smtp smtp = mailProperties.getSmtp();

        if (!smtp.isEnabled()) {
            log.info("Mail disabled — password reset link for {}: {}", toEmail, resetLink);
            return;
        }

        if (isBlank(smtp.getUsername()) || isBlank(smtp.getPassword())) {
            log.warn("MAIL_SMTP_USERNAME/PASSWORD missing — password reset link for {}: {}", toEmail, resetLink);
            return;
        }

        String from = resolveFrom(smtp);
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
            helper.setFrom(from);
            helper.setTo(toEmail);
            helper.setSubject("Đặt lại mật khẩu Easy Mart");
            helper.setText(buildResetEmailHtml(resetLink), true);
            mailSender.send(message);
            log.info("Password reset email sent to {}", toEmail);
        } catch (MessagingException | MailException ex) {
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

    private static String resolveFrom(MailProperties.Smtp smtp) {
        if (!isBlank(smtp.getFrom()))
            return smtp.getFrom().trim();
        return smtp.getUsername().trim();
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
}
