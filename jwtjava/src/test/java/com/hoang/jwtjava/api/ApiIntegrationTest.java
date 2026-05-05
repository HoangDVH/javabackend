package com.hoang.jwtjava.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hoang.jwtjava.dto.request.AuthenticationRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Sample HTTP integration tests (MockMvc + MySQL as in production).
 * Run: {@code ./mvnw test} — requires MySQL and config from {@code application.yaml}.
 */
@SpringBootTest(properties = "app.seed-images-from-network=false")
@AutoConfigureMockMvc
class ApiIntegrationTest {

    private static final String ADMIN_EMAIL = "admin@gmail.com";
    private static final String ADMIN_PASSWORD = "Admin@123456";

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    private String obtainAdminAccessToken() throws Exception {
        AuthenticationRequest body = new AuthenticationRequest(ADMIN_EMAIL, ADMIN_PASSWORD);
        String json = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.accessToken").exists())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String token = objectMapper.readTree(json).path("result").path("accessToken").asText();
        assertThat(token).isNotBlank();
        return token;
    }

    @Test
    @DisplayName("GET /api/v1/products without token returns 401")
    void listProducts_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/products")
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/v1/products with Bearer token returns 200 and page shape")
    void listProducts_withToken_returnsPage() throws Exception {
        String token = obtainAdminAccessToken();
        mockMvc.perform(get("/api/v1/products")
                        .param("page", "0")
                        .param("size", "5")
                        .param("sort", "createdAt,desc")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.result.items").isArray())
                .andExpect(jsonPath("$.result.totalElements").value(greaterThanOrEqualTo(0)))
                .andExpect(jsonPath("$.result.page").value(0))
                .andExpect(jsonPath("$.result.size").value(5));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login wrong password returns 401")
    void login_wrongPassword_returns401() throws Exception {
        AuthenticationRequest body = new AuthenticationRequest(ADMIN_EMAIL, "WrongPassword!!!");
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(1001));
    }
}
