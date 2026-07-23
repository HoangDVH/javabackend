package com.hoang.jwtjava.service;

import com.hoang.jwtjava.dto.response.ProductReviewResponse;
import com.hoang.jwtjava.entity.Product;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ProductReviewRealtimeNotifierTest {

    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private ProductReviewRealtimeNotifier notifier;

    @Test
    void publishNotifiesTopicSellerAndReviewer() {
        Product product = Product.builder()
                .id(12L)
                .sellerEmail("seller@example.com")
                .rating(new BigDecimal("4.50"))
                .reviewCount(2)
                .build();
        ProductReviewResponse review = ProductReviewResponse.builder()
                .id(1L)
                .productId(12L)
                .rating(5)
                .build();

        notifier.publish(ProductReviewRealtimeNotifier.CREATED, review, product, "buyer@example.com");

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher, times(3)).publishEvent(captor.capture());

        List<Object> events = captor.getAllValues();
        assertTrue(events.stream().anyMatch(e -> e instanceof ProductReviewRealtimeNotifier.ProductReviewChangedEvent));
        assertEquals(2, events.stream().filter(e -> e instanceof ProductReviewRealtimeNotifier.UserReviewChangedEvent).count());

        var topic = events.stream()
                .filter(e -> e instanceof ProductReviewRealtimeNotifier.ProductReviewChangedEvent)
                .map(e -> (ProductReviewRealtimeNotifier.ProductReviewChangedEvent) e)
                .findFirst()
                .orElseThrow();
        assertEquals(12L, topic.productId());
        assertEquals(ProductReviewRealtimeNotifier.CREATED, topic.payload().getType());
    }
}
