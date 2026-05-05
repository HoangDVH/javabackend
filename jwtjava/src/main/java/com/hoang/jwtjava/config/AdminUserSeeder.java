package com.hoang.jwtjava.config;

import com.hoang.jwtjava.entity.User;
import com.hoang.jwtjava.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Tạo tài khoản admin mặc định khi chưa có trong DB (chỉ chạy một lần cho mỗi email).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AdminUserSeeder implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed-admin.email}")
    private String adminEmail;

    @Value("${app.seed-admin.password}")
    private String adminPassword;

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.existsByEmail(adminEmail)) {
            return;
        }
        User admin = User.builder()
                .email(adminEmail)
                .password(passwordEncoder.encode(adminPassword))
                .roles(Set.of("ADMIN"))
                .build();
        userRepository.save(admin);
        log.info("Đã tạo user admin: {} (role ADMIN). Đổi mật khẩu sau khi đăng nhập nếu cần.", adminEmail);
    }
}
