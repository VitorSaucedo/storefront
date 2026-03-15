package com.order.service.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public class OrderRequest {

    @NotEmpty(message = "O pedido deve ter pelo menos um item")
    @Valid
    private List<OrderItemRequest> items;

    // Getters e Setters
    public List<OrderItemRequest> getItems() { return items; }
    public void setItems(List<OrderItemRequest> items) { this.items = items; }
}