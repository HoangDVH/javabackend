package com.hoang.jwtjava.service;

import com.hoang.jwtjava.dto.request.PaymentCreateRequest;
import com.hoang.jwtjava.dto.response.PaymentResponse;
import com.hoang.jwtjava.entity.Order;
import com.hoang.jwtjava.entity.OrderStatus;
import com.hoang.jwtjava.entity.Payment;
import com.hoang.jwtjava.entity.PaymentStatus;
import com.hoang.jwtjava.entity.User;
import com.hoang.jwtjava.repository.OrderRepository;
import com.hoang.jwtjava.repository.PaymentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private OrderRealtimeNotifier orderRealtimeNotifier;

    @InjectMocks
    private PaymentService paymentService;

    @Test
    void createPaymentReturnsExistingSuccessIdempotently() {
        Order order = Order.builder()
                .id(1L)
                .user(User.builder().email("buyer@example.com").build())
                .totalAmount(100_000)
                .status(OrderStatus.PAID)
                .build();
        Payment existing = Payment.builder()
                .id(9L)
                .order(order)
                .method("CASH")
                .amount(100_000)
                .status(PaymentStatus.SUCCESS)
                .transactionRef("old-ref")
                .build();

        when(orderRepository.findByIdAndUserEmail(1L, "buyer@example.com")).thenReturn(Optional.of(order));
        when(paymentRepository.findByOrder_IdAndStatus(1L, PaymentStatus.SUCCESS))
                .thenReturn(List.of(existing));

        PaymentCreateRequest request = new PaymentCreateRequest();
        request.setOrderId(1L);
        request.setMethod("CASH");

        PaymentResponse response = paymentService.createPayment("buyer@example.com", request);

        assertEquals(9L, response.getId());
        assertEquals("old-ref", response.getTransactionRef());
        verify(paymentRepository, never()).save(any());
        verify(orderRealtimeNotifier, never()).publishToSellers(any(), any());
    }
}
