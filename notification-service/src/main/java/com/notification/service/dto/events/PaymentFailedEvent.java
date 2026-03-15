package com.notification.service.dto.events;

public record PaymentFailedEvent(
        Long orderId,
        Long userId,
        String reason
) {}
