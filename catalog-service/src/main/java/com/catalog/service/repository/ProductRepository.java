package com.catalog.service.repository;

import com.catalog.service.domain.Product;
import com.catalog.service.dto.ProductResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    Page<Product> findByCategory(String category, Pageable pageable);

    Page<Product> findByStockQuantityGreaterThan(Integer quantity, Pageable pageable);

    boolean existsByNameAndCategory(String name, String category);
}
