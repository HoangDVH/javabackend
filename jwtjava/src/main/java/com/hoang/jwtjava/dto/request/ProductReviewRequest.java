package com.hoang.jwtjava.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Tạo/cập nhật đánh giá sản phẩm")
public class ProductReviewRequest {

    @NotNull(message = "REVIEW_INVALID")
    @Min(value = 1, message = "REVIEW_INVALID")
    @Max(value = 5, message = "REVIEW_INVALID")
    @Schema(example = "5", description = "Điểm 1–5")
    private Integer rating;

    @Size(max = 2000, message = "REVIEW_INVALID")
    @Schema(example = "Sản phẩm tốt, giao hàng nhanh.")
    private String comment;
}
