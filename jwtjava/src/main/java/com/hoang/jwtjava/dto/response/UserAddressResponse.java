package com.hoang.jwtjava.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserAddressResponse {
    private Long id;
    private String label;
    private String receiverName;
    private String phone;
    private String address;
    @JsonProperty("isDefault")
    private boolean isDefault;
}
