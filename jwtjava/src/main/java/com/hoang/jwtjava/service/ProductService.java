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

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;
    private final CategoryRepository categoryRepository;
    private final ProductImageStorageService productImageStorageService;

    @Transactional(readOnly = true)
    public Page<ProductResponse> listProducts(Long categoryId, Long brandId, Boolean isFeatured, Pageable pageable) {
        Specification<Product> spec = Specification.allOf(
                ProductSpecifications.categoryIdEquals(categoryId),
                ProductSpecifications.brandIdEquals(brandId),
                ProductSpecifications.isFeaturedEquals(isFeatured)
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

    @Transactional
    public ProductResponse createProduct(ProductCreateRequest request) {
        Category category = resolveCategory(request.getCategoryId());
        Product product = productMapper.toEntity(request, category);
        product.setImages(productImageStorageService.resolveImageUrlsForPersistence(request.getImages()));
        return productMapper.toResponse(productRepository.save(product));
    }

    @Transactional
    public ProductResponse updateProduct(Long id, ProductCreateRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));
        Category category = resolveCategory(request.getCategoryId());
        productMapper.updateEntity(product, request, category);
        product.setImages(productImageStorageService.resolveImageUrlsForPersistence(request.getImages()));
        return productMapper.toResponse(productRepository.save(product));
    }

    @Transactional
    public void deleteProduct(Long id) {
        if (!productRepository.existsById(id))
            throw new AppException(ErrorCode.PRODUCT_NOT_FOUND);
        productRepository.deleteById(id);
    }

    private Category resolveCategory(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));
    }
}
