package com.hoang.jwtjava.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hoang.jwtjava.config.GeminiProperties;
import com.hoang.jwtjava.dto.request.ChatAdviseRequest;
import com.hoang.jwtjava.dto.response.ChatAdviseResponse;
import com.hoang.jwtjava.dto.response.ChatProductResponse;
import com.hoang.jwtjava.dto.response.ProductResponse;
import com.hoang.jwtjava.exception.AppException;
import com.hoang.jwtjava.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductChatService {

    private static final Pattern TOKEN_SPLIT = Pattern.compile("[^\\p{L}\\p{N}]+");
    private static final Set<String> STOP_WORDS = Set.of(
            "toi", "tôi", "muon", "muốn", "mua", "tim", "tìm", "cho", "voi", "với", "cua", "của",
            "mot", "một", "cac", "các", "va", "và", "hoac", "hoặc", "duoi", "dưới", "tren", "trên",
            "gia", "giá", "bao", "nhieu", "nhiều", "khong", "không", "co", "có", "la", "là",
            "san", "sản", "pham", "phẩm", "hang", "hàng", "giup", "giúp", "tu", "tư", "van", "vấn",
            "please", "the", "a", "an", "for", "with", "under", "over", "and", "or");

    private static final String DISCLAIMER =
            "Gợi ý dựa trên catalog Easy Mart hiện tại. Giá và tồn kho lấy từ hệ thống.";

    private final ProductService productService;
    private final GeminiClient geminiClient;
    private final ChatHistoryService chatHistoryService;
    private final GeminiProperties geminiProperties;
    private final ObjectMapper objectMapper;

    public ChatAdviseResponse advise(ChatAdviseRequest request) {
        String message = request.getMessage().trim();
        String sessionId = normalizeSessionId(request.getSessionId());

        List<ProductResponse> catalog = retrieveCatalog(
                message, request.getCategoryId(), request.getMaxBudget());

        if (catalog.isEmpty()) {
            String reply = "Hiện chưa tìm thấy sản phẩm phù hợp trong catalog. "
                    + "Bạn thử đổi từ khóa, danh mục hoặc ngân sách nhé.";
            chatHistoryService.appendTurn(sessionId, message, reply);
            return ChatAdviseResponse.builder()
                    .reply(reply)
                    .sessionId(sessionId)
                    .products(List.of())
                    .disclaimer(DISCLAIMER)
                    .build();
        }

        List<ChatHistoryService.ChatTurn> history = chatHistoryService.getRecentTurns(sessionId);
        String systemPrompt = buildSystemPrompt(catalog);

        ParsedAdvice advice;
        try {
            String modelRaw = geminiClient.generateText(systemPrompt, history, message);
            advice = parseAdvice(modelRaw, catalog);
        } catch (AppException ex) {
            if (!isRecoverableChatError(ex.getErrorCode()))
                throw ex;
            log.warn("Gemini unavailable ({}), falling back to catalog suggestions", ex.getErrorCode());
            advice = catalogFallback(catalog);
        }

        chatHistoryService.appendTurn(sessionId, message, advice.reply());

        return ChatAdviseResponse.builder()
                .reply(advice.reply())
                .sessionId(sessionId)
                .products(advice.products())
                .disclaimer(DISCLAIMER)
                .build();
    }

    private static boolean isRecoverableChatError(ErrorCode code) {
        return code == ErrorCode.CHAT_UNAVAILABLE
                || code == ErrorCode.CHAT_INVALID_RESPONSE
                || code == ErrorCode.CHAT_AUTH_FAILED
                || code == ErrorCode.CHAT_QUOTA_EXCEEDED;
    }

    private ParsedAdvice catalogFallback(List<ProductResponse> catalog) {
        List<ChatProductResponse> products = catalog.stream().limit(5).map(this::toChatProduct).toList();
        String names = products.stream()
                .map(ChatProductResponse::getName)
                .filter(n -> n != null && !n.isBlank())
                .limit(3)
                .collect(Collectors.joining(", "));
        String reply = names.isBlank()
                ? "Trợ lý AI đang bận. Đây là vài sản phẩm đang bán trong catalog."
                : "Trợ lý AI đang bận, mình gợi ý nhanh từ catalog: " + names + ".";
        return new ParsedAdvice(reply, products);
    }

    List<ProductResponse> retrieveCatalog(String message, Long categoryId, Integer maxBudget) {
        int limit = Math.max(1, Math.min(20, geminiProperties.getMaxCatalogProducts()));
        PageRequest pageable = PageRequest.of(
                0,
                limit,
                Sort.by(Sort.Order.desc("featured"), Sort.Order.desc("rating"), Sort.Order.asc("price")));

        Map<Long, ProductResponse> byId = new LinkedHashMap<>();

        String keyword = extractKeywordPhrase(message);
        addProducts(byId, productService.listProducts(
                categoryId, null, null, keyword, null, maxBudget, null, null, true, pageable).getContent());

        for (String token : extractTokens(message)) {
            if (byId.size() >= limit)
                break;
            addProducts(byId, productService.listProducts(
                    categoryId, null, null, token, null, maxBudget, null, null, true,
                    PageRequest.of(0, Math.min(6, limit), pageable.getSort())).getContent());
        }

        if (byId.isEmpty()) {
            addProducts(byId, productService.listProducts(
                    categoryId, null, true, null, null, maxBudget, null, null, true, pageable).getContent());
        }

        if (byId.isEmpty()) {
            addProducts(byId, productService.listProducts(
                    categoryId, null, null, null, null, maxBudget, null, null, true, pageable).getContent());
        }

        return byId.values().stream().limit(limit).toList();
    }

    private void addProducts(Map<Long, ProductResponse> byId, List<ProductResponse> products) {
        for (ProductResponse product : products) {
            if (product != null && product.getId() != null)
                byId.putIfAbsent(product.getId(), product);
        }
    }

    String buildSystemPrompt(List<ProductResponse> catalog) {
        List<Map<String, Object>> compact = catalog.stream()
                .map(this::toPromptProduct)
                .toList();

        String catalogJson;
        try {
            catalogJson = objectMapper.writeValueAsString(compact);
        } catch (Exception ex) {
            throw new AppException(ErrorCode.CHAT_UNAVAILABLE);
        }

        return """
                Bạn là trợ lý mua sắm của Easy Mart.
                Chỉ tư vấn dựa trên catalog JSON bên dưới. Không bịa sản phẩm, giá, tồn kho hay danh mục.
                Nếu không có sản phẩm phù hợp trong catalog, hãy nói rõ và gợi ý cách lọc khác.
                Trả lời bằng tiếng Việt, ngắn gọn, thân thiện.
                Bắt buộc trả JSON đúng schema:
                {"reply":"string","recommendedProductIds":[number]}
                recommendedProductIds chỉ chứa id có trong catalog, tối đa 5 id, ưu tiên phù hợp nhất.
                Catalog:
                %s
                """.formatted(catalogJson);
    }

    private Map<String, Object> toPromptProduct(ProductResponse product) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", product.getId());
        map.put("name", product.getName());
        map.put("price", product.getPrice());
        map.put("discountPrice", product.getDiscountPrice());
        map.put("stock", product.getStock());
        map.put("categoryName", product.getCategoryName());
        map.put("rating", product.getRating());
        map.put("featured", product.isFeatured());
        map.put("description", truncate(product.getDescription(), geminiProperties.getDescriptionMaxChars()));
        return map;
    }

    ParsedAdvice parseAdvice(String modelRaw, List<ProductResponse> catalog) {
        Map<Long, ProductResponse> byId = catalog.stream()
                .collect(Collectors.toMap(ProductResponse::getId, p -> p, (a, b) -> a, LinkedHashMap::new));

        try {
            String json = extractJsonObject(modelRaw);
            JsonNode root = objectMapper.readTree(json);
            String reply = root.path("reply").asText("").trim();
            if (reply.isBlank())
                throw new AppException(ErrorCode.CHAT_INVALID_RESPONSE);

            List<ChatProductResponse> products = new ArrayList<>();
            JsonNode ids = root.path("recommendedProductIds");
            if (ids.isArray()) {
                for (JsonNode idNode : ids) {
                    if (!idNode.canConvertToLong())
                        continue;
                    ProductResponse product = byId.get(idNode.asLong());
                    if (product != null)
                        products.add(toChatProduct(product));
                    if (products.size() >= 5)
                        break;
                }
            }

            if (products.isEmpty())
                products = catalog.stream().limit(3).map(this::toChatProduct).toList();

            return new ParsedAdvice(reply, products);
        } catch (AppException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("Invalid Gemini chat JSON: {}", ex.getMessage());
            throw new AppException(ErrorCode.CHAT_INVALID_RESPONSE);
        }
    }

    private ChatProductResponse toChatProduct(ProductResponse product) {
        String imageUrl = null;
        if (product.getImages() != null && !product.getImages().isEmpty())
            imageUrl = product.getImages().get(0);

        return ChatProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .price(product.getPrice())
                .discountPrice(product.getDiscountPrice())
                .stock(product.getStock())
                .categoryName(product.getCategoryName())
                .rating(product.getRating())
                .imageUrl(imageUrl)
                .build();
    }

    static String extractKeywordPhrase(String message) {
        List<String> tokens = extractTokens(message);
        if (tokens.isEmpty())
            return null;
        return String.join(" ", tokens.stream().limit(4).toList());
    }

    static List<String> extractTokens(String message) {
        if (message == null || message.isBlank())
            return List.of();

        LinkedHashSet<String> tokens = new LinkedHashSet<>();
        for (String raw : TOKEN_SPLIT.split(message.toLowerCase(Locale.ROOT))) {
            String token = raw.trim();
            if (token.length() < 2 || STOP_WORDS.contains(token))
                continue;
            if (token.chars().allMatch(Character::isDigit))
                continue;
            tokens.add(token);
            if (tokens.size() >= 6)
                break;
        }
        return List.copyOf(tokens);
    }

    private static String normalizeSessionId(String sessionId) {
        if (sessionId == null || sessionId.isBlank())
            return UUID.randomUUID().toString();
        return sessionId.trim();
    }

    private static String truncate(String value, int maxChars) {
        if (value == null)
            return "";
        String trimmed = value.trim();
        if (trimmed.length() <= maxChars)
            return trimmed;
        return trimmed.substring(0, Math.max(0, maxChars - 1)) + "…";
    }

    private static String extractJsonObject(String raw) {
        String text = raw.trim();
        if (text.startsWith("```")) {
            int firstNewline = text.indexOf('\n');
            int lastFence = text.lastIndexOf("```");
            if (firstNewline > 0 && lastFence > firstNewline)
                text = text.substring(firstNewline + 1, lastFence).trim();
        }
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start < 0 || end <= start)
            throw new AppException(ErrorCode.CHAT_INVALID_RESPONSE);
        return text.substring(start, end + 1);
    }

    record ParsedAdvice(String reply, List<ChatProductResponse> products) {
    }
}
