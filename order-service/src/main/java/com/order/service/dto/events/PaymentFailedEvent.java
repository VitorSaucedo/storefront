package com.order.service.dto.events;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentFailedEvent(
        Long orderId,
        Long userId,
        BigDecimal amount,
        String reason,
        LocalDateTime occurredAt
) {}