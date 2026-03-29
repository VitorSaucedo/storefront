package com.notification.service.dto.events;

import lombok.Builder;

@Builder
public record OrderCancelledEvent(
        Long orderId,
        Long userId,
        String reason
) {}
