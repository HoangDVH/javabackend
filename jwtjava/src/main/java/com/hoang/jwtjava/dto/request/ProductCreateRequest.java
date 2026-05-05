package com.hoang.jwtjava.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Create/update product. Images: https URL (downloaded) or /files/... path after upload")
public class ProductCreateRequest {

    @NotBlank(message = "INVALID_KEY")
    @Size(max = 255, message = "INVALID_KEY")
    @Schema(example = "Sample T-shirt (Swagger)")
    String name;

    @Size(max = 10_000, message = "INVALID_KEY")
    @Schema(example = "Short description for Swagger demo")
    String description;

    @NotNull(message = "INVALID_KEY")
    @PositiveOrZero(message = "INVALID_KEY")
    @Schema(example = "199000")
    Integer price;

    @NotNull(message = "INVALID_KEY")
    @PositiveOrZero(message = "INVALID_KEY")
    @Schema(example = "159000")
    Integer discountPrice;

    @NotNull(message = "INVALID_KEY")
    @PositiveOrZero(message = "INVALID_KEY")
    @Schema(example = "50")
    Integer stock;

    @NotNull(message = "INVALID_KEY")
    @Positive(message = "INVALID_KEY")
    @Schema(example = "1", description = "Category id (seed data often uses 1–6)")
    Long categoryId;

    @NotNull(message = "INVALID_KEY")
    @Positive(message = "INVALID_KEY")
    @Schema(example = "1")
    Long brandId;

    @Valid
    @NotNull(message = "INVALID_KEY")
    @Schema(example = "[\"https://picsum.photos/seed/swagger-demo/400/400.jpg\"]")
    List<@NotBlank @Size(max = 512) String> images = new ArrayList<>();

    @NotNull(message = "INVALID_KEY")
    @DecimalMin(value = "0.0", inclusive = true, message = "INVALID_KEY")
    @DecimalMax(value = "5.0", inclusive = true, message = "INVALID_KEY")
    @Schema(example = "4.5")
    BigDecimal rating;

    @NotNull(message = "INVALID_KEY")
    @Schema(example = "false")
    Boolean isFeatured;
}
