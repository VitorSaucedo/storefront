package com.payment.service.dto.events;

import lombok.Builder;

@Builder
public record PaymentFailedEvent(
        Long orderId,
        Long userId,
        String reason
) {}
