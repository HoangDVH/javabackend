package com.hoang.jwtjava.controller;

import com.hoang.jwtjava.dto.request.OrderCreateRequest;
import com.hoang.jwtjava.dto.request.SellerOrderStatusUpdateRequest;
import com.hoang.jwtjava.dto.response.ApiResponse;
import com.hoang.jwtjava.dto.response.OrderResponse;
import com.hoang.jwtjava.dto.response.OrderStatusHistoryResponse;
import com.hoang.jwtjava.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Tag(name = "Orders")
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @PreAuthorize("hasAnyRole('USER','SELLER','ADMIN')")
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody @Valid OrderCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.<OrderResponse>builder()
                .message("Order created successfully")
                .result(orderService.createOrder(jwt.getSubject(), request))
                .build());
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('USER','SELLER','ADMIN')")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getMyOrders(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(ApiResponse.<List<OrderResponse>>builder()
                .result(orderService.getMyOrders(jwt.getSubject()))
                .build());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER','SELLER','ADMIN')")
    public ResponseEntity<ApiResponse<OrderResponse>> getMyOrder(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.<OrderResponse>builder()
                .result(orderService.getMyOrder(jwt.getSubject(), id))
                .build());
    }

    @GetMapping("/{id}/status-history")
    @PreAuthorize("hasAnyRole('USER','SELLER','ADMIN')")
    @Operation(summary = "Order status history", description = "Lịch sử thay đổi trạng thái đơn hàng.")
    public ResponseEntity<ApiResponse<List<OrderStatusHistoryResponse>>> getOrderStatusHistory(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.<List<OrderStatusHistoryResponse>>builder()
                .result(orderService.getOrderStatusHistory(jwt.getSubject(), id))
                .build());
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('USER','SELLER','ADMIN')")
    @Operation(
            summary = "Cancel order",
            description = """
                    Hủy đơn **chưa thanh toán** (`PENDING_PAYMENT`).
                    Hoàn tồn kho sản phẩm và đánh dấu payment VNPay đang chờ là `FAILED`.
                    Không hủy được đơn đã `PAID` hoặc đã `CANCELLED`.
                    """)
    public ResponseEntity<ApiResponse<OrderResponse>> cancelOrder(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.<OrderResponse>builder()
                .message("Order cancelled successfully")
                .result(orderService.cancelOrder(jwt.getSubject(), id))
                .build());
    }

    @GetMapping("/seller/history")
    @PreAuthorize("hasAnyRole('SELLER','ADMIN')")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getSellerOrderHistory(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) String sellerEmail) {
        String targetSeller = hasRole(jwt, "ADMIN") && sellerEmail != null && !sellerEmail.isBlank()
                ? sellerEmail
                : jwt.getSubject();
        return ResponseEntity.ok(ApiResponse.<List<OrderResponse>>builder()
                .result(orderService.getSellerOrders(targetSeller))
                .build());
    }

    @PatchMapping("/{id}/seller-status")
    @PreAuthorize("hasAnyRole('SELLER','ADMIN')")
    @Operation(
            summary = "Update seller fulfillment status",
            description = """
                    Cập nhật trạng thái giao hàng cho các sản phẩm của seller trong đơn đã thanh toán.
                    Luồng hợp lệ: `AWAITING_CONFIRMATION` → `CONFIRMED` → `PROCESSING`
                    → `SHIPPED` → `DELIVERED`. ADMIN có thể truyền `sellerEmail`.
                    """)
    public ResponseEntity<ApiResponse<OrderResponse>> updateSellerFulfillmentStatus(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id,
            @RequestParam(required = false) String sellerEmail,
            @RequestBody @Valid SellerOrderStatusUpdateRequest request) {
        String targetSeller = hasRole(jwt, "ADMIN") && sellerEmail != null && !sellerEmail.isBlank()
                ? sellerEmail
                : jwt.getSubject();
        return ResponseEntity.ok(ApiResponse.<OrderResponse>builder()
                .message("Fulfillment status updated successfully")
                .result(orderService.updateSellerFulfillmentStatus(targetSeller, id, request))
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
