package com.hoang.jwtjava.mapper;

import com.hoang.jwtjava.dto.request.ProductCreateRequest;
import com.hoang.jwtjava.dto.response.ProductResponse;
import com.hoang.jwtjava.entity.Category;
import com.hoang.jwtjava.entity.Product;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ProductMapper {

    public Product toEntity(ProductCreateRequest request, Category category) {
        if (request == null) return null;
        List<String> imgs = request.getImages() != null ? new ArrayList<>(request.getImages()) : new ArrayList<>();
        return Product.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .discountPrice(request.getDiscountPrice())
                .stock(request.getStock())
                .category(category)
                .brandId(request.getBrandId())
                .images(imgs)
                .rating(request.getRating())
                .featured(Boolean.TRUE.equals(request.getIsFeatured()))
                .build();
    }

    public void updateEntity(Product product, ProductCreateRequest request, Category category) {
        if (request == null || product == null) return;
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setDiscountPrice(request.getDiscountPrice());
        product.setStock(request.getStock());
        product.setCategory(category);
        product.setBrandId(request.getBrandId());
        if (product.getImages() == null)
            product.setImages(new ArrayList<>());
        else
            product.getImages().clear();
        if (request.getImages() != null)
            product.getImages().addAll(request.getImages());
        product.setRating(request.getRating());
        product.setFeatured(Boolean.TRUE.equals(request.getIsFeatured()));
    }

    public ProductResponse toResponse(Product product) {
        if (product == null) return null;
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .discountPrice(product.getDiscountPrice())
                .stock(product.getStock())
                .categoryId(product.getCategory().getId())
                .categoryName(product.getCategory().getName())
                .brandId(product.getBrandId())
                .sellerEmail(product.getSellerEmail())
                .images(product.getImages() != null ? List.copyOf(product.getImages()) : List.of())
                .rating(product.getRating())
                .isFeatured(product.isFeatured())
                .createdAt(product.getCreatedAt())
                .build();
    }
}
