package com.hoang.jwtjava.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Entity
@Table(name = "order_status_history", indexes = {
        @Index(name = "idx_order_status_history_order_changed", columnList = "order_id, changed_at")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OrderStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "order_id", nullable = false)
    Long orderId;

    @Column(name = "old_status", length = 32)
    String oldStatus;

    @Column(name = "new_status", nullable = false, length = 32)
    String newStatus;

    @Column(name = "changed_at", nullable = false)
    LocalDateTime changedAt;

    @Column(name = "changed_by", length = 255)
    String changedBy;
}
