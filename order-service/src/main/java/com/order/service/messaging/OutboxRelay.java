package com.order.service.messaging;

import com.order.service.config.MessagingConstants;
import com.order.service.domain.OutboxEvent;
import com.order.service.repository.OutboxRepository;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Component
public class OutboxRelay {

    private final OutboxRepository outboxRepository;
    private final RabbitTemplate rabbitTemplate;

    public OutboxRelay(OutboxRepository outboxRepository, RabbitTemplate rabbitTemplate) {
        this.outboxRepository = outboxRepository;
        this.rabbitTemplate = rabbitTemplate;
    }

    @Scheduled(fixedDelay = 1000)
    @Transactional
    public void processOutbox() {
        List<OutboxEvent> events = outboxRepository.findByProcessedFalseOrderByCreatedAtAsc();

        for (OutboxEvent event : events) {
            try {
                rabbitTemplate.convertAndSend(
                        MessagingConstants.ORDER_EXCHANGE,
                        event.getRoutingKey(),
                        event.getPayload()
                );

                event.setProcessed(true);
                outboxRepository.save(event);
            } catch (Exception e) {
            }
        }
    }
}