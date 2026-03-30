package com.payment.service.dto.events;

import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record PaymentProcessedEvent(
        Long orderId,
        Long userId,
        Long paymentId,
        BigDecimal amount
) {}
