package com.hoang.jwtjava.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Envelope chuẩn cho mọi response API")
public class ApiResponse<T> {

    @Builder.Default
    @Schema(description = "1000 = success; mã lỗi khác xem ErrorCode", example = "1000")
    int code = 1000;

    @Schema(description = "Thông báo tùy chọn (vd. sau create/update)", example = "Login successful")
    String message;
    @Schema(description = "Payload dữ liệu")
    T result;
}
