package com.hoang.jwtjava.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProductResponse {
    Long id;
    String name;
    String description;
    Integer price;
    Integer discountPrice;
    Integer stock;
    Long categoryId;
    String categoryName;
    Long brandId;
    String sellerEmail;
    List<String> images;
    BigDecimal rating;
    boolean isFeatured;

    @JsonFormat(pattern = "yyyy-MM-dd")
    LocalDate createdAt;
}
