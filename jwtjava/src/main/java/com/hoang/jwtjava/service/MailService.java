package com.hoang.jwtjava.service;

import com.hoang.jwtjava.config.MailProperties;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MailService {

    private final MailProperties mailProperties;
    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    public void sendPasswordResetEmail(String toEmail, String rawToken) {
        String resetLink = buildResetLink(rawToken);
        if (!mailProperties.isEnabled()) {
            log.info("Mail disabled — password reset link for {}: {}", toEmail, resetLink);
            return;
        }

        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null || !hasSmtpCredentials(mailSender)) {
            log.warn("SMTP credentials missing (MAIL_USERNAME / MAIL_PASSWORD) — password reset link for {}: {}",
                    toEmail, resetLink);
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
            helper.setFrom(mailProperties.getFrom());
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

    private static boolean hasSmtpCredentials(JavaMailSender mailSender) {
        if (!(mailSender instanceof JavaMailSenderImpl impl))
            return true;
        String username = impl.getUsername();
        String password = impl.getPassword();
        return username != null && !username.isBlank()
                && password != null && !password.isBlank();
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
