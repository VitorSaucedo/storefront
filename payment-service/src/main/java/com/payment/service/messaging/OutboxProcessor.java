package com.payment.service.messaging;

import com.payment.service.domain.OutboxEvent;
import com.payment.service.dto.events.PaymentFailedEvent;
import com.payment.service.dto.events.PaymentProcessedEvent;
import com.payment.service.repository.OutboxRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@Component
public class OutboxProcessor {

    private final OutboxRepository outboxRepository;
    private final PaymentEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    public OutboxProcessor(OutboxRepository outboxRepository, PaymentEventPublisher eventPublisher, ObjectMapper objectMapper) {
        this.outboxRepository = outboxRepository;
        this.eventPublisher = eventPublisher;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void processPendingEvents() {
        var events = outboxRepository.findByStatusOrderByCreatedAtAsc("PENDING");

        for (OutboxEvent outbox : events) {
            try {
                if ("PAYMENT_PROCESSED".equals(outbox.getType())) {
                    var event = objectMapper.readValue(outbox.getPayload(), PaymentProcessedEvent.class);
                    eventPublisher.publishPaymentProcessed(event);
                } else if ("PAYMENT_FAILED".equals(outbox.getType())) {
                    var event = objectMapper.readValue(outbox.getPayload(), PaymentFailedEvent.class);
                    eventPublisher.publishPaymentFailed(event);
                }

                outbox.setStatus("PROCESSED");
                outboxRepository.save(outbox);
            } catch (Exception e) {
                outbox.setStatus("FAILED");
                outboxRepository.save(outbox);
            }
        }
    }
}