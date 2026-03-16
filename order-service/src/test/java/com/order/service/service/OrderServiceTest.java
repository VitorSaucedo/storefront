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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderEventPublisher eventPublisher;

    @Mock
    private CatalogClient catalogClient;

    @InjectMocks
    private OrderService orderService;

    private Order pendingOrder;

    @BeforeEach
    void setUp() {
        pendingOrder = new Order();
        pendingOrder.setId(1L);
        pendingOrder.setUserId(42L);
        pendingOrder.setUserEmail("user@example.com");
        pendingOrder.setStatus(OrderStatus.PENDING);
        pendingOrder.setTotalAmount(new BigDecimal("299.90"));
        pendingOrder.setCreatedAt(LocalDateTime.now());
        pendingOrder.setUpdatedAt(LocalDateTime.now());

        OrderItem item = new OrderItem(10L, 2, new BigDecimal("149.95"));
        pendingOrder.setItems(List.of(item));
    }

    // -------------------------------------------------------------------------
    // findByUserId
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("findByUserId")
    class FindByUserId {

        @Test
        @DisplayName("deve retornar página de pedidos do usuário")
        void shouldReturnOrderPageForUser() {
            PageRequest pageable = PageRequest.of(0, 10);
            Page<Order> orderPage = new PageImpl<>(List.of(pendingOrder), pageable, 1);
            when(orderRepository.findByUserId(42L, pageable)).thenReturn(orderPage);

            Page<OrderResponse> result = orderService.findByUserId(42L, pageable);

            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent().get(0).getId()).isEqualTo(1L);
            assertThat(result.getContent().get(0).getUserEmail()).isEqualTo("user@example.com");
        }

        @Test
        @DisplayName("deve retornar página vazia quando usuário não tem pedidos")
        void shouldReturnEmptyPageWhenNoOrders() {
            PageRequest pageable = PageRequest.of(0, 10);
            when(orderRepository.findByUserId(99L, pageable)).thenReturn(Page.empty());

            Page<OrderResponse> result = orderService.findByUserId(99L, pageable);

            assertThat(result).isEmpty();
        }
    }

    // -------------------------------------------------------------------------
    // findById
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("deve retornar o pedido quando encontrado")
        void shouldReturnOrderWhenFound() {
            when(orderRepository.findById(1L)).thenReturn(Optional.of(pendingOrder));

            OrderResponse result = orderService.findById(1L);

            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getStatus()).isEqualTo(OrderStatus.PENDING);
        }

        @Test
        @DisplayName("deve lançar OrderNotFoundException quando não encontrado")
        void shouldThrowWhenNotFound() {
            when(orderRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.findById(99L))
                    .isInstanceOf(OrderNotFoundException.class)
                    .hasMessageContaining("99");
        }
    }

    // -------------------------------------------------------------------------
    // findAll
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("findAll")
    class FindAll {

        @Test
        @DisplayName("deve retornar todos os pedidos paginados")
        void shouldReturnAllOrders() {
            PageRequest pageable = PageRequest.of(0, 10);
            Page<Order> page = new PageImpl<>(List.of(pendingOrder), pageable, 1);
            when(orderRepository.findAll(pageable)).thenReturn(page);

            Page<OrderResponse> result = orderService.findAll(pageable);

            assertThat(result.getTotalElements()).isEqualTo(1);
        }
    }

    // -------------------------------------------------------------------------
    // create
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("create")
    class Create {

        private OrderRequest buildRequest() {
            OrderItemRequest item = new OrderItemRequest();
            item.setProductId(10L);
            item.setQuantity(2);
            item.setUnitPrice(new BigDecimal("149.95"));

            OrderRequest request = new OrderRequest();
            request.setItems(List.of(item));
            return request;
        }

        @Test
        @DisplayName("deve criar pedido e publicar OrderCreatedEvent")
        void shouldCreateOrderAndPublishEvent() {
            CatalogClient.ProductStock stock = new CatalogClient.ProductStock(10L, "Teclado", 50);
            when(catalogClient.getProductStock(10L)).thenReturn(stock);
            when(orderRepository.save(any())).thenReturn(pendingOrder);

            OrderResponse result = orderService.create(buildRequest(), 42L, "user@example.com");

            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getStatus()).isEqualTo(OrderStatus.PENDING);

            ArgumentCaptor<OrderCreatedEvent> captor = ArgumentCaptor.forClass(OrderCreatedEvent.class);
            verify(eventPublisher).publishOrderCreated(captor.capture());
            OrderCreatedEvent event = captor.getValue();
            assertThat(event.orderId()).isEqualTo(1L);
            assertThat(event.userId()).isEqualTo(42L);
            assertThat(event.userEmail()).isEqualTo("user@example.com");
        }

        @Test
        @DisplayName("deve calcular o totalAmount corretamente")
        void shouldCalculateTotalAmount() {
            CatalogClient.ProductStock stock = new CatalogClient.ProductStock(10L, "Teclado", 50);
            when(catalogClient.getProductStock(10L)).thenReturn(stock);

            Order saved = new Order();
            saved.setId(2L);
            saved.setUserId(42L);
            saved.setUserEmail("user@example.com");
            saved.setStatus(OrderStatus.PENDING);
            saved.setTotalAmount(new BigDecimal("299.90")); // 149.95 * 2
            saved.setItems(pendingOrder.getItems());
            saved.setCreatedAt(LocalDateTime.now());
            saved.setUpdatedAt(LocalDateTime.now());
            when(orderRepository.save(any())).thenReturn(saved);

            OrderResponse result = orderService.create(buildRequest(), 42L, "user@example.com");

            assertThat(result.getTotalAmount()).isEqualByComparingTo(new BigDecimal("299.90"));
        }

        @Test
        @DisplayName("deve lançar RuntimeException quando produto não é encontrado no catálogo")
        void shouldThrowWhenProductNotFound() {
            when(catalogClient.getProductStock(10L)).thenReturn(null);

            assertThatThrownBy(() -> orderService.create(buildRequest(), 42L, "user@example.com"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Produto não encontrado");
        }

        @Test
        @DisplayName("deve lançar RuntimeException quando estoque é insuficiente")
        void shouldThrowWhenStockInsufficient() {
            // stock de 1, mas pediu 2
            CatalogClient.ProductStock stock = new CatalogClient.ProductStock(10L, "Teclado", 1);
            when(catalogClient.getProductStock(10L)).thenReturn(stock);

            assertThatThrownBy(() -> orderService.create(buildRequest(), 42L, "user@example.com"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Estoque insuficiente");
        }

        @Test
        @DisplayName("não deve publicar evento quando save falha")
        void shouldNotPublishEventWhenSaveFails() {
            CatalogClient.ProductStock stock = new CatalogClient.ProductStock(10L, "Teclado", 50);
            when(catalogClient.getProductStock(10L)).thenReturn(stock);
            when(orderRepository.save(any())).thenThrow(new RuntimeException("DB error"));

            assertThatThrownBy(() -> orderService.create(buildRequest(), 42L, "user@example.com"))
                    .isInstanceOf(RuntimeException.class);

            verify(eventPublisher, never()).publishOrderCreated(any());
        }
    }

    // -------------------------------------------------------------------------
    // confirmOrder
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("confirmOrder")
    class ConfirmOrder {

        @Test
        @DisplayName("deve confirmar pedido PENDING e publicar OrderConfirmedEvent")
        void shouldConfirmOrderAndPublishEvent() {
            when(orderRepository.findById(1L)).thenReturn(Optional.of(pendingOrder));
            when(orderRepository.save(any())).thenReturn(pendingOrder);

            orderService.confirmOrder(1L);

            assertThat(pendingOrder.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
            verify(orderRepository).save(pendingOrder);

            ArgumentCaptor<OrderConfirmedEvent> captor = ArgumentCaptor.forClass(OrderConfirmedEvent.class);
            verify(eventPublisher).publishOrderConfirmed(captor.capture());
            OrderConfirmedEvent event = captor.getValue();
            assertThat(event.orderId()).isEqualTo(1L);
            assertThat(event.userId()).isEqualTo(42L);
        }

        @Test
        @DisplayName("deve lançar OrderNotFoundException quando pedido não existe")
        void shouldThrowWhenOrderNotFound() {
            when(orderRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.confirmOrder(99L))
                    .isInstanceOf(OrderNotFoundException.class);

            verify(eventPublisher, never()).publishOrderConfirmed(any());
        }

        @Test
        @DisplayName("deve lançar InvalidOrderStatusException quando status não é PENDING")
        void shouldThrowWhenStatusIsNotPending() {
            pendingOrder.setStatus(OrderStatus.CONFIRMED);
            when(orderRepository.findById(1L)).thenReturn(Optional.of(pendingOrder));

            assertThatThrownBy(() -> orderService.confirmOrder(1L))
                    .isInstanceOf(InvalidOrderStatusException.class)
                    .hasMessageContaining("confirmar");

            verify(eventPublisher, never()).publishOrderConfirmed(any());
        }

        @Test
        @DisplayName("não deve confirmar pedido CANCELLED")
        void shouldThrowWhenOrderIsCancelled() {
            pendingOrder.setStatus(OrderStatus.CANCELLED);
            when(orderRepository.findById(1L)).thenReturn(Optional.of(pendingOrder));

            assertThatThrownBy(() -> orderService.confirmOrder(1L))
                    .isInstanceOf(InvalidOrderStatusException.class);
        }
    }

    // -------------------------------------------------------------------------
    // cancelOrder
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("cancelOrder")
    class CancelOrder {

        @Test
        @DisplayName("deve cancelar pedido PENDING e publicar OrderCancelledEvent")
        void shouldCancelOrderAndPublishEvent() {
            when(orderRepository.findById(1L)).thenReturn(Optional.of(pendingOrder));
            when(orderRepository.save(any())).thenReturn(pendingOrder);

            orderService.cancelOrder(1L, "Cancelado pelo usuário");

            assertThat(pendingOrder.getStatus()).isEqualTo(OrderStatus.CANCELLED);
            verify(orderRepository).save(pendingOrder);

            ArgumentCaptor<OrderCancelledEvent> captor = ArgumentCaptor.forClass(OrderCancelledEvent.class);
            verify(eventPublisher).publishOrderCancelled(captor.capture());
            OrderCancelledEvent event = captor.getValue();
            assertThat(event.orderId()).isEqualTo(1L);
            assertThat(event.userId()).isEqualTo(42L);
            assertThat(event.reason()).isEqualTo("Cancelado pelo usuário");
        }

        @Test
        @DisplayName("deve lançar OrderNotFoundException quando pedido não existe")
        void shouldThrowWhenOrderNotFound() {
            when(orderRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.cancelOrder(99L, "motivo"))
                    .isInstanceOf(OrderNotFoundException.class);

            verify(eventPublisher, never()).publishOrderCancelled(any());
        }

        @Test
        @DisplayName("deve lançar InvalidOrderStatusException quando status não é PENDING")
        void shouldThrowWhenStatusIsNotPending() {
            pendingOrder.setStatus(OrderStatus.CONFIRMED);
            when(orderRepository.findById(1L)).thenReturn(Optional.of(pendingOrder));

            assertThatThrownBy(() -> orderService.cancelOrder(1L, "motivo"))
                    .isInstanceOf(InvalidOrderStatusException.class)
                    .hasMessageContaining("cancelar");

            verify(eventPublisher, never()).publishOrderCancelled(any());
        }

        @Test
        @DisplayName("não deve cancelar pedido já CANCELLED")
        void shouldThrowWhenOrderIsAlreadyCancelled() {
            pendingOrder.setStatus(OrderStatus.CANCELLED);
            when(orderRepository.findById(1L)).thenReturn(Optional.of(pendingOrder));

            assertThatThrownBy(() -> orderService.cancelOrder(1L, "motivo"))
                    .isInstanceOf(InvalidOrderStatusException.class);
        }
    }
}