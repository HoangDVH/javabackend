package com.hoang.jwtjava.service;

import com.hoang.jwtjava.entity.FulfillmentStatus;
import com.hoang.jwtjava.entity.Order;
import com.hoang.jwtjava.entity.OrderItem;
import com.hoang.jwtjava.entity.OrderStatus;
import com.hoang.jwtjava.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrderRealtimeNotifierTest {

    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private OrderRealtimeNotifier notifier;

    @Test
    void publishToSellerAlsoNotifiesBuyerWithFullOrder() {
        Order order = order(
                item("seller@example.com", FulfillmentStatus.CONFIRMED),
                item("other@example.com", FulfillmentStatus.AWAITING_CONFIRMATION));

        notifier.publishToSeller(
                order,
                "seller@example.com",
                OrderRealtimeNotifier.FULFILLMENT_STATUS_CHANGED);

        ArgumentCaptor<OrderRealtimeNotifier.UserOrderChangedEvent> captor =
                ArgumentCaptor.forClass(OrderRealtimeNotifier.UserOrderChangedEvent.class);
        verify(eventPublisher, times(2)).publishEvent(captor.capture());

        var events = captor.getAllValues();
        var sellerEvent = events.stream()
                .filter(event -> "seller@example.com".equals(event.userEmail()))
                .findFirst()
                .orElseThrow();
        var buyerEvent = events.stream()
                .filter(event -> "buyer@example.com".equals(event.userEmail()))
                .findFirst()
                .orElseThrow();

        assertEquals(1, sellerEvent.payload().getOrder().getItems().size());
        assertEquals(2, buyerEvent.payload().getOrder().getItems().size());
        assertEquals(OrderRealtimeNotifier.FULFILLMENT_STATUS_CHANGED, buyerEvent.payload().getType());
        assertEquals(200, buyerEvent.payload().getOrder().getTotalAmount());
    }

    private static OrderItem item(String sellerEmail, FulfillmentStatus status) {
        return OrderItem.builder()
                .productId(1L)
                .productName("Product")
                .unitPrice(100)
                .quantity(1)
                .sellerEmail(sellerEmail)
                .fulfillmentStatus(status)
                .build();
    }

    private static Order order(OrderItem... items) {
        return Order.builder()
                .id(1L)
                .user(User.builder().email("buyer@example.com").build())
                .items(List.of(items))
                .totalAmount(200)
                .status(OrderStatus.PAID)
                .build();
    }
}
