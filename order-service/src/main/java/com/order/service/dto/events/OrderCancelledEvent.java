package com.order.service.dto.events;

import java.time.LocalDateTime;

public record OrderCancelledEvent(
        Long orderId,
        Long userId,
        String userEmail,
        String reason,
        LocalDateTime occurredAt
) {
    public OrderCancelledEvent(Long orderId, Long userId, String userEmail, String reason) {
        this(orderId, userId, userEmail, reason, LocalDateTime.now());
    }
}