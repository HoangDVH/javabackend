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
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

@Slf4j
@Service
public class CatalogCacheService {

    private static final String PRODUCT_VERSION_KEY = "catalog:products:version";
    private static final String CATEGORY_LIST_KEY = "catalog:categories:list";
    private static final String LOCK_PREFIX = "catalog:lock:";

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

    public PageResponse<ProductResponse> getOrLoadProductList(
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
            Supplier<PageResponse<ProductResponse>> loader) {
        String key = productListKey(categoryId, brandId, isFeatured, keyword, minPrice, maxPrice,
                minRating, hasDiscount, inStock, pageable);
        return getOrLoad(key, PRODUCT_LIST_TYPE, catalogCacheProperties.getProductListTtlSeconds(), loader);
    }

    public ProductResponse getOrLoadProduct(Long id, Supplier<ProductResponse> loader) {
        return getOrLoad(productDetailKey(id), ProductResponse.class,
                catalogCacheProperties.getProductDetailTtlSeconds(), loader);
    }

    public List<CategoryResponse> getOrLoadCategoryList(Supplier<List<CategoryResponse>> loader) {
        return getOrLoad(CATEGORY_LIST_KEY, CATEGORY_LIST_TYPE,
                catalogCacheProperties.getCategoryListTtlSeconds(), loader);
    }

    public CategoryResponse getOrLoadCategory(Long id, Supplier<CategoryResponse> loader) {
        return getOrLoad(categoryDetailKey(id), CategoryResponse.class,
                catalogCacheProperties.getCategoryDetailTtlSeconds(), loader);
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

    private <T> T getOrLoad(String key, Class<T> type, int ttlSeconds, Supplier<T> loader) {
        Optional<T> cached = readJson(key, type);
        if (cached.isPresent()) {
            log.debug("Catalog cache HIT key={}", key);
            return cached.get();
        }
        return loadWithStampedeGuard(key, ttlSeconds, loader, () -> readJson(key, type));
    }

    private <T> T getOrLoad(String key, TypeReference<T> type, int ttlSeconds, Supplier<T> loader) {
        Optional<T> cached = readJson(key, type);
        if (cached.isPresent()) {
            log.debug("Catalog cache HIT key={}", key);
            return cached.get();
        }
        return loadWithStampedeGuard(key, ttlSeconds, loader, () -> readJson(key, type));
    }

    private <T> T loadWithStampedeGuard(
            String key,
            int ttlSeconds,
            Supplier<T> loader,
            Supplier<Optional<T>> reader) {
        if (!isActive()) {
            log.info("Catalog cache MISS (inactive) key={}", key);
            return loader.get();
        }

        log.info("Catalog cache MISS key={}", key);
        String lockKey = LOCK_PREFIX + key;
        boolean locked = tryLock(lockKey);
        if (locked) {
            try {
                Optional<T> again = reader.get();
                if (again.isPresent())
                    return again.get();
                T loaded = loader.get();
                writeJson(key, loaded, ttlSeconds);
                return loaded;
            } finally {
                unlock(lockKey);
            }
        }

        Optional<T> waited = waitForCache(reader);
        if (waited.isPresent())
            return waited.get();

        log.info("Catalog cache stampede fallback load key={}", key);
        T loaded = loader.get();
        writeJson(key, loaded, ttlSeconds);
        return loaded;
    }

    private boolean tryLock(String lockKey) {
        try {
            Boolean ok = redisTemplate.get().opsForValue().setIfAbsent(
                    lockKey,
                    "1",
                    Duration.ofSeconds(Math.max(1, catalogCacheProperties.getStampedeLockSeconds())));
            return Boolean.TRUE.equals(ok);
        } catch (RuntimeException ex) {
            log.warn("Catalog stampede lock failed: {}", ex.getMessage());
            return true; // fail open: allow this request to load
        }
    }

    private void unlock(String lockKey) {
        try {
            redisTemplate.get().delete(lockKey);
        } catch (RuntimeException ex) {
            log.warn("Catalog stampede unlock failed: {}", ex.getMessage());
        }
    }

    private <T> Optional<T> waitForCache(Supplier<Optional<T>> reader) {
        long deadline = System.currentTimeMillis() + Math.max(50, catalogCacheProperties.getStampedeWaitMs());
        while (System.currentTimeMillis() < deadline) {
            Optional<T> value = reader.get();
            if (value.isPresent())
                return value;
            try {
                Thread.sleep(20L + ThreadLocalRandom.current().nextInt(20));
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                return Optional.empty();
            }
        }
        return Optional.empty();
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
        } catch (JsonProcessingException | RuntimeException ex) {
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
        } catch (JsonProcessingException | RuntimeException ex) {
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
        } catch (JsonProcessingException | RuntimeException ex) {
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
