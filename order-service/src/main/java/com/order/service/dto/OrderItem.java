package com.order.service.dto;

import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record OrderItem(
        Long productId,
        Integer quantity,
        BigDecimal unitPrice
) {}
