package com.hoang.jwtjava.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserAddressRequest {

    @Size(max = 100, message = "ADDRESS_INVALID")
    private String label;

    @NotBlank(message = "ADDRESS_INVALID")
    @Size(max = 255, message = "ADDRESS_INVALID")
    private String receiverName;

    @NotBlank(message = "ADDRESS_INVALID")
    @Size(max = 32, message = "ADDRESS_INVALID")
    private String phone;

    @NotBlank(message = "ADDRESS_INVALID")
    @Size(max = 1000, message = "ADDRESS_INVALID")
    private String address;

    private Boolean isDefault;
}
