package com.hoang.jwtjava.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class ChatProductResponse {
    private Long id;
    private String name;
    private Integer price;
    private Integer discountPrice;
    private Integer stock;
    private String categoryName;
    private BigDecimal rating;
    private String imageUrl;
}
