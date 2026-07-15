package com.hoang.jwtjava.service;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class VnpaySignatureUtilTest {

    @Test
    void buildHashData_usesPlusForSpacesPerVnpayDemo() {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("vnp_OrderInfo", "Thanh toan don hang 1");
        params.put("vnp_TxnRef", "EM1abc");

        String hashData = VnpaySignatureUtil.buildHashData(params);

        assertThat(hashData).isEqualTo("vnp_OrderInfo=Thanh+toan+don+hang+1&vnp_TxnRef=EM1abc");
    }

    @Test
    void buildHashData_skipsEmptyValuesLikeOfficialIterator() {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("vnp_Amount", "1000000");
        params.put("vnp_BankCode", "");
        params.put("vnp_TxnRef", "123");

        String hashData = VnpaySignatureUtil.buildHashData(params);

        assertThat(hashData).isEqualTo("vnp_Amount=1000000&vnp_TxnRef=123");
        assertThat(hashData).doesNotContain("vnp_BankCode");
    }

    @Test
    void buildPaymentUrl_matchesOfficialDemoFormat() {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("vnp_Version", "2.1.0");
        params.put("vnp_Command", "pay");
        params.put("vnp_TmnCode", "TIBBMYJU");
        params.put("vnp_Amount", "1000000");
        params.put("vnp_CurrCode", "VND");
        params.put("vnp_TxnRef", "12345678");
        params.put("vnp_OrderInfo", "test");
        params.put("vnp_OrderType", "other");
        params.put("vnp_Locale", "vn");
        params.put("vnp_ReturnUrl", "http://localhost:5173/payment/result");
        params.put("vnp_IpAddr", "127.0.0.1");
        params.put("vnp_CreateDate", "20260101120000");
        params.put("vnp_ExpireDate", "20260101121500");

        String url = VnpaySignatureUtil.buildPaymentUrl(
                "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html",
                params,
                "8YJPTW1TD40WLS1I5HTVWUV38V3HZX25");

        assertThat(url).startsWith("https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?");
        assertThat(url).contains("vnp_SecureHash=");
        assertThat(url).contains("vnp_TmnCode=TIBBMYJU");
    }
}
