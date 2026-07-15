package com.hoang.jwtjava.service;

import com.hoang.jwtjava.config.VnpayProperties;
import com.hoang.jwtjava.dto.request.VnpayPaymentInitRequest;
import com.hoang.jwtjava.dto.response.VnpayIpnResponse;
import com.hoang.jwtjava.dto.response.VnpayPaymentInitResponse;
import com.hoang.jwtjava.entity.Order;
import com.hoang.jwtjava.entity.OrderStatus;
import com.hoang.jwtjava.entity.Payment;
import com.hoang.jwtjava.entity.PaymentStatus;
import com.hoang.jwtjava.exception.AppException;
import com.hoang.jwtjava.exception.ErrorCode;
import com.hoang.jwtjava.repository.OrderRepository;
import com.hoang.jwtjava.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class VnpayPaymentService {

    private static final String METHOD_VNPAY = "VNPAY";
    private static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter VNPAY_DATE = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final VnpayProperties vnpayProperties;
    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;

    @Transactional
    public VnpayPaymentInitResponse initiatePayment(String userEmail, VnpayPaymentInitRequest request, String clientIp) {
        if (!vnpayProperties.isConfigured())
            throw new AppException(ErrorCode.PAYMENT_GATEWAY_ERROR);

        Order order = orderRepository.findByIdAndUserEmail(request.getOrderId(), userEmail)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));
        if (order.getStatus() == OrderStatus.PAID)
            throw new AppException(ErrorCode.PAYMENT_ALREADY_EXISTS);

        paymentRepository.findTopByOrderIdAndMethodAndStatusInOrderByCreatedAtDesc(
                        order.getId(), METHOD_VNPAY, PaymentStatus.SUCCESS)
                .ifPresent(existing -> {
                    throw new AppException(ErrorCode.PAYMENT_ALREADY_EXISTS);
                });

        Payment payment = paymentRepository.findTopByOrderIdAndMethodAndStatusInOrderByCreatedAtDesc(
                        order.getId(), METHOD_VNPAY, PaymentStatus.PENDING)
                .orElse(null);

        String txnRef = "EM" + order.getId() + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        String orderInfo = "Thanh toan don hang " + order.getId();

        if (payment == null) {
            payment = Payment.builder()
                    .order(order)
                    .method(METHOD_VNPAY)
                    .amount(order.getTotalAmount())
                    .status(PaymentStatus.PENDING)
                    .transactionRef(txnRef)
                    .build();
        } else {
            payment.setTransactionRef(txnRef);
            payment.setAmount(order.getTotalAmount());
        }
        payment = paymentRepository.save(payment);

        String paymentUrl = buildPaymentUrl(order, txnRef, orderInfo, clientIp);
        return VnpayPaymentInitResponse.builder()
                .paymentId(payment.getId())
                .orderId(order.getId())
                .amount(order.getTotalAmount())
                .status(PaymentStatus.PENDING.name())
                .transactionRef(txnRef)
                .paymentUrl(paymentUrl)
                .build();
    }

    @Transactional
    public VnpayIpnResponse handleIpn(Map<String, String> params) {
        if (!vnpayProperties.isConfigured())
            return new VnpayIpnResponse("99", "Gateway not configured");

        Map<String, String> vnpParams = extractVnpParams(params);
        String secureHash = vnpParams.remove("vnp_SecureHash");
        vnpParams.remove("vnp_SecureHashType");

        if (!VnpaySignatureUtil.verify(vnpParams, vnpayProperties.getHashSecret().trim(), secureHash))
            return new VnpayIpnResponse("97", "Invalid signature");

        String txnRef = vnpParams.get("vnp_TxnRef");
        if (txnRef == null || txnRef.isBlank())
            return new VnpayIpnResponse("01", "Order not found");

        Payment payment = paymentRepository.findByTransactionRef(txnRef).orElse(null);
        if (payment == null)
            return new VnpayIpnResponse("01", "Order not found");

        if (payment.getStatus() == PaymentStatus.SUCCESS)
            return new VnpayIpnResponse("02", "Order already confirmed");

        long vnpAmount = parseLong(vnpParams.get("vnp_Amount"));
        if (vnpAmount != (long) payment.getAmount() * 100L)
            return new VnpayIpnResponse("04", "Invalid amount");

        String responseCode = vnpParams.getOrDefault("vnp_ResponseCode", "");
        String transactionStatus = vnpParams.getOrDefault("vnp_TransactionStatus", "");

        if ("00".equals(responseCode) && "00".equals(transactionStatus)) {
            payment.setStatus(PaymentStatus.SUCCESS);
            Order order = payment.getOrder();
            order.setStatus(OrderStatus.PAID);
            orderRepository.save(order);
            paymentRepository.save(payment);
            return new VnpayIpnResponse("00", "Confirm Success");
        }

        payment.setStatus(PaymentStatus.FAILED);
        paymentRepository.save(payment);
        return new VnpayIpnResponse("00", "Confirm Success");
    }

    private String buildPaymentUrl(Order order, String txnRef, String orderInfo, String clientIp) {
        LocalDateTime now = LocalDateTime.now(VN_ZONE);
        Map<String, String> params = new LinkedHashMap<>();
        params.put("vnp_Version", vnpayProperties.getVersion());
        params.put("vnp_Command", vnpayProperties.getCommand());
        params.put("vnp_TmnCode", vnpayProperties.getTmnCode().trim());
        params.put("vnp_Amount", String.valueOf((long) order.getTotalAmount() * 100L));
        params.put("vnp_CurrCode", vnpayProperties.getCurrCode());
        params.put("vnp_TxnRef", txnRef);
        params.put("vnp_OrderInfo", orderInfo);
        params.put("vnp_OrderType", vnpayProperties.getOrderType());
        params.put("vnp_Locale", vnpayProperties.getLocale());
        params.put("vnp_ReturnUrl", vnpayProperties.getReturnUrl());
        params.put("vnp_IpAddr", normalizeIp(clientIp));
        params.put("vnp_CreateDate", now.format(VNPAY_DATE));
        params.put("vnp_ExpireDate", now.plusMinutes(vnpayProperties.getExpireMinutes()).format(VNPAY_DATE));

        String hashSecret = vnpayProperties.getHashSecret().trim();
        String hashData = VnpaySignatureUtil.buildHashData(params);
        log.debug("VNPay hashData for signing: {}", hashData);
        log.debug("VNPay tmnCode={} txnRef={}", vnpayProperties.getTmnCode().trim(), txnRef);

        return VnpaySignatureUtil.buildPaymentUrl(
                vnpayProperties.getPayUrl(),
                params,
                hashSecret);
    }

    private static Map<String, String> extractVnpParams(Map<String, String> params) {
        Map<String, String> vnpParams = new LinkedHashMap<>();
        params.forEach((key, value) -> {
            if (key != null && key.startsWith("vnp_") && value != null)
                vnpParams.put(key, value);
        });
        return vnpParams;
    }

    private static long parseLong(String value) {
        if (value == null || value.isBlank())
            return -1L;
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ex) {
            return -1L;
        }
    }

    private static String normalizeIp(String clientIp) {
        if (clientIp == null || clientIp.isBlank())
            return "127.0.0.1";
        int comma = clientIp.indexOf(',');
        if (comma > 0)
            clientIp = clientIp.substring(0, comma).trim();
        clientIp = clientIp.trim();
        if ("::1".equals(clientIp) || "0:0:0:0:0:0:0:1".equals(clientIp))
            return "127.0.0.1";
        return clientIp;
    }

    /** Kiểm tra config đang load (ADMIN) — giúp debug lỗi sai chữ ký code 70. */
    public com.hoang.jwtjava.dto.response.VnpayDiagnosticsResponse diagnostics() {
        String secret = vnpayProperties.getHashSecret() == null ? "" : vnpayProperties.getHashSecret().trim();
        String tmn = vnpayProperties.getTmnCode() == null ? "" : vnpayProperties.getTmnCode().trim();

        Map<String, String> sample = new LinkedHashMap<>();
        sample.put("vnp_Version", "2.1.0");
        sample.put("vnp_Command", "pay");
        sample.put("vnp_TmnCode", tmn);
        sample.put("vnp_Amount", "10000000");
        sample.put("vnp_CurrCode", "VND");
        sample.put("vnp_TxnRef", "DIAG0001");
        sample.put("vnp_OrderInfo", "Thanh toan don hang test");
        sample.put("vnp_OrderType", vnpayProperties.getOrderType());
        sample.put("vnp_Locale", vnpayProperties.getLocale());
        sample.put("vnp_ReturnUrl", vnpayProperties.getReturnUrl());
        sample.put("vnp_IpAddr", "127.0.0.1");
        sample.put("vnp_CreateDate", LocalDateTime.now(VN_ZONE).format(VNPAY_DATE));
        sample.put("vnp_ExpireDate", LocalDateTime.now(VN_ZONE).plusMinutes(15).format(VNPAY_DATE));

        String hashData = VnpaySignatureUtil.buildHashData(sample);
        String paymentUrl = secret.isBlank()
                ? ""
                : VnpaySignatureUtil.buildPaymentUrl(vnpayProperties.getPayUrl(), sample, secret);

        String preview = secret.length() < 8
                ? "(empty or too short)"
                : secret.substring(0, 4) + "..." + secret.substring(secret.length() - 4);

        return com.hoang.jwtjava.dto.response.VnpayDiagnosticsResponse.builder()
                .enabled(vnpayProperties.isEnabled())
                .configured(vnpayProperties.isConfigured())
                .tmnCode(tmn)
                .hashSecretLength(secret.length())
                .hashSecretPreview(preview)
                .returnUrl(vnpayProperties.getReturnUrl())
                .ipnUrl(vnpayProperties.getIpnUrl())
                .sampleHashData(hashData)
                .samplePaymentUrl(paymentUrl)
                .note("Loi 70 = VNPay tu choi chu ky. Neu hashSecretLength != 32 hoac preview khong khop portal, "
                        + "sua application-local.yaml va xoa bien moi truong VNPAY_HASH_SECRET neu co.")
                .build();
    }
}
