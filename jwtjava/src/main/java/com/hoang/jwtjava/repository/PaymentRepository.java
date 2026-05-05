package com.hoang.jwtjava.repository;

import com.hoang.jwtjava.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findAllByOrderByCreatedAtDesc();
    List<Payment> findByOrderUserEmailOrderByCreatedAtDesc(String email);
}
