package com.hoang.jwtjava.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class OrderCreateRequest {
    @Valid
    @NotEmpty(message = "ORDER_ITEM_INVALID")
    private List<OrderItemRequest> items;
}
