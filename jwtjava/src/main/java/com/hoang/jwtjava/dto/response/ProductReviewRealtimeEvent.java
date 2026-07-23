package com.hoang.jwtjava.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class ProductReviewRealtimeEvent {
    /** REVIEW_CREATED | REVIEW_UPDATED | REVIEW_DELETED */
    private String type;
    private ProductReviewResponse review;
    private Long productId;
    private BigDecimal productRating;
    private Integer reviewCount;
    private LocalDateTime occurredAt;
}
