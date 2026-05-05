package com.hoang.jwtjava.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;

@Entity
@Table(name = "invalidated_tokens")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class InvalidatedToken {

    @Id
    @Column(length = 64)
    String id;

    @Column(nullable = false, name = "expires_at")
    Instant expiresAt;

    @Column(nullable = false, name = "token_kind", length = 16)
    String tokenKind;
}
