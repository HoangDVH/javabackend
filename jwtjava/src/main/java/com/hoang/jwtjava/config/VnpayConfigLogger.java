package com.hoang.jwtjava.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class VnpayConfigLogger implements ApplicationRunner {

    private final VnpayProperties vnpayProperties;

    @Override
    public void run(ApplicationArguments args) {
        if (!vnpayProperties.isEnabled())
            return;

        String secret = vnpayProperties.getHashSecret();
        int secretLen = secret == null ? 0 : secret.trim().length();
        String tmn = vnpayProperties.getTmnCode() == null ? "" : vnpayProperties.getTmnCode().trim();

        log.info("VNPay enabled: tmnCode={}, hashSecretLength={}, configured={}",
                tmn, secretLen, vnpayProperties.isConfigured());

        if (secretLen != 32)
            log.warn("VNPay hash-secret length is {} (expected 32). Check application-local.yaml "
                    + "and remove VNPAY_HASH_SECRET env var if set.", secretLen);
    }
}
