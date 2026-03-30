package com.payment.service.service;

import tools.jackson.databind.ObjectMapper;
import com.payment.service.domain.Payment;
import com.payment.service.domain.PaymentStatus;
import com.payment.service.domain.OutboxEvent;
import com.payment.service.dto.PaymentResponse;
import com.payment.service.dto.events.OrderCreatedEvent;
import com.payment.service.exception.PaymentNotFoundException;
import com.payment.service.repository.PaymentRepository;
import com.payment.service.repository.OutboxRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private OutboxRepository outboxRepository;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private PaymentService paymentService;

    private Payment existingPayment;

    @BeforeEach
    void setUp() {
        existingPayment = Payment.builder()
                .id(1L)
                .orderId(10L)
                .userId(42L)
                .amount(new BigDecimal("299.90"))
                .status(PaymentStatus.PENDING)
                .build();
    }

    @Nested
    @DisplayName("processPayment")
    class ProcessPayment {

        private OrderCreatedEvent buildEvent() {
            return new OrderCreatedEvent(10L, 42L, new BigDecimal("299.90"));
        }

        @Test
        @DisplayName("deve criar o pagamento e salvar exatamente um evento no outbox")
        void shouldCreatePaymentAndSaveOutboxEvent() {
            when(paymentRepository.findByOrderId(10L)).thenReturn(Optional.empty());
            when(paymentRepository.saveAndFlush(any())).thenReturn(existingPayment);

            paymentService.processPayment(buildEvent());

            verify(paymentRepository).saveAndFlush(any());
            verify(outboxRepository, times(1)).save(any(OutboxEvent.class));
        }

        @Test
        @DisplayName("deve ignorar processamento quando já existe pagamento (idempotência defensiva)")
        void shouldSkipWhenPaymentAlreadyExists() {
            when(paymentRepository.findByOrderId(10L)).thenReturn(Optional.of(existingPayment));

            paymentService.processPayment(buildEvent());

            verify(paymentRepository, never()).saveAndFlush(any());
            verify(outboxRepository, never()).save(any());
        }

        @Test
        @DisplayName("deve tratar concorrência via DataIntegrityViolationException graciosamente")
        void shouldHandleConcurrencyGraciously() {
            when(paymentRepository.findByOrderId(10L)).thenReturn(Optional.empty());
            when(paymentRepository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("Duplicate key"));

            paymentService.processPayment(buildEvent());

            verify(outboxRepository, never()).save(any());
        }

        @Test
        @DisplayName("deve salvar evento PAYMENT_PROCESSED no outbox quando bem-sucedido")
        void shouldSaveProcessedEventToOutbox() throws Exception {
            boolean eventSaved = false;
            for (int i = 0; i < 100; i++) {
                reset(paymentRepository, outboxRepository);
                when(paymentRepository.findByOrderId(10L)).thenReturn(Optional.empty());
                when(paymentRepository.saveAndFlush(any())).thenReturn(existingPayment);

                paymentService.processPayment(buildEvent());

                ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
                try {
                    verify(outboxRepository).save(captor.capture());
                    if ("PAYMENT_PROCESSED".equals(captor.getValue().getType())) {
                        assertThat(captor.getValue().getStatus()).isEqualTo("PENDING");
                        assertThat(captor.getValue().getPayload()).contains("\"orderId\":10");
                        eventSaved = true;
                        break;
                    }
                } catch (AssertionError ignored) {}
            }
            assertThat(eventSaved).isTrue();
        }

        @Test
        @DisplayName("deve salvar evento PAYMENT_FAILED no outbox quando falho")
        void shouldSaveFailedEventToOutbox() throws Exception {
            boolean eventSaved = false;
            for (int i = 0; i < 100; i++) {
                reset(paymentRepository, outboxRepository);
                when(paymentRepository.findByOrderId(10L)).thenReturn(Optional.empty());
                when(paymentRepository.saveAndFlush(any())).thenReturn(existingPayment);

                paymentService.processPayment(buildEvent());

                ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
                try {
                    verify(outboxRepository).save(captor.capture());
                    if ("PAYMENT_FAILED".equals(captor.getValue().getType())) {
                        assertThat(captor.getValue().getPayload()).contains("Insufficient funds");
                        eventSaved = true;
                        break;
                    }
                } catch (AssertionError ignored) {}
            }
            assertThat(eventSaved).isTrue();
        }
    }

    @Nested
    @DisplayName("getPaymentsByUser")
    class GetPaymentsByUser {

        @Test
        @DisplayName("deve retornar página de pagamentos do usuário")
        void shouldReturnPaymentPageForUser() {
            PageRequest pageable = PageRequest.of(0, 10);
            Page<Payment> page = new PageImpl<>(List.of(existingPayment), pageable, 1);
            when(paymentRepository.findByUserId(42L, pageable)).thenReturn(page);

            Page<PaymentResponse> result = paymentService.getPaymentsByUser(42L, pageable);

            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent().get(0).orderId()).isEqualTo(10L);
        }
    }

    @Nested
    @DisplayName("getPaymentByOrder")
    class GetPaymentByOrder {

        @Test
        @DisplayName("deve retornar o pagamento quando encontrado")
        void shouldReturnPaymentWhenFound() {
            when(paymentRepository.findByOrderId(10L)).thenReturn(Optional.of(existingPayment));

            PaymentResponse result = paymentService.getPaymentByOrder(10L);

            assertThat(result.orderId()).isEqualTo(10L);
            assertThat(result.status()).isEqualTo("PENDING");
        }

        @Test
        @DisplayName("deve lançar PaymentNotFoundException quando não encontrado")
        void shouldThrowWhenNotFound() {
            when(paymentRepository.findByOrderId(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> paymentService.getPaymentByOrder(99L))
                    .isInstanceOf(PaymentNotFoundException.class);
        }
    }
}