package com.hoang.jwtjava.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
@Schema(description = "Thanh toán mock (COD/CASH). Không dùng method=VNPAY.")
public class PaymentCreateRequest {
    @NotNull(message = "PAYMENT_INVALID")
    @Positive(message = "PAYMENT_INVALID")
    @Schema(example = "1")
    private Long orderId;

    @NotBlank(message = "PAYMENT_INVALID")
    @Schema(description = "Phương thức mock: CASH, COD. VNPay dùng POST /payments/vnpay.", example = "CASH")
    private String method;
}
