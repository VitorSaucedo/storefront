package com.order.service.dto.events;

import com.order.service.dto.OrderItem;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

@Builder
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
}