package com.hoang.jwtjava.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class OrderRealtimeEvent {
    private String type;
    private OrderResponse order;
    private LocalDateTime occurredAt;
}
