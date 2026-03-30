package com.payment.service.dto.events;

import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record OrderCreatedEvent(
        Long orderId,
        Long userId,
        BigDecimal totalAmount
) {}
