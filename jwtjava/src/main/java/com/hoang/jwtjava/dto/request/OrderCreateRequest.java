package com.hoang.jwtjava.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class OrderCreateRequest {
    @Valid
    @NotEmpty(message = "ORDER_ITEM_INVALID")
    private List<OrderItemRequest> items;

    @NotBlank(message = "SHIPPING_INFO_INVALID")
    @Size(max = 255, message = "SHIPPING_INFO_INVALID")
    private String receiverName;

    @NotBlank(message = "SHIPPING_INFO_INVALID")
    @Size(max = 32, message = "SHIPPING_INFO_INVALID")
    private String receiverPhone;

    @NotBlank(message = "SHIPPING_INFO_INVALID")
    @Size(max = 1000, message = "SHIPPING_INFO_INVALID")
    private String shippingAddress;
}
