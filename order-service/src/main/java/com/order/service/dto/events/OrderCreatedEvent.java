package com.order.service.dto.events;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderCreatedEvent(
        Long orderId,
        Long userId,
        String userEmail,
        List<OrderItem> items,
        BigDecimal totalAmount,
        LocalDateTime occurredAt
) {
    public OrderCreatedEvent(Long orderId, Long userId, String userEmail,
                             List<OrderItem> items, BigDecimal totalAmount) {
        this(orderId, userId, userEmail, items, totalAmount, LocalDateTime.now());
    }

    public record OrderItem(
            Long productId,
            Integer quantity,
            BigDecimal unitPrice
    ) {}
}