package com.order.service.service;

import com.order.service.client.CatalogClient;
import com.order.service.domain.Order;
import com.order.service.domain.OrderItem;
import com.order.service.domain.OrderStatus;
import com.order.service.dto.OrderItemRequest;
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
import java.util.List;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final OrderEventPublisher eventPublisher;
    private final CatalogClient catalogClient;

    public OrderService(OrderRepository orderRepository,
                        OrderEventPublisher eventPublisher,
                        CatalogClient catalogClient) {
        this.orderRepository = orderRepository;
        this.eventPublisher = eventPublisher;
        this.catalogClient = catalogClient;
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

        for (OrderItemRequest item : request.getItems()) {
            CatalogClient.ProductStock stock = catalogClient.getProductStock(item.getProductId());
            if (stock == null) {
                throw new RuntimeException("Produto não encontrado: id=" + item.getProductId());
            }
            if (stock.stockQuantity() < item.getQuantity()) {
                throw new RuntimeException(
                        "Estoque insuficiente para o produto '" + stock.name() + "'. " +
                                "Disponível: " + stock.stockQuantity() + ", Solicitado: " + item.getQuantity()
                );
            }
        }

        Order order = new Order();
        order.setUserId(userId);
        order.setUserEmail(userEmail);

        List<OrderItem> items = request.getItems().stream()
                .map(i -> new OrderItem(i.getProductId(), i.getQuantity(), i.getUnitPrice()))
                .toList();

        BigDecimal total = request.getItems().stream()
                .map(i -> i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        order.setItems(items);
        order.setTotalAmount(total);

        Order saved = orderRepository.save(order);
        log.info("Pedido criado: id={}, userId={}", saved.getId(), userId);

        List<OrderCreatedEvent.OrderItem> eventItems = items.stream()
                .map(i -> new OrderCreatedEvent.OrderItem(i.getProductId(), i.getQuantity(), i.getUnitPrice()))
                .toList();

        eventPublisher.publishOrderCreated(new OrderCreatedEvent(
                saved.getId(), userId, userEmail, eventItems, total
        ));

        return toResponse(saved);
    }

    @Transactional
    public void confirmOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new InvalidOrderStatusException(orderId, order.getStatus().name(), "confirmar");
        }

        order.setStatus(OrderStatus.CONFIRMED);
        orderRepository.save(order);
        log.info("Pedido confirmado: id={}", orderId);

        List<OrderConfirmedEvent.OrderItem> eventItems = order.getItems().stream()
                .map(i -> new OrderConfirmedEvent.OrderItem(i.getProductId(), i.getQuantity()))
                .toList();

        eventPublisher.publishOrderConfirmed(new OrderConfirmedEvent(
                order.getId(), order.getUserId(), order.getUserEmail(), eventItems
        ));
    }

    @Transactional
    public void cancelOrder(Long orderId, String reason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new InvalidOrderStatusException(orderId, order.getStatus().name(), "cancelar");
        }

        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);
        log.info("Pedido cancelado: id={}, reason={}", orderId, reason);

        eventPublisher.publishOrderCancelled(new OrderCancelledEvent(
                order.getId(), order.getUserId(), order.getUserEmail(), reason
        ));
    }

    private OrderResponse toResponse(Order order) {
        List<OrderResponse.OrderItemResponse> itemResponses = order.getItems().stream()
                .map(i -> new OrderResponse.OrderItemResponse(
                        i.getProductId(), i.getQuantity(), i.getUnitPrice()))
                .toList();

        return new OrderResponse(
                order.getId(),
                order.getUserId(),
                order.getUserEmail(),
                order.getStatus(),
                order.getTotalAmount(),
                itemResponses,
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }
}
