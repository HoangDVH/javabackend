package com.hoang.jwtjava.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Authentication result. Refresh token is stored in HttpOnly cookie.")
public class AuthenticationResponse {
    @Schema(description = "JWT access token used in Authorization: Bearer <token>")
    String accessToken;
    @Schema(description = "Whether authentication/refresh succeeded", example = "true")
    boolean authenticated;
}
