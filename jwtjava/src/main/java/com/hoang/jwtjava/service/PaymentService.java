package com.hoang.jwtjava.service;

import com.hoang.jwtjava.dto.request.PaymentCreateRequest;
import com.hoang.jwtjava.dto.response.PaymentResponse;
import com.hoang.jwtjava.entity.Order;
import com.hoang.jwtjava.entity.OrderStatus;
import com.hoang.jwtjava.entity.Payment;
import com.hoang.jwtjava.entity.PaymentStatus;
import com.hoang.jwtjava.exception.AppException;
import com.hoang.jwtjava.exception.ErrorCode;
import com.hoang.jwtjava.repository.OrderRepository;
import com.hoang.jwtjava.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final OrderRealtimeNotifier orderRealtimeNotifier;

    @Transactional
    public PaymentResponse createPayment(String userEmail, PaymentCreateRequest request) {
        if (request.getMethod() == null || request.getMethod().isBlank())
            throw new AppException(ErrorCode.PAYMENT_INVALID);

        Order order = orderRepository.findByIdAndUserEmail(request.getOrderId(), userEmail)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        String method = request.getMethod().trim().toUpperCase();
        if ("VNPAY".equals(method))
            throw new AppException(ErrorCode.PAYMENT_INVALID);

        // Idempotent: already paid → return existing SUCCESS payment (any method).
        var existingSuccess = paymentRepository.findByOrder_IdAndStatus(order.getId(), PaymentStatus.SUCCESS);
        if (!existingSuccess.isEmpty()) {
            if (order.getStatus() != OrderStatus.PAID) {
                order.setStatus(OrderStatus.PAID);
                orderRepository.save(order);
            }
            return toResponse(existingSuccess.get(0), null);
        }
        if (order.getStatus() == OrderStatus.PAID)
            throw new AppException(ErrorCode.PAYMENT_ALREADY_EXISTS);
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT)
            throw new AppException(ErrorCode.PAYMENT_INVALID);

        Payment payment = Payment.builder()
                .order(order)
                .method(method)
                .amount(order.getTotalAmount())
                .status(PaymentStatus.SUCCESS)
                .transactionRef(UUID.randomUUID().toString())
                .build();

        order.setStatus(OrderStatus.PAID);
        orderRepository.save(order);
        orderRealtimeNotifier.publishToSellers(order, OrderRealtimeNotifier.STATUS_CHANGED);

        return toResponse(paymentRepository.save(payment), null);
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> getMyPayments(String userEmail) {
        return paymentRepository.findByOrderUserEmailOrderByCreatedAtDesc(userEmail)
                .stream()
                .map(payment -> toResponse(payment, null))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> getSellerPayments(String sellerEmail) {
        return paymentRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(payment -> toResponse(payment, sellerEmail))
                .filter(payment -> payment.getAmount() > 0)
                .toList();
    }

    private PaymentResponse toResponse(Payment payment, String sellerEmailFilter) {
        int amount = payment.getAmount();
        if (sellerEmailFilter != null) {
            amount = payment.getOrder().getItems().stream()
                    .filter(item -> sellerEmailFilter.equalsIgnoreCase(item.getSellerEmail()))
                    .mapToInt(item -> item.getUnitPrice() * item.getQuantity())
                    .sum();
        }
        return PaymentResponse.builder()
                .id(payment.getId())
                .orderId(payment.getOrder().getId())
                .method(payment.getMethod())
                .amount(amount)
                .status(payment.getStatus().name())
                .transactionRef(payment.getTransactionRef())
                .createdAt(payment.getCreatedAt())
                .build();
    }
}
