package com.hoang.jwtjava.repository;

import com.hoang.jwtjava.entity.Payment;
import com.hoang.jwtjava.entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findAllByOrderByCreatedAtDesc();
    List<Payment> findByOrderUserEmailOrderByCreatedAtDesc(String email);
    Optional<Payment> findByTransactionRef(String transactionRef);
    Optional<Payment> findTopByOrderIdAndMethodAndStatusInOrderByCreatedAtDesc(
            Long orderId, String method, PaymentStatus... statuses);
}
