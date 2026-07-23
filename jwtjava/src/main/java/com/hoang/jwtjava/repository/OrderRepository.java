package com.hoang.jwtjava.repository;

import com.hoang.jwtjava.entity.Order;
import com.hoang.jwtjava.entity.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findAllByOrderByCreatedAtDesc();
    List<Order> findByUserEmailOrderByCreatedAtDesc(String email);
    Optional<Order> findByIdAndUserEmail(Long id, String email);

    @Query("""
            select case when count(o) > 0 then true else false end
            from Order o join o.items i
            where lower(o.user.email) = lower(:email)
              and o.status = :status
              and i.productId = :productId
            """)
    boolean existsByUserEmailAndStatusAndProductId(
            @Param("email") String email,
            @Param("status") OrderStatus status,
            @Param("productId") Long productId);
}
