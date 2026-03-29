package com.catalog.service.dto;

public record OrderItem(
        Long productId,
        Integer quantity
) {}
