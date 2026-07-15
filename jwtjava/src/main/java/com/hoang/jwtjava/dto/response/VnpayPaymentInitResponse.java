package com.hoang.jwtjava.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "Kết quả khởi tạo VNPay — frontend redirect tới paymentUrl")
public class VnpayPaymentInitResponse {
    @Schema(example = "1")
    private Long paymentId;
    @Schema(example = "1")
    private Long orderId;
    @Schema(description = "Số tiền VND", example = "150000")
    private Integer amount;
    @Schema(example = "PENDING")
    private String status;
    @Schema(description = "Mã giao dịch merchant (vnp_TxnRef)", example = "EM11abc123def456")
    private String transactionRef;
    @Schema(description = "URL redirect sang cổng VNPay sandbox")
    private String paymentUrl;
}
