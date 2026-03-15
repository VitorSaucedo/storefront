package com.catalog.service.dto.events;

import java.time.LocalDateTime;
import java.util.List;

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
