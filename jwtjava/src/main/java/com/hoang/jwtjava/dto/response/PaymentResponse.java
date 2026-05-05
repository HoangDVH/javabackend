package com.hoang.jwtjava.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class PaymentResponse {
    private Long id;
    private Long orderId;
    private String method;
    private Integer amount;
    private String status;
    private String transactionRef;
    private LocalDateTime createdAt;
}
