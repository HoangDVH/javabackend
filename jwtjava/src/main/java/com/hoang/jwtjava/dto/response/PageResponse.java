package com.hoang.jwtjava.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Trang kết quả phân trang")
public class PageResponse<T> {
    @Schema(description = "Danh sách phần tử trang hiện tại")
    List<T> items;
    @Schema(description = "Tổng số phần tử khớp filter", example = "100")
    long totalElements;
    @Schema(description = "Tổng số trang", example = "5")
    int totalPages;
    @Schema(description = "Trang hiện tại (0-based)", example = "0")
    int page;
    @Schema(description = "Kích thước trang", example = "20")
    int size;
}
