package com.order.service.service;

import com.order.service.client.CatalogClient;
import com.order.service.config.MessagingConstants;
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
import com.order.service.repository.OrderRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
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
    private OutboxService outboxService;

    @Mock
    private CatalogClient catalogClient;

    @InjectMocks
    private OrderService orderService;

    // -------------------------------------------------------------------------
    // Fixtures
    // -------------------------------------------------------------------------

    private static final Long   ORDER_ID   = 1L;
    private static final Long   USER_ID    = 42L;
    private static final String USER_EMAIL = "user@example.com";

    private static final OrderItem ITEM = new OrderItem(10L, 2, new BigDecimal("149.95"));

    private static Order pendingOrder() {
        return Order.builder()
                .id(ORDER_ID)
                .userId(USER_ID)
                .userEmail(USER_EMAIL)
                .status(OrderStatus.PENDING)
                .totalAmount(new BigDecimal("299.90"))
                .items(List.of(ITEM))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private static OrderRequest buildRequest() {
        OrderItemRequest item = OrderItemRequest.builder()
                .productId(10L)
                .quantity(2)
                .unitPrice(new BigDecimal("149.95"))
                .build();

        return OrderRequest.builder()
                .items(List.of(item))
                .build();
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
            Order order = pendingOrder();
            Page<Order> orderPage = new PageImpl<>(List.of(order), pageable, 1);
            when(orderRepository.findByUserId(USER_ID, pageable)).thenReturn(orderPage);

            Page<OrderResponse> result = orderService.findByUserId(USER_ID, pageable);

            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent().get(0).id()).isEqualTo(ORDER_ID);
            assertThat(result.getContent().get(0).userEmail()).isEqualTo(USER_EMAIL);
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
            Order order = pendingOrder();
            when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));

            OrderResponse result = orderService.findById(ORDER_ID);

            assertThat(result.id()).isEqualTo(ORDER_ID);
            assertThat(result.status()).isEqualTo(OrderStatus.PENDING);
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
            Page<Order> page = new PageImpl<>(List.of(pendingOrder()), pageable, 1);
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

        @Test
        @DisplayName("deve criar pedido e salvar evento no outbox")
        void shouldCreateOrderAndSaveOutboxEvent() {
            Order saved = pendingOrder();
            when(orderRepository.save(any())).thenReturn(saved);

            OrderResponse result = orderService.create(buildRequest(), USER_ID, USER_EMAIL);

            assertThat(result.id()).isEqualTo(ORDER_ID);
            assertThat(result.status()).isEqualTo(OrderStatus.PENDING);

            ArgumentCaptor<OrderCreatedEvent> captor = ArgumentCaptor.forClass(OrderCreatedEvent.class);
            verify(outboxService).saveEvent(
                    captor.capture(),
                    eq(ORDER_ID.toString()),
                    eq("ORDER_CREATED"),
                    eq(MessagingConstants.ORDER_CREATED_ROUTING_KEY)
            );

            OrderCreatedEvent event = captor.getValue();
            assertThat(event.orderId()).isEqualTo(ORDER_ID);
            assertThat(event.userId()).isEqualTo(USER_ID);
            assertThat(event.userEmail()).isEqualTo(USER_EMAIL);
        }

        @Test
        @DisplayName("deve calcular o totalAmount corretamente")
        void shouldCalculateTotalAmount() {
            // 149.95 * 2 = 299.90
            Order saved = Order.builder()
                    .id(2L)
                    .userId(USER_ID)
                    .userEmail(USER_EMAIL)
                    .status(OrderStatus.PENDING)
                    .totalAmount(new BigDecimal("299.90"))
                    .items(List.of(ITEM))
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            when(orderRepository.save(any())).thenReturn(saved);

            OrderResponse result = orderService.create(buildRequest(), USER_ID, USER_EMAIL);

            assertThat(result.totalAmount()).isEqualByComparingTo(new BigDecimal("299.90"));
        }

        @Test
        @DisplayName("deve reservar estoque antes de salvar o pedido")
        void shouldReserveStockBeforeSaving() {
            when(orderRepository.save(any())).thenReturn(pendingOrder());

            OrderRequest request = buildRequest();
            orderService.create(request, USER_ID, USER_EMAIL);

            InOrder inOrder = inOrder(catalogClient, orderRepository);
            inOrder.verify(catalogClient).reserveStock(request.items());
            inOrder.verify(orderRepository).save(any());
        }

        @Test
        @DisplayName("não deve salvar pedido nem evento quando reserva de estoque falha")
        void shouldNotSaveOrderWhenStockReservationFails() {
            doThrow(new RuntimeException("Estoque insuficiente"))
                    .when(catalogClient).reserveStock(any());

            assertThatThrownBy(() -> orderService.create(buildRequest(), USER_ID, USER_EMAIL))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Estoque insuficiente");

            verify(orderRepository, never()).save(any());
            verify(outboxService, never()).saveEvent(any(), any(), any(), any());
        }

        @Test
        @DisplayName("não deve publicar evento quando save falha")
        void shouldNotPublishEventWhenSaveFails() {
            when(orderRepository.save(any())).thenThrow(new RuntimeException("DB error"));

            assertThatThrownBy(() -> orderService.create(buildRequest(), USER_ID, USER_EMAIL))
                    .isInstanceOf(RuntimeException.class);

            verify(outboxService, never()).saveEvent(any(), any(), any(), any());
        }
    }

    // -------------------------------------------------------------------------
    // confirmOrder
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("confirmOrder")
    class ConfirmOrder {

        @Test
        @DisplayName("deve confirmar pedido PENDING e salvar evento no outbox")
        void shouldConfirmOrderAndSaveOutboxEvent() {
            Order order = pendingOrder();
            when(orderRepository.findByIdForUpdate(ORDER_ID)).thenReturn(Optional.of(order));
            when(orderRepository.save(any())).thenReturn(order);

            orderService.confirmOrder(ORDER_ID);

            assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
            verify(orderRepository).save(order);

            ArgumentCaptor<OrderConfirmedEvent> captor = ArgumentCaptor.forClass(OrderConfirmedEvent.class);
            verify(outboxService).saveEvent(
                    captor.capture(),
                    eq(ORDER_ID.toString()),
                    eq("ORDER_CONFIRMED"),
                    eq(MessagingConstants.ORDER_CONFIRMED_ROUTING_KEY)
            );

            OrderConfirmedEvent event = captor.getValue();
            assertThat(event.orderId()).isEqualTo(ORDER_ID);
            assertThat(event.userId()).isEqualTo(USER_ID);
        }

        @Test
        @DisplayName("deve lançar OrderNotFoundException quando pedido não existe")
        void shouldThrowWhenOrderNotFound() {
            when(orderRepository.findByIdForUpdate(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.confirmOrder(99L))
                    .isInstanceOf(OrderNotFoundException.class);

            verify(outboxService, never()).saveEvent(any(), any(), any(), any());
        }

        @Test
        @DisplayName("deve lançar InvalidOrderStatusException quando status não é PENDING")
        void shouldThrowWhenStatusIsNotPending() {
            Order confirmed = Order.builder()
                    .id(ORDER_ID)
                    .userId(USER_ID)
                    .userEmail(USER_EMAIL)
                    .status(OrderStatus.CONFIRMED)
                    .totalAmount(new BigDecimal("299.90"))
                    .items(List.of(ITEM))
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            when(orderRepository.findByIdForUpdate(ORDER_ID)).thenReturn(Optional.of(confirmed));

            assertThatThrownBy(() -> orderService.confirmOrder(ORDER_ID))
                    .isInstanceOf(InvalidOrderStatusException.class)
                    .hasMessageContaining("confirmar");

            verify(outboxService, never()).saveEvent(any(), any(), any(), any());
        }

        @Test
        @DisplayName("não deve confirmar pedido CANCELLED")
        void shouldThrowWhenOrderIsCancelled() {
            Order cancelled = Order.builder()
                    .id(ORDER_ID)
                    .userId(USER_ID)
                    .userEmail(USER_EMAIL)
                    .status(OrderStatus.CANCELLED)
                    .totalAmount(new BigDecimal("299.90"))
                    .items(List.of(ITEM))
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            when(orderRepository.findByIdForUpdate(ORDER_ID)).thenReturn(Optional.of(cancelled));

            assertThatThrownBy(() -> orderService.confirmOrder(ORDER_ID))
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
        @DisplayName("deve cancelar pedido PENDING e salvar evento no outbox")
        void shouldCancelOrderAndSaveOutboxEvent() {
            Order order = pendingOrder();
            when(orderRepository.findByIdForUpdate(ORDER_ID)).thenReturn(Optional.of(order));
            when(orderRepository.save(any())).thenReturn(order);

            orderService.cancelOrder(ORDER_ID, "Cancelado pelo usuário");

            assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
            verify(orderRepository).save(order);

            ArgumentCaptor<OrderCancelledEvent> captor = ArgumentCaptor.forClass(OrderCancelledEvent.class);
            verify(outboxService).saveEvent(
                    captor.capture(),
                    eq(ORDER_ID.toString()),
                    eq("ORDER_CANCELLED"),
                    eq(MessagingConstants.ORDER_CANCELLED_ROUTING_KEY)
            );

            OrderCancelledEvent event = captor.getValue();
            assertThat(event.orderId()).isEqualTo(ORDER_ID);
            assertThat(event.userId()).isEqualTo(USER_ID);
            assertThat(event.reason()).isEqualTo("Cancelado pelo usuário");
        }

        @Test
        @DisplayName("deve lançar OrderNotFoundException quando pedido não existe")
        void shouldThrowWhenOrderNotFound() {
            when(orderRepository.findByIdForUpdate(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.cancelOrder(99L, "motivo"))
                    .isInstanceOf(OrderNotFoundException.class);

            verify(outboxService, never()).saveEvent(any(), any(), any(), any());
        }

        @Test
        @DisplayName("deve lançar InvalidOrderStatusException quando status não é PENDING")
        void shouldThrowWhenStatusIsNotPending() {
            Order confirmed = Order.builder()
                    .id(ORDER_ID)
                    .userId(USER_ID)
                    .userEmail(USER_EMAIL)
                    .status(OrderStatus.CONFIRMED)
                    .totalAmount(new BigDecimal("299.90"))
                    .items(List.of(ITEM))
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            when(orderRepository.findByIdForUpdate(ORDER_ID)).thenReturn(Optional.of(confirmed));

            assertThatThrownBy(() -> orderService.cancelOrder(ORDER_ID, "motivo"))
                    .isInstanceOf(InvalidOrderStatusException.class)
                    .hasMessageContaining("cancelar");

            verify(outboxService, never()).saveEvent(any(), any(), any(), any());
        }

        @Test
        @DisplayName("não deve cancelar pedido já CANCELLED")
        void shouldThrowWhenOrderIsAlreadyCancelled() {
            Order cancelled = Order.builder()
                    .id(ORDER_ID)
                    .userId(USER_ID)
                    .userEmail(USER_EMAIL)
                    .status(OrderStatus.CANCELLED)
                    .totalAmount(new BigDecimal("299.90"))
                    .items(List.of(ITEM))
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            when(orderRepository.findByIdForUpdate(ORDER_ID)).thenReturn(Optional.of(cancelled));

            assertThatThrownBy(() -> orderService.cancelOrder(ORDER_ID, "motivo"))
                    .isInstanceOf(InvalidOrderStatusException.class);
        }
    }
}