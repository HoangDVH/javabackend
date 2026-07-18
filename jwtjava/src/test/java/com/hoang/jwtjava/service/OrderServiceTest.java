package com.hoang.jwtjava.service;

import com.hoang.jwtjava.dto.request.SellerOrderStatusUpdateRequest;
import com.hoang.jwtjava.entity.FulfillmentStatus;
import com.hoang.jwtjava.entity.Order;
import com.hoang.jwtjava.entity.OrderItem;
import com.hoang.jwtjava.entity.OrderStatus;
import com.hoang.jwtjava.entity.User;
import com.hoang.jwtjava.exception.AppException;
import com.hoang.jwtjava.exception.ErrorCode;
import com.hoang.jwtjava.repository.OrderRepository;
import com.hoang.jwtjava.repository.OrderStatusHistoryRepository;
import com.hoang.jwtjava.repository.PaymentRepository;
import com.hoang.jwtjava.repository.ProductRepository;
import com.hoang.jwtjava.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private OrderStatusHistoryRepository orderStatusHistoryRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private OrderRealtimeNotifier orderRealtimeNotifier;

    @InjectMocks
    private OrderService orderService;

    @Test
    void sellerUpdatesOnlyTheirItems() {
        OrderItem sellerItem = item("seller@example.com");
        OrderItem otherItem = item("other@example.com");
        Order order = paidOrder(sellerItem, otherItem);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);

        var response = orderService.updateSellerFulfillmentStatus(
                "seller@example.com",
                1L,
                request(FulfillmentStatus.CONFIRMED));

        assertEquals(FulfillmentStatus.CONFIRMED, sellerItem.getFulfillmentStatus());
        assertEquals(FulfillmentStatus.AWAITING_CONFIRMATION, otherItem.getFulfillmentStatus());
        assertEquals(1, response.getItems().size());
        assertEquals("CONFIRMED", response.getItems().get(0).getFulfillmentStatus());
        verify(orderRealtimeNotifier).publishToSeller(
                order,
                "seller@example.com",
                OrderRealtimeNotifier.FULFILLMENT_STATUS_CHANGED);
    }

    @Test
    void sellerCannotSkipFulfillmentSteps() {
        Order order = paidOrder(item("seller@example.com"));
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        AppException exception = assertThrows(
                AppException.class,
                () -> orderService.updateSellerFulfillmentStatus(
                        "seller@example.com",
                        1L,
                        request(FulfillmentStatus.SHIPPED)));

        assertEquals(ErrorCode.ORDER_FULFILLMENT_UPDATE_NOT_ALLOWED, exception.getErrorCode());
        verify(orderRepository, never()).save(any());
        verify(orderRealtimeNotifier, never()).publishToSeller(any(), any(), any());
    }

    private static SellerOrderStatusUpdateRequest request(FulfillmentStatus status) {
        SellerOrderStatusUpdateRequest request = new SellerOrderStatusUpdateRequest();
        request.setStatus(status);
        return request;
    }

    private static OrderItem item(String sellerEmail) {
        return OrderItem.builder()
                .productId(1L)
                .productName("Product")
                .unitPrice(100)
                .quantity(1)
                .sellerEmail(sellerEmail)
                .fulfillmentStatus(FulfillmentStatus.AWAITING_CONFIRMATION)
                .build();
    }

    private static Order paidOrder(OrderItem... items) {
        return Order.builder()
                .id(1L)
                .user(User.builder().email("buyer@example.com").build())
                .items(List.of(items))
                .totalAmount(100)
                .status(OrderStatus.PAID)
                .build();
    }
}
