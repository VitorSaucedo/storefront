package com.catalog.service.dto.events;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
public record ProductUpdatedEvent(
        Long productId,
        String name,
        BigDecimal price,
        Integer stockQuantity,
        LocalDateTime occurredAt
) {
    public ProductUpdatedEvent(Long productId, String name, BigDecimal price, Integer stockQuantity) {
        this(productId, name, price, stockQuantity, LocalDateTime.now());
    }
}
