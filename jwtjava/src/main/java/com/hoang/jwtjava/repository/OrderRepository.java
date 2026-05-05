package com.hoang.jwtjava.repository;

import com.hoang.jwtjava.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findAllByOrderByCreatedAtDesc();
    List<Order> findByUserEmailOrderByCreatedAtDesc(String email);
    Optional<Order> findByIdAndUserEmail(Long id, String email);
}
