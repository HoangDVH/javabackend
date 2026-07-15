package com.hoang.jwtjava.controller;

import com.hoang.jwtjava.dto.request.VnpayPaymentInitRequest;
import com.hoang.jwtjava.dto.response.ApiResponse;
import com.hoang.jwtjava.dto.response.VnpayDiagnosticsResponse;
import com.hoang.jwtjava.dto.response.VnpayIpnResponse;
import com.hoang.jwtjava.dto.response.VnpayPaymentInitResponse;
import com.hoang.jwtjava.service.VnpayPaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/payments/vnpay")
@RequiredArgsConstructor
@Tag(name = "Payments")
public class VnpayPaymentController {

    private final VnpayPaymentService vnpayPaymentService;

    @PostMapping
    @PreAuthorize("hasAnyRole('USER','SELLER','ADMIN')")
    @Operation(
            summary = "Initiate VNPay payment",
            description = """
                    Tạo link thanh toán VNPay sandbox cho đơn `PENDING_PAYMENT`.
                    
                    **Frontend:** redirect user tới `result.paymentUrl` (không mở iframe).
                    Sau thanh toán VNPay redirect về `return-url` (Vercel `/payment/result`).
                    Đơn chuyển `PAID` khi VNPay gọi IPN tới backend.
                    
                    Luồng: POST `/api/v1/orders` → POST endpoint này → redirect `paymentUrl`.
                    """)
    public ResponseEntity<ApiResponse<VnpayPaymentInitResponse>> initiatePayment(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody @Valid VnpayPaymentInitRequest request,
            HttpServletRequest httpRequest) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.<VnpayPaymentInitResponse>builder()
                .message("VNPay payment initiated")
                .result(vnpayPaymentService.initiatePayment(
                        jwt.getSubject(), request, resolveClientIp(httpRequest)))
                .build());
    }

    @GetMapping("/ipn")
    @SecurityRequirements
    @Operation(
            summary = "VNPay IPN callback",
            description = """
                    **Public — VNPay server gọi, không dùng JWT.**
                    Cấu hình URL này trên VNPay merchant portal.
                    Xác minh chữ ký HMAC-SHA512 và cập nhật đơn sang `PAID` khi thành công.
                    """)
    public ResponseEntity<VnpayIpnResponse> handleIpn(@RequestParam Map<String, String> params) {
        return ResponseEntity.ok(vnpayPaymentService.handleIpn(new LinkedHashMap<>(params)));
    }

    @GetMapping("/diagnostics")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "VNPay config diagnostics (ADMIN)",
            description = "Kiểm tra tmnCode, hashSecret length, sample payment URL — debug lỗi sai chữ ký (code 70).")
    public ResponseEntity<ApiResponse<VnpayDiagnosticsResponse>> diagnostics() {
        return ResponseEntity.ok(ApiResponse.<VnpayDiagnosticsResponse>builder()
                .message("VNPay diagnostics")
                .result(vnpayPaymentService.diagnostics())
                .build());
    }

    private static String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank())
            return forwarded;
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank())
            return realIp;
        return request.getRemoteAddr();
    }
}
