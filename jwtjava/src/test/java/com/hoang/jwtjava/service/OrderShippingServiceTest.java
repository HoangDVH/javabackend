package com.hoang.jwtjava.service;

import com.hoang.jwtjava.dto.request.OrderCreateRequest;
import com.hoang.jwtjava.dto.request.OrderItemRequest;
import com.hoang.jwtjava.dto.response.OrderResponse;
import com.hoang.jwtjava.entity.Product;
import com.hoang.jwtjava.entity.User;
import com.hoang.jwtjava.repository.OrderRepository;
import com.hoang.jwtjava.repository.OrderStatusHistoryRepository;
import com.hoang.jwtjava.repository.PaymentRepository;
import com.hoang.jwtjava.repository.ProductRepository;
import com.hoang.jwtjava.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderShippingServiceTest {

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
    void createOrderAppliesShippingUnderThreshold() {
        User user = User.builder().id("u1").email("buyer@example.com").build();
        Product product = Product.builder()
                .id(1L)
                .name("Item")
                .price(400_000)
                .stock(5)
                .sellerEmail("seller@example.com")
                .build();

        when(userRepository.findByEmail("buyer@example.com")).thenReturn(Optional.of(user));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.decrementStockIfAvailable(1L, 1)).thenReturn(1);
        when(orderRepository.save(any())).thenAnswer(invocation -> {
            var order = invocation.getArgument(0, com.hoang.jwtjava.entity.Order.class);
            order.setId(10L);
            return order;
        });

        OrderCreateRequest request = new OrderCreateRequest();
        OrderItemRequest item = new OrderItemRequest();
        item.setProductId(1L);
        item.setQuantity(1);
        request.setItems(List.of(item));
        request.setReceiverName("Nguyễn Văn A");
        request.setReceiverPhone("0901234567");
        request.setShippingAddress("123 Nguyễn Huệ, Q1, TP.HCM");

        OrderResponse response = orderService.createOrder("buyer@example.com", request);

        assertEquals(400_000, response.getSubtotal());
        assertEquals(30_000, response.getShippingFee());
        assertEquals(430_000, response.getTotalAmount());
        assertEquals("Nguyễn Văn A", response.getReceiverName());
        assertEquals("0901234567", response.getReceiverPhone());
        assertEquals("123 Nguyễn Huệ, Q1, TP.HCM", response.getShippingAddress());

        ArgumentCaptor<com.hoang.jwtjava.entity.Order> captor =
                ArgumentCaptor.forClass(com.hoang.jwtjava.entity.Order.class);
        verify(orderRepository).save(captor.capture());
        assertEquals(430_000, captor.getValue().getTotalAmount());
    }

    @Test
    void createOrderFreeShippingAtThreshold() {
        User user = User.builder().id("u1").email("buyer@example.com").build();
        Product product = Product.builder()
                .id(2L)
                .name("Item")
                .price(500_000)
                .stock(5)
                .sellerEmail("seller@example.com")
                .build();

        when(userRepository.findByEmail("buyer@example.com")).thenReturn(Optional.of(user));
        when(productRepository.findById(2L)).thenReturn(Optional.of(product));
        when(productRepository.decrementStockIfAvailable(2L, 1)).thenReturn(1);
        when(orderRepository.save(any())).thenAnswer(invocation -> {
            var order = invocation.getArgument(0, com.hoang.jwtjava.entity.Order.class);
            order.setId(11L);
            return order;
        });

        OrderCreateRequest request = new OrderCreateRequest();
        OrderItemRequest item = new OrderItemRequest();
        item.setProductId(2L);
        item.setQuantity(1);
        request.setItems(List.of(item));
        request.setReceiverName("A");
        request.setReceiverPhone("0901111111");
        request.setShippingAddress("Addr");

        OrderResponse response = orderService.createOrder("buyer@example.com", request);

        assertEquals(500_000, response.getSubtotal());
        assertEquals(0, response.getShippingFee());
        assertEquals(500_000, response.getTotalAmount());
    }
}
