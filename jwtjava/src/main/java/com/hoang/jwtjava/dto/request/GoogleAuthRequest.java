package com.hoang.jwtjava.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Google Sign-In — gửi ID token từ Google Identity Services (FE)")
public class GoogleAuthRequest {

    @NotBlank(message = "GOOGLE_TOKEN_INVALID")
    @Schema(description = "credential / id_token từ Google button hoặc One Tap", example = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String idToken;
}
