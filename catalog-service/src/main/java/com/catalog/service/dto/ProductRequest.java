package com.catalog.service.dto;

import jakarta.validation.constraints.*;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record ProductRequest(
    String imageUrl,

    @NotBlank(message = "Nome é obrigatório")
    String name,

    String description,

    @NotNull(message = "Preço é obrigatório")
    @DecimalMin(value = "0.01", message = "Preço deve ser maior que zero")
    @Digits(integer = 8, fraction = 2,
            message = "Preço inválido — máximo 8 dígitos inteiros e 2 decimais")
    BigDecimal price,

    @NotNull(message = "Quantidade em estoque é obrigatória")
    @Min(value = 0, message = "Estoque não pode ser negativo")
    @Max(value = 999999, message = "Estoque não pode exceder 999999")
    Integer stockQuantity,

    @NotBlank(message = "Categoria é obrigatória")
    String category
){}