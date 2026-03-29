package com.order.service.dto;

import com.order.service.domain.OrderStatus;
import lombok.Builder;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.ToStringSerializer;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Builder
public record OrderResponse(
    @JsonSerialize(using = ToStringSerializer.class)
    Long id,
    @JsonSerialize(using = ToStringSerializer.class)
    Long userId,
    String userEmail,
    OrderStatus status,
    @JsonSerialize(using = ToStringSerializer.class)
    BigDecimal totalAmount,
    List<OrderItemResponse> items,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
){}
