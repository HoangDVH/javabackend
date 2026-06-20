package com.hoang.jwtjava.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hoang.jwtjava.config.AppRedisProperties;
import com.hoang.jwtjava.config.CatalogCacheProperties;
import com.hoang.jwtjava.dto.response.CategoryResponse;
import com.hoang.jwtjava.dto.response.PageResponse;
import com.hoang.jwtjava.dto.response.ProductResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class CatalogCacheService {

    private static final String PRODUCT_VERSION_KEY = "catalog:products:version";
    private static final String CATEGORY_LIST_KEY = "catalog:categories:list";

    private static final TypeReference<PageResponse<ProductResponse>> PRODUCT_LIST_TYPE =
            new TypeReference<>() {};
    private static final TypeReference<List<CategoryResponse>> CATEGORY_LIST_TYPE =
            new TypeReference<>() {};

    private final CatalogCacheProperties catalogCacheProperties;
    private final AppRedisProperties redisProperties;
    private final Optional<StringRedisTemplate> redisTemplate;
    private final ObjectMapper objectMapper;

    public CatalogCacheService(
            CatalogCacheProperties catalogCacheProperties,
            AppRedisProperties redisProperties,
            ObjectMapper objectMapper,
            @Autowired(required = false) StringRedisTemplate redisTemplate) {
        this.catalogCacheProperties = catalogCacheProperties;
        this.redisProperties = redisProperties;
        this.objectMapper = objectMapper;
        this.redisTemplate = Optional.ofNullable(redisTemplate);
    }

    public boolean isActive() {
        return redisProperties.isEnabled()
                && catalogCacheProperties.isEnabled()
                && redisTemplate.isPresent();
    }

    public Optional<PageResponse<ProductResponse>> getProductList(
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
        return readJson(productListKey(categoryId, brandId, isFeatured, keyword, minPrice, maxPrice,
                minRating, hasDiscount, inStock, pageable), PRODUCT_LIST_TYPE);
    }

    public void putProductList(
            Long categoryId,
            Long brandId,
            Boolean isFeatured,
            String keyword,
            Integer minPrice,
            Integer maxPrice,
            BigDecimal minRating,
            Boolean hasDiscount,
            Boolean inStock,
            Pageable pageable,
            PageResponse<ProductResponse> page) {
        writeJson(
                productListKey(categoryId, brandId, isFeatured, keyword, minPrice, maxPrice, minRating,
                        hasDiscount, inStock, pageable),
                page,
                catalogCacheProperties.getProductListTtlSeconds());
    }

    public Optional<ProductResponse> getProduct(Long id) {
        return readJson(productDetailKey(id), ProductResponse.class);
    }

    public void putProduct(Long id, ProductResponse product) {
        writeJson(productDetailKey(id), product, catalogCacheProperties.getProductDetailTtlSeconds());
    }

    public Optional<List<CategoryResponse>> getCategoryList() {
        return readJson(CATEGORY_LIST_KEY, CATEGORY_LIST_TYPE);
    }

    public void putCategoryList(List<CategoryResponse> categories) {
        writeJson(CATEGORY_LIST_KEY, categories, catalogCacheProperties.getCategoryListTtlSeconds());
    }

    public Optional<CategoryResponse> getCategory(Long id) {
        return readJson(categoryDetailKey(id), CategoryResponse.class);
    }

    public void putCategory(Long id, CategoryResponse category) {
        writeJson(categoryDetailKey(id), category, catalogCacheProperties.getCategoryDetailTtlSeconds());
    }

    public void invalidateProducts() {
        if (!isActive())
            return;
        try {
            redisTemplate.get().opsForValue().increment(PRODUCT_VERSION_KEY);
        } catch (RuntimeException ex) {
            log.warn("Failed to invalidate product cache: {}", ex.getMessage());
        }
    }

    private String productListKey(
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
        String queryHash = sha256Hex(canonicalProductListQuery(
                categoryId, brandId, isFeatured, keyword, minPrice, maxPrice, minRating, hasDiscount,
                inStock, pageable));
        return "catalog:products:v:" + productCacheVersion() + ":q:" + queryHash;
    }

    private String productDetailKey(Long id) {
        return "catalog:product:v:" + productCacheVersion() + ":id:" + id;
    }

    private String categoryDetailKey(Long id) {
        return "catalog:category:id:" + id;
    }

    private long productCacheVersion() {
        try {
            String value = redisTemplate.get().opsForValue().get(PRODUCT_VERSION_KEY);
            if (value == null || value.isBlank())
                return 1L;
            return Long.parseLong(value);
        } catch (RuntimeException ex) {
            log.warn("Failed to read product cache version: {}", ex.getMessage());
            return 1L;
        }
    }

    private String canonicalProductListQuery(
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
        StringBuilder sb = new StringBuilder(160);
        appendParam(sb, "categoryId", categoryId);
        appendParam(sb, "brandId", brandId);
        appendParam(sb, "isFeatured", isFeatured);
        if (keyword != null && !keyword.isBlank())
            appendParam(sb, "keyword", keyword.trim().toLowerCase());
        appendParam(sb, "minPrice", minPrice);
        appendParam(sb, "maxPrice", maxPrice);
        appendParam(sb, "minRating", minRating);
        appendParam(sb, "hasDiscount", hasDiscount);
        appendParam(sb, "inStock", inStock);
        appendParam(sb, "page", pageable.getPageNumber());
        appendParam(sb, "size", pageable.getPageSize());
        Sort sort = pageable.getSort();
        if (sort.isSorted()) {
            for (Sort.Order order : sort)
                sb.append("|sort=").append(order.getProperty()).append(',').append(order.getDirection());
        }
        return sb.toString();
    }

    private static void appendParam(StringBuilder sb, String name, Object value) {
        if (value != null)
            sb.append('|').append(name).append('=').append(value);
    }

    private <T> Optional<T> readJson(String key, Class<T> type) {
        if (!isActive())
            return Optional.empty();
        try {
            String json = redisTemplate.get().opsForValue().get(key);
            if (json == null || json.isBlank())
                return Optional.empty();
            return Optional.of(objectMapper.readValue(json, type));
        } catch (JsonProcessingException ex) {
            log.warn("Catalog cache read failed for {}: {}", key, ex.getMessage());
            return Optional.empty();
        } catch (RuntimeException ex) {
            log.warn("Catalog cache read failed for {}: {}", key, ex.getMessage());
            return Optional.empty();
        }
    }

    private <T> Optional<T> readJson(String key, TypeReference<T> type) {
        if (!isActive())
            return Optional.empty();
        try {
            String json = redisTemplate.get().opsForValue().get(key);
            if (json == null || json.isBlank())
                return Optional.empty();
            return Optional.of(objectMapper.readValue(json, type));
        } catch (JsonProcessingException ex) {
            log.warn("Catalog cache read failed for {}: {}", key, ex.getMessage());
            return Optional.empty();
        } catch (RuntimeException ex) {
            log.warn("Catalog cache read failed for {}: {}", key, ex.getMessage());
            return Optional.empty();
        }
    }

    private void writeJson(String key, Object value, int ttlSeconds) {
        if (!isActive())
            return;
        try {
            String json = objectMapper.writeValueAsString(value);
            redisTemplate.get().opsForValue().set(key, json, Duration.ofSeconds(Math.max(1, ttlSeconds)));
        } catch (JsonProcessingException ex) {
            log.warn("Catalog cache write failed for {}: {}", key, ex.getMessage());
        } catch (RuntimeException ex) {
            log.warn("Catalog cache write failed for {}: {}", key, ex.getMessage());
        }
    }

    private static String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 not available", ex);
        }
    }
}
