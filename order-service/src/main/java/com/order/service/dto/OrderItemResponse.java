package com.order.service.dto;

import lombok.Builder;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.ToStringSerializer;

import java.math.BigDecimal;

@Builder
public record OrderItemResponse(
        @JsonSerialize(using = ToStringSerializer.class) Long productId,
        Integer quantity,
        @JsonSerialize(using = ToStringSerializer.class) BigDecimal unitPrice
) {}
