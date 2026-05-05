package com.hoang.jwtjava.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Paste refresh_token returned from login")
public class RefreshTokenRequest {
    @NotBlank(message = "INVALID_KEY")
    @Schema(example = "<refresh_token_from_login>")
    String refreshToken;
}
