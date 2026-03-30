package com.payment.service.controller;

import com.payment.service.config.TestSecurityConfig;
import com.payment.service.dto.PaymentResponse;
import com.payment.service.exception.PaymentNotFoundException;
import com.payment.service.service.PaymentService;
import com.payment.service.util.JwtClaimsExtractor;
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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PaymentController.class)
@Import(TestSecurityConfig.class)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PaymentService paymentService;

    @MockitoBean
    private JwtClaimsExtractor claimsExtractor;

    private PaymentResponse paymentResponse;

    @BeforeEach
    void setUp() {
        paymentResponse = PaymentResponse.builder()
                .id(1L)
                .orderId(10L)
                .userId(42L)
                .amount(new BigDecimal("299.90"))
                .status("PROCESSED")
                .build();

        when(claimsExtractor.currentUserId()).thenReturn(42L);
    }

    @Nested
    @DisplayName("GET /payments/user")
    class GetMyPayments {

        @Test
        @WithMockUser
        @DisplayName("deve retornar 200 com os pagamentos do usuário autenticado")
        void shouldReturn200WithUserPayments() throws Exception {
            Page<PaymentResponse> page = new PageImpl<>(
                    List.of(paymentResponse), PageRequest.of(0, 10), 1
            );
            when(paymentService.getPaymentsByUser(eq(42L), any())).thenReturn(page);

            mockMvc.perform(get("/payments/user"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].id").value("1"))
                    .andExpect(jsonPath("$.content[0].status").value("PROCESSED"))
                    .andExpect(jsonPath("$.totalElements").value(1));
        }

        @Test
        @WithMockUser
        @DisplayName("deve retornar página vazia quando usuário não tem pagamentos")
        void shouldReturnEmptyPage() throws Exception {
            when(paymentService.getPaymentsByUser(eq(42L), any())).thenReturn(Page.empty());

            mockMvc.perform(get("/payments/user"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isEmpty());
        }
    }

    @Nested
    @DisplayName("GET /payments/user/{userId}")
    class GetPaymentsByUser {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("deve retornar 200 com pagamentos do usuário para ADMIN")
        void shouldReturn200ForAdmin() throws Exception {
            Page<PaymentResponse> page = new PageImpl<>(
                    List.of(paymentResponse), PageRequest.of(0, 10), 1
            );
            when(paymentService.getPaymentsByUser(eq(42L), any())).thenReturn(page);

            mockMvc.perform(get("/payments/user/42"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].userId").value("42"));
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("deve retornar 403 para usuário sem role ADMIN")
        void shouldReturn403ForNonAdmin() throws Exception {
            mockMvc.perform(get("/payments/user/42"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /payments/order/{orderId}")
    class GetPaymentByOrder {

        @Test
        @WithMockUser
        @DisplayName("deve retornar 200 com o pagamento do pedido")
        void shouldReturn200WhenPaymentExists() throws Exception {
            when(paymentService.getPaymentByOrder(10L)).thenReturn(paymentResponse);

            mockMvc.perform(get("/payments/order/10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value("1"))
                    .andExpect(jsonPath("$.amount").value("299.90"));
        }

        @Test
        @WithMockUser
        @DisplayName("deve retornar 200 com failureReason quando pagamento falhou")
        void shouldReturn200WithFailureReason() throws Exception {
            PaymentResponse failed = PaymentResponse.builder()
                    .id(2L)
                    .orderId(11L)
                    .userId(42L)
                    .amount(new BigDecimal("150.00"))
                    .status("FAILED")
                    .failureReason("Insufficient funds")
                    .build();

            when(paymentService.getPaymentByOrder(11L)).thenReturn(failed);

            mockMvc.perform(get("/payments/order/11"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("FAILED"))
                    .andExpect(jsonPath("$.failureReason").value("Insufficient funds"));
        }

        @Test
        @WithMockUser
        @DisplayName("deve retornar 404 quando pagamento não existe")
        void shouldReturn404WhenNotFound() throws Exception {
            when(paymentService.getPaymentByOrder(99L))
                    .thenThrow(PaymentNotFoundException.byOrderId(99L));

            mockMvc.perform(get("/payments/order/99"))
                    .andExpect(status().isNotFound());
        }
    }
}