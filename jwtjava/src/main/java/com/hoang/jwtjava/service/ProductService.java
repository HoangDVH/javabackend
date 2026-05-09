package com.hoang.jwtjava.service;

import com.hoang.jwtjava.dto.request.ProductCreateRequest;
import com.hoang.jwtjava.dto.response.ProductResponse;
import com.hoang.jwtjava.entity.Product;
import com.hoang.jwtjava.exception.AppException;
import com.hoang.jwtjava.exception.ErrorCode;
import com.hoang.jwtjava.mapper.ProductMapper;
import com.hoang.jwtjava.entity.Category;
import com.hoang.jwtjava.repository.CategoryRepository;
import com.hoang.jwtjava.repository.ProductRepository;
import com.hoang.jwtjava.repository.ProductSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;
    private final CategoryRepository categoryRepository;
    private final ProductImageStorageService productImageStorageService;

    @Transactional(readOnly = true)
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
        Specification<Product> spec = Specification.allOf(
                ProductSpecifications.categoryIdEquals(categoryId),
                ProductSpecifications.brandIdEquals(brandId),
                ProductSpecifications.isFeaturedEquals(isFeatured),
                ProductSpecifications.keywordContains(keyword),
                ProductSpecifications.priceGte(minPrice),
                ProductSpecifications.priceLte(maxPrice),
                ProductSpecifications.ratingGte(minRating),
                ProductSpecifications.hasDiscount(hasDiscount),
                ProductSpecifications.inStock(inStock)
        );
        return productRepository.findAll(spec, pageable).map(productMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public ProductResponse getProduct(Long id) {
        return productMapper.toResponse(
                productRepository.findById(id)
                        .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND))
        );
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> getSellerProducts(String sellerEmail) {
        return productRepository.findBySellerEmailIgnoreCaseOrderByCreatedAtDesc(sellerEmail)
                .stream()
                .map(productMapper::toResponse)
                .toList();
    }

    @Transactional
    public ProductResponse createProduct(ProductCreateRequest request, String actorEmail, boolean isAdmin) {
        Category category = resolveCategory(request.getCategoryId());
        Product product = productMapper.toEntity(request, category);
        if (!isAdmin)
            product.setSellerEmail(actorEmail);
        product.setImages(productImageStorageService.resolveImageUrlsForPersistence(request.getImages()));
        return productMapper.toResponse(productRepository.save(product));
    }

    @Transactional
    public ProductResponse updateProduct(Long id, ProductCreateRequest request, String actorEmail, boolean isAdmin) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));
        if (!canManageProduct(product, actorEmail, isAdmin))
            throw new AppException(ErrorCode.UNAUTHORIZED);
        Category category = resolveCategory(request.getCategoryId());
        productMapper.updateEntity(product, request, category);
        if (!isAdmin && product.getSellerEmail() == null)
            product.setSellerEmail(actorEmail);
        product.setImages(productImageStorageService.resolveImageUrlsForPersistence(request.getImages()));
        return productMapper.toResponse(productRepository.save(product));
    }

    @Transactional
    public void deleteProduct(Long id, String actorEmail, boolean isAdmin) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));
        if (!canManageProduct(product, actorEmail, isAdmin))
            throw new AppException(ErrorCode.UNAUTHORIZED);
        productRepository.deleteById(id);
    }

    private Category resolveCategory(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));
    }

    private boolean canManageProduct(Product product, String actorEmail, boolean isAdmin) {
        if (isAdmin)
            return true;
        return actorEmail != null && actorEmail.equalsIgnoreCase(product.getSellerEmail());
    }
}
