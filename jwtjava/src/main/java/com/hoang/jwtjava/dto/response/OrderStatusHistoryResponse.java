package com.hoang.jwtjava.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class OrderStatusHistoryResponse {
    private Long id;
    private String oldStatus;
    private String newStatus;
    private LocalDateTime changedAt;
    private String changedBy;
}
