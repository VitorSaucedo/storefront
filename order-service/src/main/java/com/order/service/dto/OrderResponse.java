package com.order.service.dto;

import com.order.service.domain.OrderStatus;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.ToStringSerializer;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class OrderResponse {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;
    private String userEmail;
    private OrderStatus status;
    @JsonSerialize(using = ToStringSerializer.class)
    private BigDecimal totalAmount;
    private List<OrderItemResponse> items;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public OrderResponse(Long id, Long userId, String userEmail, OrderStatus status,
                         BigDecimal totalAmount, List<OrderItemResponse> items,
                         LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.userId = userId;
        this.userEmail = userEmail;
        this.status = status;
        this.totalAmount = totalAmount;
        this.items = items;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public record OrderItemResponse(
            @JsonSerialize(using = ToStringSerializer.class) Long productId,
            Integer quantity,
            @JsonSerialize(using = ToStringSerializer.class) BigDecimal unitPrice
    ) {}

    // Getters
    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getUserEmail() { return userEmail; }
    public OrderStatus getStatus() { return status; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public List<OrderItemResponse> getItems() { return items; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}