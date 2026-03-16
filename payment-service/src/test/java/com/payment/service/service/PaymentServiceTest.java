package com.payment.service.service;

import com.payment.service.domain.Payment;
import com.payment.service.domain.PaymentStatus;
import com.payment.service.dto.PaymentResponse;
import com.payment.service.dto.events.OrderCreatedEvent;
import com.payment.service.dto.events.PaymentFailedEvent;
import com.payment.service.dto.events.PaymentProcessedEvent;
import com.payment.service.exception.PaymentNotFoundException;
import com.payment.service.messaging.PaymentEventPublisher;
import com.payment.service.repository.PaymentRepository;
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
    private PaymentEventPublisher eventPublisher;

    @InjectMocks
    private PaymentService paymentService;

    private Payment existingPayment;

    @BeforeEach
    void setUp() {
        existingPayment = new Payment(10L, 42L, new BigDecimal("299.90"));
        existingPayment.setId(1L);
        existingPayment.setStatus(PaymentStatus.PENDING);
    }

    // -------------------------------------------------------------------------
    // processPayment
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("processPayment")
    class ProcessPayment {

        private OrderCreatedEvent buildEvent() {
            return new OrderCreatedEvent(10L, 42L, new BigDecimal("299.90"));
        }

        @Test
        @DisplayName("deve criar o pagamento e publicar exatamente um evento (processado ou falho)")
        void shouldCreatePaymentAndPublishExactlyOneEvent() {
            when(paymentRepository.findByOrderId(10L)).thenReturn(Optional.empty());
            when(paymentRepository.save(any())).thenReturn(existingPayment);

            paymentService.processPayment(buildEvent());

            verify(paymentRepository, atLeast(2)).save(any());

            int processed = mockingDetails(eventPublisher).getInvocations().stream()
                    .filter(i -> i.getMethod().getName().equals("publishPaymentProcessed"))
                    .mapToInt(i -> 1).sum();
            int failed = mockingDetails(eventPublisher).getInvocations().stream()
                    .filter(i -> i.getMethod().getName().equals("publishPaymentFailed"))
                    .mapToInt(i -> 1).sum();

            assertThat(processed + failed).isEqualTo(1);
        }

        @Test
        @DisplayName("deve ignorar quando já existe pagamento para o pedido (idempotência)")
        void shouldSkipWhenPaymentAlreadyExists() {
            when(paymentRepository.findByOrderId(10L)).thenReturn(Optional.of(existingPayment));

            paymentService.processPayment(buildEvent());

            verify(paymentRepository, never()).save(any());
            verify(eventPublisher, never()).publishPaymentProcessed(any());
            verify(eventPublisher, never()).publishPaymentFailed(any());
        }

        @Test
        @DisplayName("deve publicar PaymentProcessedEvent com os dados corretos quando bem-sucedido")
        void shouldPublishProcessedEventWithCorrectData() {
            when(paymentRepository.findByOrderId(10L)).thenReturn(Optional.empty());

            boolean eventPublished = false;
            for (int attempt = 0; attempt < 100; attempt++) {
                reset(paymentRepository, eventPublisher);
                when(paymentRepository.findByOrderId(10L)).thenReturn(Optional.empty());
                when(paymentRepository.save(any())).thenReturn(existingPayment);

                paymentService.processPayment(buildEvent());

                ArgumentCaptor<PaymentProcessedEvent> captor =
                        ArgumentCaptor.forClass(PaymentProcessedEvent.class);
                try {
                    verify(eventPublisher).publishPaymentProcessed(captor.capture());
                    PaymentProcessedEvent event = captor.getValue();
                    assertThat(event.orderId()).isEqualTo(10L);
                    assertThat(event.userId()).isEqualTo(42L);
                    assertThat(event.amount()).isEqualByComparingTo(new BigDecimal("299.90"));
                    eventPublished = true;
                    break;
                } catch (AssertionError ignored) {
                }
            }
            assertThat(eventPublished)
                    .as("PaymentProcessedEvent deve ter sido publicado em ao menos uma das 100 tentativas")
                    .isTrue();
        }

        @Test
        @DisplayName("deve publicar PaymentFailedEvent com reason 'Insufficient funds' quando falho")
        void shouldPublishFailedEventWithCorrectData() {
            when(paymentRepository.findByOrderId(10L)).thenReturn(Optional.empty());

            // Força 100 tentativas — ao menos uma deve ser falha dado que 20% falham
            boolean eventPublished = false;
            for (int attempt = 0; attempt < 100; attempt++) {
                reset(paymentRepository, eventPublisher);
                when(paymentRepository.findByOrderId(10L)).thenReturn(Optional.empty());
                when(paymentRepository.save(any())).thenReturn(existingPayment);

                paymentService.processPayment(buildEvent());

                ArgumentCaptor<PaymentFailedEvent> captor =
                        ArgumentCaptor.forClass(PaymentFailedEvent.class);
                try {
                    verify(eventPublisher).publishPaymentFailed(captor.capture());
                    PaymentFailedEvent event = captor.getValue();
                    assertThat(event.orderId()).isEqualTo(10L);
                    assertThat(event.userId()).isEqualTo(42L);
                    assertThat(event.reason()).isEqualTo("Insufficient funds");
                    eventPublished = true;
                    break;
                } catch (AssertionError ignored) {
                }
            }
            assertThat(eventPublished)
                    .as("PaymentFailedEvent deve ter sido publicado em ao menos uma das 100 tentativas")
                    .isTrue();
        }
    }

    // -------------------------------------------------------------------------
    // getPaymentsByUser
    // -------------------------------------------------------------------------
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
            assertThat(result.getContent().get(0).id()).isEqualTo(1L);
            assertThat(result.getContent().get(0).orderId()).isEqualTo(10L);
            assertThat(result.getContent().get(0).userId()).isEqualTo(42L);
        }

        @Test
        @DisplayName("deve retornar página vazia quando usuário não tem pagamentos")
        void shouldReturnEmptyPageWhenNoPayments() {
            PageRequest pageable = PageRequest.of(0, 10);
            when(paymentRepository.findByUserId(99L, pageable)).thenReturn(Page.empty());

            Page<PaymentResponse> result = paymentService.getPaymentsByUser(99L, pageable);

            assertThat(result).isEmpty();
        }
    }

    // -------------------------------------------------------------------------
    // getPaymentByOrder
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("getPaymentByOrder")
    class GetPaymentByOrder {

        @Test
        @DisplayName("deve retornar o pagamento quando encontrado pelo orderId")
        void shouldReturnPaymentWhenFound() {
            existingPayment.setStatus(PaymentStatus.PROCESSED);
            when(paymentRepository.findByOrderId(10L)).thenReturn(Optional.of(existingPayment));

            PaymentResponse result = paymentService.getPaymentByOrder(10L);

            assertThat(result.id()).isEqualTo(1L);
            assertThat(result.orderId()).isEqualTo(10L);
            assertThat(result.status()).isEqualTo("PROCESSED");
            assertThat(result.failureReason()).isNull();
        }

        @Test
        @DisplayName("deve retornar failureReason quando pagamento falhou")
        void shouldReturnFailureReasonWhenPaymentFailed() {
            existingPayment.setStatus(PaymentStatus.FAILED);
            existingPayment.setFailureReason("Insufficient funds");
            when(paymentRepository.findByOrderId(10L)).thenReturn(Optional.of(existingPayment));

            PaymentResponse result = paymentService.getPaymentByOrder(10L);

            assertThat(result.status()).isEqualTo("FAILED");
            assertThat(result.failureReason()).isEqualTo("Insufficient funds");
        }

        @Test
        @DisplayName("deve lançar PaymentNotFoundException quando não encontrado")
        void shouldThrowWhenNotFound() {
            when(paymentRepository.findByOrderId(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> paymentService.getPaymentByOrder(99L))
                    .isInstanceOf(PaymentNotFoundException.class)
                    .hasMessageContaining("99");
        }
    }
}