package com.hoang.jwtjava.service;

import com.hoang.jwtjava.dto.request.ProductReviewRequest;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductReviewServiceTest {

    @Mock
    private ProductReviewRepository productReviewRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private CatalogCacheService catalogCacheService;

    @InjectMocks
    private ProductReviewService productReviewService;

    @Test
    void createReviewUpdatesProductAverage() {
        Product product = Product.builder().id(10L).name("Phone").rating(BigDecimal.ZERO).reviewCount(0).build();
        User user = User.builder().id("u1").email("buyer@example.com").fullName("Buyer").build();
        ProductReviewRequest request = new ProductReviewRequest();
        request.setRating(5);
        request.setComment("Great");

        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(userRepository.findByEmail("buyer@example.com")).thenReturn(Optional.of(user));
        when(productReviewRepository.existsByProduct_IdAndUser_EmailIgnoreCase(10L, "buyer@example.com"))
                .thenReturn(false);
        when(orderRepository.existsByUserEmailAndStatusAndProductId("buyer@example.com", OrderStatus.PAID, 10L))
                .thenReturn(true);
        when(productReviewRepository.save(any(ProductReview.class))).thenAnswer(inv -> {
            ProductReview r = inv.getArgument(0);
            r.setId(1L);
            return r;
        });
        when(productReviewRepository.countByProduct_Id(10L)).thenReturn(1L);
        when(productReviewRepository.averageRatingByProductId(10L)).thenReturn(5.0);

        ProductReviewResponse response = productReviewService.create("buyer@example.com", 10L, request);

        assertEquals(5, response.getRating());
        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(productCaptor.capture());
        assertEquals(0, new BigDecimal("5.00").compareTo(productCaptor.getValue().getRating()));
        assertEquals(1, productCaptor.getValue().getReviewCount());
        verify(catalogCacheService).invalidateProducts();
    }

    @Test
    void createReviewRequiresPaidPurchase() {
        Product product = Product.builder().id(10L).build();
        User user = User.builder().id("u1").email("buyer@example.com").build();
        ProductReviewRequest request = new ProductReviewRequest();
        request.setRating(4);

        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(userRepository.findByEmail("buyer@example.com")).thenReturn(Optional.of(user));
        when(productReviewRepository.existsByProduct_IdAndUser_EmailIgnoreCase(10L, "buyer@example.com"))
                .thenReturn(false);
        when(orderRepository.existsByUserEmailAndStatusAndProductId("buyer@example.com", OrderStatus.PAID, 10L))
                .thenReturn(false);

        AppException ex = assertThrows(AppException.class,
                () -> productReviewService.create("buyer@example.com", 10L, request));
        assertEquals(ErrorCode.REVIEW_NOT_ALLOWED, ex.getErrorCode());
    }
}
