package com.hoang.jwtjava.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hoang.jwtjava.config.GeminiProperties;
import com.hoang.jwtjava.dto.request.ChatAdviseRequest;
import com.hoang.jwtjava.dto.response.ProductResponse;
import com.hoang.jwtjava.exception.AppException;
import com.hoang.jwtjava.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductChatServiceTest {

    private GeminiProperties geminiProperties;
    private RecordingHistory history;
    private RecordingGemini gemini;
    private FakeProductService products;
    private ProductChatService productChatService;

    @BeforeEach
    void setUp() {
        geminiProperties = new GeminiProperties();
        geminiProperties.setEnabled(true);
        geminiProperties.setMaxCatalogProducts(12);
        geminiProperties.setDescriptionMaxChars(160);
        history = new RecordingHistory();
        gemini = new RecordingGemini();
        products = new FakeProductService();
        productChatService = new ProductChatService(
                products, gemini, history, geminiProperties, new ObjectMapper());
    }

    @Test
    void emptyCatalogReturnsSoftMessageWithoutCallingGemini() {
        ChatAdviseRequest request = new ChatAdviseRequest();
        request.setMessage("Tôi muốn mua laptop");
        request.setSessionId("session-1");

        var response = productChatService.advise(request);

        assertTrue(response.getReply().contains("chưa tìm thấy"));
        assertTrue(response.getProducts().isEmpty());
        assertEquals(0, gemini.calls.get());
        assertEquals(1, history.appends.size());
    }

    @Test
    void mapsRecommendedIdsOnlyFromCatalog() {
        products.results = List.of(product(1L, "Áo khoác"), product(2L, "Quần jean"));
        gemini.response = """
                {"reply":"Gợi ý áo khoác ấm.","recommendedProductIds":[1,999,2]}
                """;

        ChatAdviseRequest request = new ChatAdviseRequest();
        request.setMessage("áo khoác");
        request.setSessionId("session-2");

        var response = productChatService.advise(request);

        assertEquals("Gợi ý áo khoác ấm.", response.getReply());
        assertEquals(2, response.getProducts().size());
        assertEquals(1L, response.getProducts().get(0).getId());
        assertEquals(2L, response.getProducts().get(1).getId());
        assertEquals("Gợi ý áo khoác ấm.", history.appends.get(0)[2]);
    }

    @Test
    void invalidGeminiJsonFallsBackToCatalog() {
        products.results = List.of(product(1L, "Áo"));
        gemini.response = "not-json";

        ChatAdviseRequest request = new ChatAdviseRequest();
        request.setMessage("áo");
        request.setSessionId("session-3");

        var response = productChatService.advise(request);
        assertTrue(response.getReply().contains("tạm thời lỗi")
                || response.getReply().contains("catalog"));
        assertEquals(1, response.getProducts().size());
        assertEquals(1L, response.getProducts().get(0).getId());
    }

    @Test
    void geminiUnavailableFallsBackToCatalog() {
        products.results = List.of(product(9L, "Laptop"));
        gemini.error = new AppException(ErrorCode.CHAT_QUOTA_EXCEEDED);

        ChatAdviseRequest request = new ChatAdviseRequest();
        request.setMessage("laptop");
        request.setSessionId("session-4");

        var response = productChatService.advise(request);
        assertEquals(1, response.getProducts().size());
        assertEquals(9L, response.getProducts().get(0).getId());
        assertTrue(response.getReply().contains("quota") || response.getReply().contains("Laptop"));
    }

    @Test
    void extractTokensSkipsStopWords() {
        List<String> tokens = ProductChatService.extractTokens("Tôi muốn mua áo khoác dưới 500000");
        assertTrue(tokens.contains("áo") || tokens.contains("khoác"));
        assertFalse(tokens.contains("tôi"));
        assertFalse(tokens.contains("mua"));
    }

    @Test
    void systemPromptContainsCatalogIds() {
        String prompt = productChatService.buildSystemPrompt(List.of(product(7L, "Giày chạy")));
        assertTrue(prompt.contains("\"id\":7"));
        assertTrue(prompt.contains("Giày chạy"));
        assertTrue(prompt.contains("recommendedProductIds"));
    }

    @Test
    void retrieveCatalogUsesBudgetFilter() {
        products.results = List.of(product(5L, "Áo thun"));
        List<ProductResponse> catalog = productChatService.retrieveCatalog("áo thun", null, 300000);
        assertEquals(1, catalog.size());
        assertEquals(5L, catalog.get(0).getId());
        assertEquals(300000, products.lastMaxPrice);
    }

    private static ProductResponse product(Long id, String name) {
        return ProductResponse.builder()
                .id(id)
                .name(name)
                .description("Mô tả " + name)
                .price(200000)
                .discountPrice(180000)
                .stock(10)
                .categoryName("Fashion")
                .rating(BigDecimal.valueOf(4.5))
                .images(List.of("https://example.com/" + id + ".jpg"))
                .isFeatured(true)
                .build();
    }

    private static final class RecordingHistory extends ChatHistoryService {
        private final List<String[]> appends = new ArrayList<>();

        private RecordingHistory() {
            super(new GeminiProperties(), new com.hoang.jwtjava.config.AppRedisProperties(), new ObjectMapper(), null);
        }

        @Override
        public List<ChatTurn> getRecentTurns(String sessionId) {
            return List.of();
        }

        @Override
        public void appendTurn(String sessionId, String userMessage, String assistantReply) {
            appends.add(new String[]{sessionId, userMessage, assistantReply});
        }
    }

    private static final class RecordingGemini extends GeminiClient {
        private final AtomicInteger calls = new AtomicInteger();
        private String response = "{}";
        private AppException error;

        private RecordingGemini() {
            super(new GeminiProperties(), new ObjectMapper());
        }

        @Override
        public String generateText(String systemInstruction, List<ChatHistoryService.ChatTurn> history, String userMessage) {
            calls.incrementAndGet();
            if (error != null)
                throw error;
            return response;
        }
    }

    private static final class FakeProductService extends ProductService {
        private List<ProductResponse> results = List.of();
        private Integer lastMaxPrice;

        private FakeProductService() {
            super(null, null, null, null, null);
        }

        @Override
        public Page<ProductResponse> listProducts(
                Long categoryId,
                Long brandId,
                Boolean isFeatured,
                String keyword,
                Integer minPrice,
                Integer maxPrice,
                BigDecimal minRating,
                Boolean hasDiscount,
                Boolean inStock,
                Pageable pageable) {
            lastMaxPrice = maxPrice;
            return new PageImpl<>(results, pageable, results.size());
        }
    }
}
