package com.hoang.jwtjava.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProductCreateRequest {

    @NotBlank(message = "INVALID_KEY")
    @Size(max = 255, message = "INVALID_KEY")
    String name;

    @Size(max = 10_000, message = "INVALID_KEY")
    String description;

    @NotNull(message = "INVALID_KEY")
    @PositiveOrZero(message = "INVALID_KEY")
    Integer price;

    @NotNull(message = "INVALID_KEY")
    @PositiveOrZero(message = "INVALID_KEY")
    Integer discountPrice;

    @NotNull(message = "INVALID_KEY")
    @PositiveOrZero(message = "INVALID_KEY")
    Integer stock;

    @NotNull(message = "INVALID_KEY")
    @Positive(message = "INVALID_KEY")
    Long categoryId;

    @NotNull(message = "INVALID_KEY")
    @Positive(message = "INVALID_KEY")
    Long brandId;

    @Valid
    @NotNull(message = "INVALID_KEY")
    List<@NotBlank @Size(max = 512) String> images = new ArrayList<>();

    @NotNull(message = "INVALID_KEY")
    @DecimalMin(value = "0.0", inclusive = true, message = "INVALID_KEY")
    @DecimalMax(value = "5.0", inclusive = true, message = "INVALID_KEY")
    BigDecimal rating;

    @NotNull(message = "INVALID_KEY")
    Boolean isFeatured;
}
