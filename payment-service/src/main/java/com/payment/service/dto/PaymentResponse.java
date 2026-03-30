package com.payment.service.dto;

import lombok.Builder;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.ToStringSerializer;

import java.math.BigDecimal;

@Builder
public record PaymentResponse(
        @JsonSerialize(using = ToStringSerializer.class) Long id,
        @JsonSerialize(using = ToStringSerializer.class) Long orderId,
        @JsonSerialize(using = ToStringSerializer.class) Long userId,
        @JsonSerialize(using = ToStringSerializer.class) BigDecimal amount,
        String status,
        String failureReason
) {}
