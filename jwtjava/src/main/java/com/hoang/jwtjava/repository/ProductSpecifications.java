package com.hoang.jwtjava.repository;

import com.hoang.jwtjava.entity.Product;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.Locale;

public final class ProductSpecifications {

    private ProductSpecifications() {
    }

    public static Specification<Product> categoryIdEquals(Long categoryId) {
        return (root, query, cb) ->
                categoryId == null ? cb.conjunction() : cb.equal(root.get("category").get("id"), categoryId);
    }

    public static Specification<Product> brandIdEquals(Long brandId) {
        return (root, query, cb) ->
                brandId == null ? cb.conjunction() : cb.equal(root.get("brandId"), brandId);
    }

    public static Specification<Product> isFeaturedEquals(Boolean isFeatured) {
        return (root, query, cb) ->
                isFeatured == null ? cb.conjunction() : cb.equal(root.get("featured"), isFeatured);
    }

    public static Specification<Product> keywordContains(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank())
                return cb.conjunction();

            String normalizedKeyword = "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("name")), normalizedKeyword),
                    cb.like(cb.lower(root.get("description")), normalizedKeyword)
            );
        };
    }

    public static Specification<Product> priceGte(Integer minPrice) {
        return (root, query, cb) ->
                minPrice == null ? cb.conjunction() : cb.greaterThanOrEqualTo(root.get("price"), minPrice);
    }

    public static Specification<Product> priceLte(Integer maxPrice) {
        return (root, query, cb) ->
                maxPrice == null ? cb.conjunction() : cb.lessThanOrEqualTo(root.get("price"), maxPrice);
    }

    public static Specification<Product> ratingGte(BigDecimal minRating) {
        return (root, query, cb) ->
                minRating == null ? cb.conjunction() : cb.greaterThanOrEqualTo(root.get("rating"), minRating);
    }

    public static Specification<Product> hasDiscount(Boolean hasDiscount) {
        return (root, query, cb) -> {
            if (hasDiscount == null)
                return cb.conjunction();
            if (Boolean.TRUE.equals(hasDiscount))
                return cb.lessThan(root.get("discountPrice"), root.get("price"));
            return cb.greaterThanOrEqualTo(root.get("discountPrice"), root.get("price"));
        };
    }

    public static Specification<Product> inStock(Boolean inStock) {
        return (root, query, cb) -> {
            if (inStock == null)
                return cb.conjunction();
            if (Boolean.TRUE.equals(inStock))
                return cb.greaterThan(root.get("stock"), 0);
            return cb.lessThanOrEqualTo(root.get("stock"), 0);
        };
    }
}
