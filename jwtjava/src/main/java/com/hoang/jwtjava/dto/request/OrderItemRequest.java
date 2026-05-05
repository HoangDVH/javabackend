package com.hoang.jwtjava.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class OrderItemRequest {
    @NotNull(message = "ORDER_ITEM_INVALID")
    @Positive(message = "ORDER_ITEM_INVALID")
    private Long productId;

    @NotNull(message = "ORDER_ITEM_INVALID")
    @Positive(message = "ORDER_ITEM_INVALID")
    private Integer quantity;
}
