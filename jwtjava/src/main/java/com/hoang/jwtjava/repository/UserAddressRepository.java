package com.hoang.jwtjava.repository;

import com.hoang.jwtjava.entity.UserAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserAddressRepository extends JpaRepository<UserAddress, Long> {

    List<UserAddress> findByUser_EmailOrderByIsDefaultDescIdAsc(String email);

    Optional<UserAddress> findByIdAndUser_Email(Long id, String email);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update UserAddress a set a.isDefault = false where a.user.id = :userId and a.isDefault = true")
    int clearDefaultForUser(@Param("userId") String userId);
}
