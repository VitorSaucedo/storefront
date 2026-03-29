package com.catalog.service.dto.events;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

@Builder
public record OrderConfirmedEvent(
        Long orderId,
        List<OrderItem> items,
        LocalDateTime occurredAt
) {
    public record OrderItem(
            Long productId,
            Integer quantity
    ) {}
}
