package com.hoang.jwtjava.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OrderItemResponse {
    private Long productId;
    private String productName;
    private Integer unitPrice;
    private Integer quantity;
    private String sellerEmail;
    private String fulfillmentStatus;
}
