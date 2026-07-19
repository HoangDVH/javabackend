package com.hoang.jwtjava.api;

import com.hoang.jwtjava.controller.ChatController;
import com.hoang.jwtjava.dto.request.ChatAdviseRequest;
import com.hoang.jwtjava.dto.response.ChatAdviseResponse;
import com.hoang.jwtjava.dto.response.ChatProductResponse;
import com.hoang.jwtjava.exception.GlobalExceptionHandler;
import com.hoang.jwtjava.service.ProductChatService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ChatAdviseApiTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ProductChatService stub = new ProductChatService(null, null, null, null, null) {
            @Override
            public ChatAdviseResponse advise(ChatAdviseRequest request) {
                return ChatAdviseResponse.builder()
                        .reply("Gợi ý áo khoác này.")
                        .sessionId(request.getSessionId())
                        .products(List.of(ChatProductResponse.builder()
                                .id(1L)
                                .name("Áo khoác")
                                .price(450000)
                                .discountPrice(399000)
                                .stock(5)
                                .categoryName("Fashion")
                                .build()))
                        .disclaimer("Gợi ý dựa trên catalog Easy Mart hiện tại.")
                        .build();
            }
        };

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(new ChatController(stub))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();
    }

    @Test
    void adviseEndpointReturnsProducts() throws Exception {
        mockMvc.perform(post("/api/v1/chat/advise")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message":"áo khoác","sessionId":"abc"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.result.reply").value("Gợi ý áo khoác này."))
                .andExpect(jsonPath("$.result.products[0].id").value(1))
                .andExpect(jsonPath("$.result.products[0].price").value(450000));
    }

    @Test
    void adviseRejectsBlankMessage() throws Exception {
        mockMvc.perform(post("/api/v1/chat/advise")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message":"   "}
                                """))
                .andExpect(status().isBadRequest());
    }
}
