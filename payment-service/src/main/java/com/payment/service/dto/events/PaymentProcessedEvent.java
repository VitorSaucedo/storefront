package com.payment.service.dto.events;

import java.math.BigDecimal;

public record PaymentProcessedEvent(
        Long orderId,
        Long userId,
        Long paymentId,
        BigDecimal amount
) {}
