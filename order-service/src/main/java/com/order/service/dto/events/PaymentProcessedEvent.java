package com.order.service.dto.events;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentProcessedEvent(
        Long orderId,
        Long userId,
        BigDecimal amount,
        LocalDateTime occurredAt
) {}