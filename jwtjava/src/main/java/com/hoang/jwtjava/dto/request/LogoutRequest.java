package com.hoang.jwtjava.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Optional fallback refresh token for non-cookie clients. Browser clients should rely on HttpOnly cookie.")
public class LogoutRequest {
    @Schema(example = "<refresh_token>", description = "Optional fallback token; usually omitted when using cookie-based auth")
    String refreshToken;
}
