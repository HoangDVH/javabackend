package com.hoang.jwtjava.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hoang.jwtjava.dto.request.AuthenticationRequest;
import com.hoang.jwtjava.dto.request.OrderCreateRequest;
import com.hoang.jwtjava.dto.request.OrderItemRequest;
import com.hoang.jwtjava.dto.request.VnpayPaymentInitRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class VnpayFlowIntegrationTest {

    private static final String ADMIN_EMAIL = "admin@gmail.com";
    private static final String ADMIN_PASSWORD = "Admin@123456";
    private static final String HASH_SECRET = "8YJPTW1TD40WLS1I5HTVWUV38V3HZX25";

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    @DisplayName("VNPay flow: create order → payment URL → simulated IPN → order PAID")
    void vnpayPaymentFlow_simulatedIpn_marksOrderPaid() throws Exception {
        String token = login();

        Long productId = fetchFirstProductId();
        Long orderId = createOrder(token, productId);

        String txnRef = initiateVnpayPayment(token, orderId);
        int orderAmount = fetchOrderAmount(token, orderId);

        mockMvc.perform(get("/api/v1/payments/vnpay/ipn")
                        .params(buildSignedIpnParams(txnRef, orderAmount)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.RspCode").value("00"));

        mockMvc.perform(get("/api/v1/orders/" + orderId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.status").value("PAID"));
    }

    private String login() throws Exception {
        AuthenticationRequest body = new AuthenticationRequest(ADMIN_EMAIL, ADMIN_PASSWORD);
        String json = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(json).path("result").path("accessToken").asText();
    }

    private Long fetchFirstProductId() throws Exception {
        String json = mockMvc.perform(get("/api/v1/products").param("page", "0").param("size", "1"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode items = objectMapper.readTree(json).path("result").path("items");
        assertThat(items.isArray()).isTrue();
        assertThat(items.size()).isGreaterThan(0);
        return items.get(0).path("id").asLong();
    }

    private Long createOrder(String token, Long productId) throws Exception {
        OrderCreateRequest request = new OrderCreateRequest();
        OrderItemRequest item = new OrderItemRequest();
        item.setProductId(productId);
        item.setQuantity(1);
        request.setItems(List.of(item));

        String json = mockMvc.perform(post("/api/v1/orders")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.result.status").value("PENDING_PAYMENT"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(json).path("result").path("id").asLong();
    }

    private String initiateVnpayPayment(String token, Long orderId) throws Exception {
        VnpayPaymentInitRequest request = new VnpayPaymentInitRequest();
        request.setOrderId(orderId);

        String json = mockMvc.perform(post("/api/v1/payments/vnpay")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.result.paymentUrl").exists())
                .andExpect(jsonPath("$.result.status").value("PENDING"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String paymentUrl = objectMapper.readTree(json).path("result").path("paymentUrl").asText();
        String txnRef = objectMapper.readTree(json).path("result").path("transactionRef").asText();
        assertThat(paymentUrl).contains("sandbox.vnpayment.vn");
        assertThat(paymentUrl).contains("vnp_SecureHash=");
        return txnRef;
    }

    private int fetchOrderAmount(String token, Long orderId) throws Exception {
        String json = mockMvc.perform(get("/api/v1/orders/" + orderId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(json).path("result").path("totalAmount").asInt();
    }

    private org.springframework.util.MultiValueMap<String, String> buildSignedIpnParams(String txnRef, int amountVnd) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("vnp_Amount", String.valueOf((long) amountVnd * 100L));
        params.put("vnp_BankCode", "NCB");
        params.put("vnp_OrderInfo", "Thanh toan don hang test");
        params.put("vnp_PayDate", "20260714120000");
        params.put("vnp_ResponseCode", "00");
        params.put("vnp_TmnCode", "TIBBMYJU");
        params.put("vnp_TransactionNo", "14323434");
        params.put("vnp_TransactionStatus", "00");
        params.put("vnp_TxnRef", txnRef);

        String secureHash = VnpaySignatureUtil.sign(params, HASH_SECRET);
        org.springframework.util.LinkedMultiValueMap<String, String> query =
                new org.springframework.util.LinkedMultiValueMap<>();
        params.forEach(query::add);
        query.add("vnp_SecureHash", secureHash);
        return query;
    }
}
