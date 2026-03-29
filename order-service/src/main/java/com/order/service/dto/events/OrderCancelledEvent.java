package com.order.service.dto.events;

import com.order.service.dto.OrderItem;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

@Builder
public record OrderCancelledEvent(
        Long orderId,
        Long userId,
        String userEmail,
        String reason,
        List<OrderItem> items,
        LocalDateTime occurredAt
) {
    public OrderCancelledEvent(Long orderId, Long userId, String userEmail, String reason, List<OrderItem> items) {
        this(orderId, userId, userEmail, reason, items, LocalDateTime.now());
    }
}