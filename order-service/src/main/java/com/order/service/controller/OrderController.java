package com.order.service.controller;

import com.order.service.util.JwtClaimsExtractor;
import com.order.service.dto.OrderRequest;
import com.order.service.dto.OrderResponse;
import com.order.service.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;
    private final JwtClaimsExtractor claimsExtractor;

    public OrderController(OrderService orderService, JwtClaimsExtractor claimsExtractor) {
        this.orderService = orderService;
        this.claimsExtractor = claimsExtractor;
    }

    @GetMapping
    public ResponseEntity<Page<OrderResponse>> findMyOrders(
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(orderService.findByUserId(claimsExtractor.currentUserId(), pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.findById(id));
    }

    @PostMapping
    public ResponseEntity<OrderResponse> create(@Valid @RequestBody OrderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(orderService.create(request, claimsExtractor.currentUserId(), claimsExtractor.currentUserEmail()));
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<OrderResponse> cancel(@PathVariable Long id) {
        orderService.cancelOrder(id, "Cancelado pelo usuário id=" + claimsExtractor.currentUserId());
        return ResponseEntity.ok(orderService.findById(id));
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<OrderResponse>> findAll(
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(orderService.findAll(pageable));
    }
}
