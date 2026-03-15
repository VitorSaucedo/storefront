package com.notification.service.dto.events;

public record OrderCancelledEvent(
        Long orderId,
        Long userId,
        String reason
) {}
