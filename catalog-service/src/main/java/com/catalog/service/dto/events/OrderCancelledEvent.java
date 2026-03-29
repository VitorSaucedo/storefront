package com.catalog.service.dto.events;

import com.catalog.service.dto.OrderItem;

import java.time.LocalDateTime;
import java.util.List;

public record OrderCancelledEvent(
        Long orderId,
        Long userId,
        String userEmail,
        String reason,
        List<OrderItem> items,
        LocalDateTime occurredAt
) {}
