package com.hoang.jwtjava.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class OrderResponse {
    private Long id;
    private String userEmail;
    private List<OrderItemResponse> items;
    private Integer totalAmount;
    private String status;
    private LocalDateTime createdAt;
}
