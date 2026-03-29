package com.catalog.service.dto;

import lombok.Builder;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.ToStringSerializer;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
public record ProductResponse(
    @JsonSerialize(using = ToStringSerializer.class)
    Long id,
    String imageUrl,
    String name,
    String description,
    @JsonSerialize(using = ToStringSerializer.class)
    BigDecimal price,
    Integer stockQuantity,
    String category,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
){}
