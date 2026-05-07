package com.hoang.jwtjava.repository;

import com.hoang.jwtjava.entity.Product;
import org.springframework.data.jpa.domain.Specification;

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
}
