package com.hoang.jwtjava.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Danh mục sản phẩm")
public class CategoryResponse {
    @Schema(example = "1")
    Long id;
    @Schema(example = "PHONE")
    String code;
    @Schema(example = "Điện thoại")
    String name;
}
