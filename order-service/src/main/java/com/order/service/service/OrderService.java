package com.order.service.service;

import com.order.service.client.CatalogClient;
import com.order.service.config.MessagingConstants;
import com.order.service.domain.Order;
import com.order.service.domain.OrderItem;
import com.order.service.domain.OrderStatus;
import com.order.service.dto.OrderItemResponse;
import com.order.service.dto.OrderRequest;
import com.order.service.dto.OrderResponse;
import com.order.service.dto.events.OrderCancelledEvent;
import com.order.service.dto.events.OrderConfirmedEvent;
import com.order.service.dto.events.OrderCreatedEvent;
import com.order.service.exception.InvalidOrderStatusException;
import com.order.service.exception.OrderNotFoundException;
import com.order.service.messaging.OrderEventPublisher;
import com.order.service.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final OrderEventPublisher eventPublisher;
    private final CatalogClient catalogClient;
    private final OutboxService outboxService;

    public OrderService(OrderRepository orderRepository,
                        OrderEventPublisher eventPublisher,
                        CatalogClient catalogClient, OutboxService outboxService) {
        this.orderRepository = orderRepository;
        this.eventPublisher = eventPublisher;
        this.catalogClient = catalogClient;
        this.outboxService = outboxService;
    }

    public Page<OrderResponse> findByUserId(Long userId, Pageable pageable) {
        return orderRepository.findByUserId(userId, pageable).map(this::toResponse);
    }

    public OrderResponse findById(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));
        return toResponse(order);
    }

    public Page<OrderResponse> findAll(Pageable pageable) {
        return orderRepository.findAll(pageable).map(this::toResponse);
    }

    @Transactional
    public OrderResponse create(OrderRequest request, Long userId, String userEmail) {
        catalogClient.reserveStock(request.items());

        BigDecimal total = request.items().stream()
                .map(i -> i.unitPrice().multiply(BigDecimal.valueOf(i.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<OrderItem> domainItems = request.items().stream()
                .map(i -> new OrderItem(i.productId(), i.quantity(), i.unitPrice()))
                .toList();

        Order order = new Order();
        order.setUserId(userId);
        order.setUserEmail(userEmail);
        order.setItems(domainItems);
        order.setTotalAmount(total);

        Order saved = orderRepository.save(order);
        log.info("Pedido criado e estoque reservado: id={}, userId={}", saved.getId(), userId);

        List<com.order.service.dto.OrderItem> eventItems = domainItems.stream()
                .map(i -> com.order.service.dto.OrderItem.builder()
                        .productId(i.getProductId())
                        .quantity(i.getQuantity())
                        .unitPrice(i.getUnitPrice())
                        .build())
                .toList();

        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .orderId(saved.getId())
                .userId(userId)
                .userEmail(userEmail)
                .items(eventItems)
                .totalAmount(total)
                .occurredAt(LocalDateTime.now())
                .build();

        outboxService.saveEvent(
                event,
                saved.getId().toString(),
                "ORDER_CREATED",
                MessagingConstants.ORDER_CREATED_ROUTING_KEY
        );

        return toResponse(saved);
    }

    @Transactional
    public void confirmOrder(Long orderId) {
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new InvalidOrderStatusException(orderId, order.getStatus().name(), "confirmar");
        }

        order.setStatus(OrderStatus.CONFIRMED);
        orderRepository.save(order);
        log.info("Pedido confirmado: id={}", orderId);

        List<com.order.service.dto.OrderItem> eventItems = order.getItems().stream()
                .map(i -> com.order.service.dto.OrderItem.builder()
                        .productId(i.getProductId())
                        .quantity(i.getQuantity())
                        .build())
                .toList();

        OrderConfirmedEvent event = OrderConfirmedEvent.builder()
                .orderId(order.getId())
                .userId(order.getUserId())
                .userEmail(order.getUserEmail())
                .items(eventItems)
                .occurredAt(LocalDateTime.now())
                .build();

        outboxService.saveEvent(
                event,
                order.getId().toString(),
                "ORDER_CONFIRMED",
                MessagingConstants.ORDER_CONFIRMED_ROUTING_KEY
        );
    }

    @Transactional
    public void cancelOrder(Long orderId, String reason) {
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new InvalidOrderStatusException(orderId, order.getStatus().name(), "cancelar");
        }

        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);
        log.info("Pedido cancelado: id={}, reason={}", orderId, reason);

        List<com.order.service.dto.OrderItem> eventItems = order.getItems().stream()
                .map(i -> com.order.service.dto.OrderItem.builder()
                        .productId(i.getProductId())
                        .quantity(i.getQuantity())
                        .build())
                .toList();

        OrderCancelledEvent event = OrderCancelledEvent.builder()
                .orderId(order.getId())
                .userId(order.getUserId())
                .userEmail(order.getUserEmail())
                .reason(reason)
                .items(eventItems)
                .occurredAt(LocalDateTime.now())
                .build();

        outboxService.saveEvent(
                event,
                order.getId().toString(),
                "ORDER_CANCELLED",
                MessagingConstants.ORDER_CANCELLED_ROUTING_KEY
        );
    }

    private OrderResponse toResponse(Order order) {
        List<OrderItemResponse> itemResponses = order.getItems().stream()
                .map(i -> OrderItemResponse.builder()
                        .productId(i.getProductId())
                        .quantity(i.getQuantity())
                        .unitPrice(i.getUnitPrice())
                        .build())
                .toList();

        return OrderResponse.builder()
                .id(order.getId())
                .userId(order.getUserId())
                .userEmail(order.getUserEmail())
                .status(order.getStatus())
                .totalAmount(order.getTotalAmount())
                .items(itemResponses)
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }
}