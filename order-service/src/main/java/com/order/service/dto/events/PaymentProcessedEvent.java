package com.order.service.dto.events;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
public record PaymentProcessedEvent(
        Long orderId,
        Long userId,
        BigDecimal amount,
        LocalDateTime occurredAt
) {}