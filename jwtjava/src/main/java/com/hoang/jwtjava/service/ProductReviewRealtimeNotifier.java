package com.hoang.jwtjava.service;

import com.hoang.jwtjava.dto.response.ProductReviewRealtimeEvent;
import com.hoang.jwtjava.dto.response.ProductReviewResponse;
import com.hoang.jwtjava.entity.Product;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class ProductReviewRealtimeNotifier {

    public static final String CREATED = "REVIEW_CREATED";
    public static final String UPDATED = "REVIEW_UPDATED";
    public static final String DELETED = "REVIEW_DELETED";

    private final ApplicationEventPublisher eventPublisher;
    private final SimpMessagingTemplate messagingTemplate;

    public void publish(
            String type,
            ProductReviewResponse review,
            Product product,
            String reviewerEmail) {
        if (product == null || product.getId() == null)
            return;

        ProductReviewRealtimeEvent payload = ProductReviewRealtimeEvent.builder()
                .type(type)
                .review(review)
                .productId(product.getId())
                .productRating(product.getRating() != null ? product.getRating() : BigDecimal.ZERO)
                .reviewCount(product.getReviewCount() != null ? product.getReviewCount() : 0)
                .occurredAt(LocalDateTime.now())
                .build();

        eventPublisher.publishEvent(new ProductReviewChangedEvent(product.getId(), payload));

        if (product.getSellerEmail() != null && !product.getSellerEmail().isBlank())
            eventPublisher.publishEvent(new UserReviewChangedEvent(product.getSellerEmail(), payload));

        if (reviewerEmail != null && !reviewerEmail.isBlank()
                && (product.getSellerEmail() == null
                || !product.getSellerEmail().equalsIgnoreCase(reviewerEmail)))
            eventPublisher.publishEvent(new UserReviewChangedEvent(reviewerEmail, payload));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void sendTopicAfterCommit(ProductReviewChangedEvent event) {
        messagingTemplate.convertAndSend(
                "/topic/product-reviews/" + event.productId(),
                event.payload());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void sendUserAfterCommit(UserReviewChangedEvent event) {
        messagingTemplate.convertAndSendToUser(
                event.userEmail(),
                "/queue/reviews",
                event.payload());
    }

    public record ProductReviewChangedEvent(Long productId, ProductReviewRealtimeEvent payload) {
    }

    public record UserReviewChangedEvent(String userEmail, ProductReviewRealtimeEvent payload) {
    }
}
