package com.hoang.jwtjava.service;

import com.hoang.jwtjava.dto.response.OrderItemResponse;
import com.hoang.jwtjava.dto.response.OrderRealtimeEvent;
import com.hoang.jwtjava.dto.response.OrderResponse;
import com.hoang.jwtjava.entity.FulfillmentStatus;
import com.hoang.jwtjava.entity.Order;
import com.hoang.jwtjava.entity.OrderItem;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class OrderRealtimeNotifier {

    public static final String CREATED = "ORDER_CREATED";
    public static final String STATUS_CHANGED = "ORDER_STATUS_CHANGED";
    public static final String FULFILLMENT_STATUS_CHANGED = "FULFILLMENT_STATUS_CHANGED";

    private final ApplicationEventPublisher eventPublisher;
    private final SimpMessagingTemplate messagingTemplate;

    public void publishToSellers(Order order, String type) {
        Set<String> sellerEmails = new LinkedHashSet<>();
        order.getItems().forEach(item -> {
            if (item.getSellerEmail() != null && !item.getSellerEmail().isBlank())
                sellerEmails.add(item.getSellerEmail());
        });

        sellerEmails.forEach(sellerEmail -> publishToSeller(order, sellerEmail, type));
    }

    public void publishToSeller(Order order, String sellerEmail, String type) {
        eventPublisher.publishEvent(new SellerOrderChangedEvent(
                sellerEmail,
                OrderRealtimeEvent.builder()
                        .type(type)
                        .order(toSellerResponse(order, sellerEmail))
                        .occurredAt(LocalDateTime.now())
                        .build()));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void sendAfterCommit(SellerOrderChangedEvent event) {
        messagingTemplate.convertAndSendToUser(
                event.sellerEmail(),
                "/queue/orders",
                event.payload());
    }

    private OrderResponse toSellerResponse(Order order, String sellerEmail) {
        var items = order.getItems().stream()
                .filter(item -> sellerEmail.equalsIgnoreCase(item.getSellerEmail()))
                .map(item -> OrderItemResponse.builder()
                        .productId(item.getProductId())
                        .productName(item.getProductName())
                        .unitPrice(item.getUnitPrice())
                        .quantity(item.getQuantity())
                        .sellerEmail(item.getSellerEmail())
                        .fulfillmentStatus(effectiveFulfillmentStatus(item).name())
                        .build())
                .toList();

        int sellerTotal = items.stream()
                .mapToInt(item -> item.getUnitPrice() * item.getQuantity())
                .sum();

        return OrderResponse.builder()
                .id(order.getId())
                .userEmail(order.getUser().getEmail())
                .items(items)
                .totalAmount(sellerTotal)
                .status(order.getStatus().name())
                .createdAt(order.getCreatedAt())
                .build();
    }

    private static FulfillmentStatus effectiveFulfillmentStatus(OrderItem item) {
        return item.getFulfillmentStatus() == null
                ? FulfillmentStatus.AWAITING_CONFIRMATION
                : item.getFulfillmentStatus();
    }

    public record SellerOrderChangedEvent(
            String sellerEmail,
            OrderRealtimeEvent payload) {
    }
}
