package com.hoang.jwtjava.repository;

import com.hoang.jwtjava.entity.ProductReview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ProductReviewRepository extends JpaRepository<ProductReview, Long> {

    Page<ProductReview> findByProduct_Id(Long productId, Pageable pageable);

    Optional<ProductReview> findByIdAndProduct_Id(Long id, Long productId);

    boolean existsByProduct_IdAndUser_EmailIgnoreCase(Long productId, String email);

    long countByProduct_Id(Long productId);

    @Query("select avg(r.rating) from ProductReview r where r.product.id = :productId")
    Double averageRatingByProductId(@Param("productId") Long productId);
}
