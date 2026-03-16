package com.order.service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.order.service.config.TestSecurityConfig;
import com.order.service.dto.OrderItemRequest;
import com.order.service.dto.OrderRequest;
import com.order.service.dto.OrderResponse;
import com.order.service.exception.OrderNotFoundException;
import com.order.service.service.OrderService;
import com.order.service.domain.OrderStatus;
import com.order.service.util.JwtClaimsExtractor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
@Import(TestSecurityConfig.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private OrderService orderService;

    @MockitoBean
    private JwtClaimsExtractor claimsExtractor;

    private OrderResponse orderResponse;

    @BeforeEach
    void setUp() {
        objectMapper.findAndRegisterModules();

        orderResponse = new OrderResponse(
                1L,
                42L,
                "user@example.com",
                OrderStatus.PENDING,
                new BigDecimal("299.90"),
                List.of(new OrderResponse.OrderItemResponse(10L, 2, new BigDecimal("149.95"))),
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(claimsExtractor.currentUserId()).thenReturn(42L);
        when(claimsExtractor.currentUserEmail()).thenReturn("user@example.com");
    }

    // -------------------------------------------------------------------------
    // GET /orders  (findMyOrders)
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("GET /orders")
    class FindMyOrders {

        @Test
        @WithMockUser
        @DisplayName("deve retornar 200 com a página de pedidos do usuário autenticado")
        void shouldReturn200WithUserOrders() throws Exception {
            Page<OrderResponse> page = new PageImpl<>(List.of(orderResponse), PageRequest.of(0, 10), 1);
            when(orderService.findByUserId(eq(42L), any())).thenReturn(page);

            mockMvc.perform(get("/orders"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].id").value("1"))
                    .andExpect(jsonPath("$.content[0].userEmail").value("user@example.com"))
                    .andExpect(jsonPath("$.content[0].status").value("PENDING"))
                    .andExpect(jsonPath("$.page.totalElements").value(1));
        }

        @Test
        @WithMockUser
        @DisplayName("deve retornar página vazia quando usuário não tem pedidos")
        void shouldReturnEmptyPage() throws Exception {
            when(orderService.findByUserId(anyLong(), any())).thenReturn(Page.empty());

            mockMvc.perform(get("/orders"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isEmpty());
        }

        @Test
        @DisplayName("deve retornar 401 para requisição sem autenticação")
        void shouldReturn401WhenUnauthenticated() throws Exception {
            mockMvc.perform(get("/orders"))
                    .andExpect(status().isUnauthorized());
        }
    }

    // -------------------------------------------------------------------------
    // GET /orders/{id}
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("GET /orders/{id}")
    class FindById {

        @Test
        @WithMockUser
        @DisplayName("deve retornar 200 com o pedido encontrado")
        void shouldReturn200WhenOrderExists() throws Exception {
            when(orderService.findById(1L)).thenReturn(orderResponse);

            mockMvc.perform(get("/orders/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value("1"))
                    .andExpect(jsonPath("$.status").value("PENDING"))
                    .andExpect(jsonPath("$.userEmail").value("user@example.com"));
        }

        @Test
        @WithMockUser
        @DisplayName("deve retornar 404 quando pedido não existe")
        void shouldReturn404WhenOrderNotFound() throws Exception {
            when(orderService.findById(99L)).thenThrow(new OrderNotFoundException(99L));

            mockMvc.perform(get("/orders/99"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("deve retornar 401 para requisição sem autenticação")
        void shouldReturn401WhenUnauthenticated() throws Exception {
            mockMvc.perform(get("/orders/1"))
                    .andExpect(status().isUnauthorized());
        }
    }

    // -------------------------------------------------------------------------
    // POST /orders
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("POST /orders")
    class Create {

        private OrderRequest validRequest() {
            OrderItemRequest item = new OrderItemRequest();
            item.setProductId(10L);
            item.setQuantity(2);
            item.setUnitPrice(new BigDecimal("149.95"));

            OrderRequest request = new OrderRequest();
            request.setItems(List.of(item));
            return request;
        }

        @Test
        @WithMockUser
        @DisplayName("deve retornar 201 com o pedido criado")
        void shouldReturn201WhenOrderCreated() throws Exception {
            when(orderService.create(any(), eq(42L), eq("user@example.com")))
                    .thenReturn(orderResponse);

            mockMvc.perform(post("/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest())))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value("1"))
                    .andExpect(jsonPath("$.status").value("PENDING"))
                    .andExpect(jsonPath("$.totalAmount").value("299.90"));
        }

        @Test
        @WithMockUser
        @DisplayName("deve retornar 400 quando body está vazio")
        void shouldReturn400WhenBodyIsEmpty() throws Exception {
            mockMvc.perform(post("/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser
        @DisplayName("deve retornar 400 quando lista de itens está vazia")
        void shouldReturn400WhenItemsListIsEmpty() throws Exception {
            OrderRequest request = new OrderRequest();
            request.setItems(List.of());

            mockMvc.perform(post("/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser
        @DisplayName("deve retornar 400 quando item não tem productId")
        void shouldReturn400WhenProductIdIsMissing() throws Exception {
            OrderItemRequest item = new OrderItemRequest();
            item.setQuantity(1);
            item.setUnitPrice(new BigDecimal("99.00"));

            OrderRequest request = new OrderRequest();
            request.setItems(List.of(item));

            mockMvc.perform(post("/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser
        @DisplayName("deve retornar 400 quando quantidade é zero")
        void shouldReturn400WhenQuantityIsZero() throws Exception {
            OrderItemRequest item = new OrderItemRequest();
            item.setProductId(1L);
            item.setQuantity(0);
            item.setUnitPrice(new BigDecimal("99.00"));

            OrderRequest request = new OrderRequest();
            request.setItems(List.of(item));

            mockMvc.perform(post("/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("deve retornar 401 para requisição sem autenticação")
        void shouldReturn401WhenUnauthenticated() throws Exception {
            mockMvc.perform(post("/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest())))
                    .andExpect(status().isUnauthorized());
        }
    }

    // -------------------------------------------------------------------------
    // PATCH /orders/{id}/cancel
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("PATCH /orders/{id}/cancel")
    class Cancel {

        @Test
        @WithMockUser
        @DisplayName("deve retornar 200 com o pedido cancelado")
        void shouldReturn200WhenOrderCancelled() throws Exception {
            OrderResponse cancelled = new OrderResponse(
                    1L, 42L, "user@example.com", OrderStatus.CANCELLED,
                    new BigDecimal("299.90"),
                    List.of(new OrderResponse.OrderItemResponse(10L, 2, new BigDecimal("149.95"))),
                    LocalDateTime.now(), LocalDateTime.now()
            );
            doNothing().when(orderService).cancelOrder(eq(1L), anyString());
            when(orderService.findById(1L)).thenReturn(cancelled);

            mockMvc.perform(patch("/orders/1/cancel"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("CANCELLED"));
        }

        @Test
        @WithMockUser
        @DisplayName("deve retornar 404 quando pedido não existe")
        void shouldReturn404WhenOrderNotFound() throws Exception {
            doThrow(new OrderNotFoundException(99L))
                    .when(orderService).cancelOrder(eq(99L), anyString());

            mockMvc.perform(patch("/orders/99/cancel"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("deve retornar 401 para requisição sem autenticação")
        void shouldReturn401WhenUnauthenticated() throws Exception {
            mockMvc.perform(patch("/orders/1/cancel"))
                    .andExpect(status().isUnauthorized());
        }
    }

    // -------------------------------------------------------------------------
    // GET /orders/all  (admin only)
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("GET /orders/all")
    class FindAll {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("deve retornar 200 com todos os pedidos para ADMIN")
        void shouldReturn200ForAdmin() throws Exception {
            Page<OrderResponse> page = new PageImpl<>(List.of(orderResponse), PageRequest.of(0, 10), 1);
            when(orderService.findAll(any())).thenReturn(page);

            mockMvc.perform(get("/orders/all"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].id").value("1"))
                    .andExpect(jsonPath("$.page.totalElements").value(1));
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("deve retornar 403 para usuário sem role ADMIN")
        void shouldReturn403ForNonAdmin() throws Exception {
            mockMvc.perform(get("/orders/all"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("deve retornar 401 para requisição sem autenticação")
        void shouldReturn401WhenUnauthenticated() throws Exception {
            mockMvc.perform(get("/orders/all"))
                    .andExpect(status().isUnauthorized());
        }
    }
}