package com.hoang.jwtjava.repository;

import com.hoang.jwtjava.entity.Product;
import org.springframework.data.jpa.domain.Specification;

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
}
