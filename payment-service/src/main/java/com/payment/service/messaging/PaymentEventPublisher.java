package com.payment.service.messaging;

import com.payment.service.config.MessagingConstants;
import com.payment.service.config.RabbitMQConfig;
import com.payment.service.dto.events.PaymentFailedEvent;
import com.payment.service.dto.events.PaymentProcessedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class PaymentEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    public PaymentEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishPaymentProcessed(PaymentProcessedEvent event) {
        log.info("Publishing payment.processed for orderId={}", event.orderId());
        rabbitTemplate.convertAndSend(
                MessagingConstants.PAYMENT_EXCHANGE,
                MessagingConstants.PAYMENT_PROCESSED_ROUTING_KEY,
                event
        );
    }

    public void publishPaymentFailed(PaymentFailedEvent event) {
        log.info("Publishing payment.failed for orderId={}", event.orderId());
        rabbitTemplate.convertAndSend(
                MessagingConstants.PAYMENT_EXCHANGE,
                MessagingConstants.PAYMENT_FAILED_ROUTING_KEY,
                event
        );
    }
}
