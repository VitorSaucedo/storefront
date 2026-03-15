package com.notification.service.dto.events;

public record OrderConfirmedEvent(
        Long orderId,
        Long userId,
        Double totalAmount
) {}
