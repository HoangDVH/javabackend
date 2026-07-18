package com.hoang.jwtjava.dto.request;

import com.hoang.jwtjava.entity.FulfillmentStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SellerOrderStatusUpdateRequest {

    @NotNull
    private FulfillmentStatus status;
}
