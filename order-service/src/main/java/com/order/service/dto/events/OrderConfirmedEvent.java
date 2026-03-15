package com.order.service.dto.events;

import java.time.LocalDateTime;
import java.util.List;

public record OrderConfirmedEvent(
        Long orderId,
        Long userId,
        String userEmail,
        List<OrderItem> items,
        LocalDateTime occurredAt
) {
    public OrderConfirmedEvent(Long orderId, Long userId, String userEmail, List<OrderItem> items) {
        this(orderId, userId, userEmail, items, LocalDateTime.now());
    }

    public record OrderItem(
            Long productId,
            Integer quantity
    ) {}
}