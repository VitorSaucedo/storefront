package com.order.service.client;

import com.order.service.dto.OrderItemRequest;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
public class CatalogClient {

    private final RestClient restClient;

    public CatalogClient(@Value("${catalog.service.url}") String catalogUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(catalogUrl)
                .build();
    }

    @CircuitBreaker(name = "catalogService")
    public List<ProductStock> getProductsStock(List<Long> productIds) {
        return restClient.post()
                .uri("/products/bulk-stock")
                .contentType(MediaType.APPLICATION_JSON)
                .body(productIds)
                .retrieve()
                .body(new ParameterizedTypeReference<List<ProductStock>>() {});
    }

    @CircuitBreaker(name = "catalogService")
    public void reserveStock(List<OrderItemRequest> items) {
        restClient.post()
                .uri("/products/reserve")
                .contentType(MediaType.APPLICATION_JSON)
                .body(items)
                .retrieve()
                .toBodilessEntity();
    }

    public record ProductStock(
            Long id,
            String name,
            Integer stockQuantity
    ) {}
}
