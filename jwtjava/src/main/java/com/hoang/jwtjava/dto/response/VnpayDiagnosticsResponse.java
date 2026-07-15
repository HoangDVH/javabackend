package com.hoang.jwtjava.dto.response;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class VnpayDiagnosticsResponse {

    boolean enabled;
    boolean configured;
    String tmnCode;
    int hashSecretLength;
    String hashSecretPreview;
    String returnUrl;
    String ipnUrl;
    String sampleHashData;
    String samplePaymentUrl;
    String note;
}
