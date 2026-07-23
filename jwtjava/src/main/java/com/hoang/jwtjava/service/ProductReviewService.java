package com.hoang.jwtjava.service;

import com.hoang.jwtjava.dto.request.ProductReviewRequest;
import com.hoang.jwtjava.dto.response.PageResponse;
import com.hoang.jwtjava.dto.response.ProductReviewResponse;
import com.hoang.jwtjava.entity.OrderStatus;
import com.hoang.jwtjava.entity.Product;
import com.hoang.jwtjava.entity.ProductReview;
import com.hoang.jwtjava.entity.User;
import com.hoang.jwtjava.exception.AppException;
import com.hoang.jwtjava.exception.ErrorCode;
import com.hoang.jwtjava.repository.OrderRepository;
import com.hoang.jwtjava.repository.ProductRepository;
import com.hoang.jwtjava.repository.ProductReviewRepository;
import com.hoang.jwtjava.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
public class ProductReviewService {

    private final ProductReviewRepository productReviewRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final CatalogCacheService catalogCacheService;

    @Transactional(readOnly = true)
    public PageResponse<ProductReviewResponse> listByProduct(Long productId, Pageable pageable) {
        requireProduct(productId);
        Page<ProductReview> page = productReviewRepository.findByProduct_Id(productId, pageable);
        return PageResponse.<ProductReviewResponse>builder()
                .items(page.getContent().stream().map(this::toResponse).toList())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .page(page.getNumber())
                .size(page.getSize())
                .build();
    }

    @Transactional
    public ProductReviewResponse create(String userEmail, Long productId, ProductReviewRequest request) {
        Product product = requireProduct(productId);
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (productReviewRepository.existsByProduct_IdAndUser_EmailIgnoreCase(productId, userEmail))
            throw new AppException(ErrorCode.REVIEW_ALREADY_EXISTS);

        if (!orderRepository.existsByUserEmailAndStatusAndProductId(userEmail, OrderStatus.PAID, productId))
            throw new AppException(ErrorCode.REVIEW_NOT_ALLOWED);

        ProductReview review = ProductReview.builder()
                .product(product)
                .user(user)
                .rating(request.getRating())
                .comment(normalizeComment(request.getComment()))
                .build();
        ProductReview saved = productReviewRepository.save(review);
        refreshProductRating(product);
        return toResponse(saved);
    }

    @Transactional
    public ProductReviewResponse update(String userEmail, Long productId, Long reviewId, ProductReviewRequest request) {
        ProductReview review = productReviewRepository.findByIdAndProduct_Id(reviewId, productId)
                .orElseThrow(() -> new AppException(ErrorCode.REVIEW_NOT_FOUND));
        if (!review.getUser().getEmail().equalsIgnoreCase(userEmail))
            throw new AppException(ErrorCode.UNAUTHORIZED);

        review.setRating(request.getRating());
        review.setComment(normalizeComment(request.getComment()));
        ProductReview saved = productReviewRepository.save(review);
        refreshProductRating(review.getProduct());
        return toResponse(saved);
    }

    @Transactional
    public void delete(String userEmail, boolean isAdmin, Long productId, Long reviewId) {
        ProductReview review = productReviewRepository.findByIdAndProduct_Id(reviewId, productId)
                .orElseThrow(() -> new AppException(ErrorCode.REVIEW_NOT_FOUND));
        if (!isAdmin && !review.getUser().getEmail().equalsIgnoreCase(userEmail))
            throw new AppException(ErrorCode.UNAUTHORIZED);

        Product product = review.getProduct();
        productReviewRepository.delete(review);
        refreshProductRating(product);
    }

    private void refreshProductRating(Product product) {
        long count = productReviewRepository.countByProduct_Id(product.getId());
        Double avg = productReviewRepository.averageRatingByProductId(product.getId());
        product.setReviewCount((int) count);
        product.setRating(avg == null
                ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP));
        productRepository.save(product);
        catalogCacheService.invalidateProducts();
    }

    private Product requireProduct(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));
    }

    private static String normalizeComment(String comment) {
        if (comment == null || comment.isBlank())
            return null;
        return comment.trim();
    }

    private ProductReviewResponse toResponse(ProductReview review) {
        User user = review.getUser();
        return ProductReviewResponse.builder()
                .id(review.getId())
                .productId(review.getProduct().getId())
                .userEmail(user.getEmail())
                .userFullName(user.getFullName())
                .userAvatarUrl(user.getAvatarUrl())
                .rating(review.getRating())
                .comment(review.getComment())
                .createdAt(review.getCreatedAt())
                .updatedAt(review.getUpdatedAt())
                .build();
    }
}
