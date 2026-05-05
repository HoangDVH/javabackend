package com.hoang.jwtjava.controller;

import com.hoang.jwtjava.dto.request.PaymentCreateRequest;
import com.hoang.jwtjava.dto.response.ApiResponse;
import com.hoang.jwtjava.dto.response.PaymentResponse;
import com.hoang.jwtjava.service.PaymentService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Tag(name = "Payments")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    @PreAuthorize("hasAnyRole('USER','SELLER','ADMIN')")
    public ResponseEntity<ApiResponse<PaymentResponse>> createPayment(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody @Valid PaymentCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.<PaymentResponse>builder()
                .message("Payment created successfully")
                .result(paymentService.createPayment(jwt.getSubject(), request))
                .build());
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('USER','SELLER','ADMIN')")
    public ResponseEntity<ApiResponse<List<PaymentResponse>>> getMyPayments(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(ApiResponse.<List<PaymentResponse>>builder()
                .result(paymentService.getMyPayments(jwt.getSubject()))
                .build());
    }

    @GetMapping("/seller/history")
    @PreAuthorize("hasAnyRole('SELLER','ADMIN')")
    public ResponseEntity<ApiResponse<List<PaymentResponse>>> getSellerPayments(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) String sellerEmail) {
        String targetSeller = hasRole(jwt, "ADMIN") && sellerEmail != null && !sellerEmail.isBlank()
                ? sellerEmail
                : jwt.getSubject();
        return ResponseEntity.ok(ApiResponse.<List<PaymentResponse>>builder()
                .result(paymentService.getSellerPayments(targetSeller))
                .build());
    }

    private boolean hasRole(Jwt jwt, String role) {
        String scope = jwt.getClaimAsString("scope");
        if (scope == null || scope.isBlank())
            return false;
        for (String token : scope.split("\\s+")) {
            if (role.equalsIgnoreCase(token))
                return true;
        }
        return false;
    }
}
