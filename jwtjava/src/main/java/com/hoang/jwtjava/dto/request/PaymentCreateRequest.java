package com.hoang.jwtjava.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class PaymentCreateRequest {
    @NotNull(message = "PAYMENT_INVALID")
    @Positive(message = "PAYMENT_INVALID")
    private Long orderId;

    @NotBlank(message = "PAYMENT_INVALID")
    private String method;
}
