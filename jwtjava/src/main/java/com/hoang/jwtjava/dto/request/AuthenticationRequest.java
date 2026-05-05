package com.hoang.jwtjava.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Login — use seeded admin or any registered user")
public class AuthenticationRequest {

    @NotBlank(message = "EMAIL_INVALID")
    @Email(message = "EMAIL_INVALID")
    @Schema(example = "admin@gmail.com")
    String email;

    @NotBlank(message = "PASSWORD_INVALID")
    @Schema(example = "Admin@123456")
    String password;
}
