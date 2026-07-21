package com.hoang.jwtjava.repository;

import com.hoang.jwtjava.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {
    List<Product> findBySellerEmailIgnoreCaseOrderByCreatedAtDesc(String sellerEmail);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Product p set p.stock = p.stock - :qty where p.id = :id and p.stock >= :qty")
    int decrementStockIfAvailable(@Param("id") Long id, @Param("qty") int qty);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Product p set p.stock = p.stock + :qty where p.id = :id")
    int incrementStock(@Param("id") Long id, @Param("qty") int qty);
}
