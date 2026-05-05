package com.hoang.jwtjava.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Optional refresh token to revoke together with current access token")
public class LogoutRequest {
    @Schema(example = "<refresh_token_from_login>")
    String refreshToken;
}
