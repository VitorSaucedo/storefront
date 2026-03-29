package com.order.service.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.deser.jdk.NumberDeserializers;

import java.math.BigDecimal;

@Builder
public record OrderItemRequest(
    @NotNull(message = "ID do produto é obrigatório")
    Long productId,

    @NotNull(message = "Quantidade é obrigatória")
    @Min(value = 1, message = "Quantidade deve ser maior que zero")
    Integer quantity,

    @NotNull(message = "Preço unitário é obrigatório")
    @JsonDeserialize(using = NumberDeserializers.BigDecimalDeserializer.class)
    BigDecimal unitPrice
){}
