package com.hoang.jwtjava.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RoleAssignmentRequest {
    @NotBlank(message = "ROLE_INVALID")
    @Schema(example = "SELLER", allowableValues = {"USER", "SELLER", "ADMIN"})
    private String role;

    @Schema(example = "true")
    private boolean enabled = true;
}
