package com.catalog.service.dto;

import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.ToStringSerializer;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ProductResponse {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    private String imageUrl;
    private String name;
    private String description;
    @JsonSerialize(using = ToStringSerializer.class)
    private BigDecimal price;
    private Integer stockQuantity;
    private String category;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public ProductResponse(Long id, String name, String description, BigDecimal price,
                           Integer stockQuantity, String category, String imageUrl,
                           LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
        this.stockQuantity = stockQuantity;
        this.category = category;
        this.imageUrl = imageUrl;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Getters
    public Long getId() { return id; }
    public String getImageUrl() { return imageUrl; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public BigDecimal getPrice() { return price; }
    public Integer getStockQuantity() { return stockQuantity; }
    public String getCategory() { return category; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
