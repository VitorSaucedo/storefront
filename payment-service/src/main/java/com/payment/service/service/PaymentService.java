package com.payment.service.service;

import org.springframework.dao.DataIntegrityViolationException;
import tools.jackson.databind.ObjectMapper;
import com.payment.service.domain.Payment;
import com.payment.service.domain.PaymentStatus;
import com.payment.service.domain.OutboxEvent;
import com.payment.service.dto.PaymentResponse;
import com.payment.service.dto.events.OrderCreatedEvent;
import com.payment.service.dto.events.PaymentFailedEvent;
import com.payment.service.dto.events.PaymentProcessedEvent;
import com.payment.service.exception.PaymentNotFoundException;
import com.payment.service.repository.PaymentRepository;
import com.payment.service.repository.OutboxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Random;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository paymentRepository;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;
    private final Random random = new Random();

    public PaymentService(PaymentRepository paymentRepository,
                          OutboxRepository outboxRepository,
                          ObjectMapper objectMapper) {
        this.paymentRepository = paymentRepository;
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void processPayment(OrderCreatedEvent event) {
        log.info("Iniciando processamento idempotente para orderId={}", event.orderId());

        if (paymentRepository.findByOrderId(event.orderId()).isPresent()) {
            log.warn("Pagamento para orderId={} já existe. Pulando processamento.", event.orderId());
            return;
        }

        try {
            Payment payment = Payment.builder()
                    .orderId(event.orderId())
                    .userId(event.userId())
                    .amount(event.totalAmount())
                    .status(PaymentStatus.PENDING)
                    .build();

            payment = paymentRepository.saveAndFlush(payment);

            boolean success = random.nextInt(10) < 8;

            if (success) {
                payment.setStatus(PaymentStatus.PROCESSED);
                paymentRepository.save(payment);

                saveToOutbox("PAYMENT_PROCESSED", new PaymentProcessedEvent(
                        event.orderId(), event.userId(), payment.getId(), event.totalAmount()));

                log.info("Pagamento confirmado com sucesso para orderId={}", event.orderId());
            } else {
                String failureReason = "Insufficient funds";
                payment.setStatus(PaymentStatus.FAILED);
                payment.setFailureReason(failureReason);
                paymentRepository.save(payment);

                saveToOutbox("PAYMENT_FAILED", new PaymentFailedEvent(
                        event.orderId(), event.userId(), failureReason));

                log.warn("Pagamento falhou para orderId={} por motivo: {}", event.orderId(), failureReason);
            }

        } catch (DataIntegrityViolationException e) {
            log.warn("Concorrência/Duplicidade detectada: O pedido {} já foi processado por outra instância.", event.orderId());
        }
    }

    private void saveToOutbox(String type, Object eventPayload) {
        try {
            String payloadJson = objectMapper.writeValueAsString(eventPayload);

            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .type(type)
                    .payload(payloadJson)
                    .status("PENDING")
                    .build();

            outboxRepository.save(outboxEvent);
        } catch (Exception e) {
            log.error("Error serializing event to JSON for outbox", e);
            throw new RuntimeException("Failed to process payment event", e);
        }
    }

    public Page<PaymentResponse> getPaymentsByUser(Long userId, Pageable pageable) {
        return paymentRepository.findByUserId(userId, pageable).map(this::toResponse);
    }

    public PaymentResponse getPaymentByOrder(Long orderId) {
        return paymentRepository.findByOrderId(orderId)
                .map(this::toResponse)
                .orElseThrow(() -> PaymentNotFoundException.byOrderId(orderId));
    }

    private PaymentResponse toResponse(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getOrderId(),
                payment.getUserId(),
                payment.getAmount(),
                payment.getStatus().name(),
                payment.getFailureReason()
        );
    }
}