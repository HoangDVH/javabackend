package com.hoang.jwtjava.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "products", indexes = {
        @Index(name = "idx_products_category_created", columnList = "category_id, created_at"),
        @Index(name = "idx_products_brand_id", columnList = "brand_id"),
        @Index(name = "idx_products_seller_created", columnList = "seller_email, created_at")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(nullable = false, length = 255)
    String name;

    @Column(columnDefinition = "TEXT")
    String description;

    @Column(nullable = false)
    Integer price;

    @Column(nullable = false, name = "discount_price")
    Integer discountPrice;

    @Column(nullable = false)
    Integer stock;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "category_id", nullable = false)
    Category category;

    @Column(nullable = false, name = "brand_id")
    Long brandId;

    @Column(name = "seller_email", length = 255)
    String sellerEmail;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "product_images", joinColumns = @JoinColumn(name = "product_id"))
    @Column(name = "image_path", length = 512)
    @OrderColumn(name = "sort_order")
    List<String> images;

    @Column(nullable = false, precision = 3, scale = 2)
    BigDecimal rating;

    @Column(nullable = false, name = "review_count")
    @Builder.Default
    Integer reviewCount = 0;

    @Column(nullable = false, name = "is_featured")
    boolean featured;

    @Column(nullable = false, name = "created_at")
    LocalDate createdAt;

    @Column(name = "updated_at")
    LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null)
            createdAt = LocalDate.now();
        if (images == null)
            images = new ArrayList<>();
        if (rating == null)
            rating = BigDecimal.ZERO;
        if (reviewCount == null)
            reviewCount = 0;
    }
}
