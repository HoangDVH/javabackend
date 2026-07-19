package com.hoang.jwtjava.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChatAdviseRequest {

    @NotBlank(message = "INVALID_KEY")
    @Size(max = 800, message = "INVALID_KEY")
    @Schema(example = "Tôi muốn mua áo khoác dưới 500 nghìn")
    private String message;

    @Size(max = 64, message = "INVALID_KEY")
    @Schema(description = "UUID do frontend tạo để nhớ hội thoại ngắn hạn", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
    private String sessionId;

    @Positive
    @Schema(description = "Lọc theo danh mục (optional)")
    private Long categoryId;

    @Positive
    @Schema(description = "Ngân sách tối đa VND (optional)", example = "500000")
    private Integer maxBudget;
}
