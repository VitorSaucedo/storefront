package com.order.service.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Builder
public record OrderRequest(

    @NotEmpty(message = "O pedido deve ter pelo menos um item")
    @Valid
    List<OrderItemRequest> items
){}