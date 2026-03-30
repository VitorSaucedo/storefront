package com.payment.service.controller;

import com.payment.service.dto.PaymentResponse;
import com.payment.service.service.PaymentService;
import com.payment.service.util.JwtClaimsExtractor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    private final PaymentService paymentService;
    private final JwtClaimsExtractor claimsExtractor;

    public PaymentController(PaymentService paymentService, JwtClaimsExtractor claimsExtractor) {
        this.paymentService = paymentService;
        this.claimsExtractor = claimsExtractor;
    }

    @GetMapping("/user")
    public ResponseEntity<Page<PaymentResponse>> getMyPayments(
            @PageableDefault(sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(paymentService.getPaymentsByUser(claimsExtractor.currentUserId(), pageable));
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<PaymentResponse>> getPaymentsByUser(
            @PathVariable Long userId,
            @PageableDefault(sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(paymentService.getPaymentsByUser(userId, pageable));
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<PaymentResponse> getPaymentByOrder(@PathVariable Long orderId) {
        return ResponseEntity.ok(paymentService.getPaymentByOrder(orderId));
    }
}