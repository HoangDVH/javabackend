package com.hoang.jwtjava.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Embeddable
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OrderItem {
    @Column(name = "product_id", nullable = false)
    Long productId;

    @Column(name = "product_name", nullable = false, length = 255)
    String productName;

    @Column(name = "unit_price", nullable = false)
    Integer unitPrice;

    @Column(name = "quantity", nullable = false)
    Integer quantity;

    @Column(name = "seller_email", length = 255)
    String sellerEmail;
}
