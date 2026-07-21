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

        sellerEmails.forEach(sellerEmail -> publishSellerEvent(order, sellerEmail, type));
        publishToBuyer(order, type);
    }

    public void publishToSeller(Order order, String sellerEmail, String type) {
        publishSellerEvent(order, sellerEmail, type);
        publishToBuyer(order, type);
    }

    public void publishToBuyer(Order order, String type) {
        if (order.getUser() == null || order.getUser().getEmail() == null || order.getUser().getEmail().isBlank())
            return;

        eventPublisher.publishEvent(new UserOrderChangedEvent(
                order.getUser().getEmail(),
                OrderRealtimeEvent.builder()
                        .type(type)
                        .order(toBuyerResponse(order))
                        .occurredAt(LocalDateTime.now())
                        .build()));
    }

    private void publishSellerEvent(Order order, String sellerEmail, String type) {
        eventPublisher.publishEvent(new UserOrderChangedEvent(
                sellerEmail,
                OrderRealtimeEvent.builder()
                        .type(type)
                        .order(toSellerResponse(order, sellerEmail))
                        .occurredAt(LocalDateTime.now())
                        .build()));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void sendAfterCommit(UserOrderChangedEvent event) {
        messagingTemplate.convertAndSendToUser(
                event.userEmail(),
                "/queue/orders",
                event.payload());
    }

    private OrderResponse toBuyerResponse(Order order) {
        var items = order.getItems().stream()
                .map(this::toItemResponse)
                .toList();

        int itemsSubtotal = items.stream()
                .mapToInt(item -> item.getUnitPrice() * item.getQuantity())
                .sum();
        Integer subtotal = order.getSubtotal() != null ? order.getSubtotal() : itemsSubtotal;
        Integer shippingFee = order.getShippingFee() != null ? order.getShippingFee() : 0;

        return OrderResponse.builder()
                .id(order.getId())
                .userEmail(order.getUser().getEmail())
                .items(items)
                .subtotal(subtotal)
                .shippingFee(shippingFee)
                .totalAmount(order.getTotalAmount())
                .receiverName(order.getReceiverName())
                .receiverPhone(order.getReceiverPhone())
                .shippingAddress(order.getShippingAddress())
                .status(order.getStatus().name())
                .createdAt(order.getCreatedAt())
                .build();
    }

    private OrderResponse toSellerResponse(Order order, String sellerEmail) {
        var items = order.getItems().stream()
                .filter(item -> sellerEmail.equalsIgnoreCase(item.getSellerEmail()))
                .map(this::toItemResponse)
                .toList();

        int sellerTotal = items.stream()
                .mapToInt(item -> item.getUnitPrice() * item.getQuantity())
                .sum();
        Integer shippingFee = order.getShippingFee() != null ? order.getShippingFee() : 0;

        return OrderResponse.builder()
                .id(order.getId())
                .userEmail(order.getUser().getEmail())
                .items(items)
                .subtotal(sellerTotal)
                .shippingFee(shippingFee)
                .totalAmount(sellerTotal)
                .receiverName(order.getReceiverName())
                .receiverPhone(order.getReceiverPhone())
                .shippingAddress(order.getShippingAddress())
                .status(order.getStatus().name())
                .createdAt(order.getCreatedAt())
                .build();
    }

    private OrderItemResponse toItemResponse(OrderItem item) {
        return OrderItemResponse.builder()
                .productId(item.getProductId())
                .productName(item.getProductName())
                .unitPrice(item.getUnitPrice())
                .quantity(item.getQuantity())
                .sellerEmail(item.getSellerEmail())
                .fulfillmentStatus(effectiveFulfillmentStatus(item).name())
                .build();
    }

    private static FulfillmentStatus effectiveFulfillmentStatus(OrderItem item) {
        return item.getFulfillmentStatus() == null
                ? FulfillmentStatus.AWAITING_CONFIRMATION
                : item.getFulfillmentStatus();
    }

    public record UserOrderChangedEvent(
            String userEmail,
            OrderRealtimeEvent payload) {
    }
}
