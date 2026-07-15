package com.hoang.jwtjava.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
@Schema(description = "Khởi tạo thanh toán VNPay cho đơn hàng đã tạo")
public class VnpayPaymentInitRequest {
    @NotNull(message = "PAYMENT_INVALID")
    @Positive(message = "PAYMENT_INVALID")
    @Schema(description = "ID đơn hàng (status PENDING_PAYMENT)", example = "1")
    private Long orderId;
}
