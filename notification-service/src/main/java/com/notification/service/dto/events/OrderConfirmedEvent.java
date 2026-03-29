package com.notification.service.dto.events;

import lombok.Builder;

@Builder
public record OrderConfirmedEvent(
        Long orderId,
        Long userId,
        Double totalAmount
) {}
