package com.hoang.jwtjava.config;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Cấu hình cổng thanh toán VNPay (v2.1.0).
 * Bật khi {@code enabled=true} và đã set đủ tmn-code, hash-secret, return-url, ipn-url.
 */
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
@ConfigurationProperties(prefix = "app.vnpay")
public class VnpayProperties {

    boolean enabled = false;

    String tmnCode = "";

    String hashSecret = "";

    String payUrl = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html";

    String version = "2.1.0";

    String command = "pay";

    String currCode = "VND";

    String locale = "vn";

    String orderType = "other";

    /** URL frontend nhận redirect sau khi khách thanh toán trên VNPay. */
    String returnUrl = "";

    /** URL public backend nhận IPN (server-to-server) từ VNPay. */
    String ipnUrl = "";

    /** Thời gian hết hạn link thanh toán (phút). */
    int expireMinutes = 15;

    public boolean isConfigured() {
        return enabled
                && tmnCode != null && !tmnCode.isBlank()
                && hashSecret != null && !hashSecret.isBlank()
                && returnUrl != null && !returnUrl.isBlank()
                && ipnUrl != null && !ipnUrl.isBlank();
    }
}
