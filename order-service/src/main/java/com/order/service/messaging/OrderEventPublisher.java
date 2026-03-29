package com.order.service.messaging;

import com.order.service.config.MessagingConstants;
import com.order.service.dto.events.OrderCancelledEvent;
import com.order.service.dto.events.OrderConfirmedEvent;
import com.order.service.dto.events.OrderCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class OrderEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(OrderEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    public OrderEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishOrderCreated(OrderCreatedEvent event) {
        log.info("Publicando order.created: orderId={}", event.orderId());
        rabbitTemplate.convertAndSend(
                MessagingConstants.ORDER_EXCHANGE,
                MessagingConstants.ORDER_CREATED_ROUTING_KEY,
                event
        );
        log.info("Evento order.created publicado: orderId={}", event.orderId());
    }

    public void publishOrderConfirmed(OrderConfirmedEvent event) {
        log.info("Publicando order.confirmed: orderId={}", event.orderId());
        rabbitTemplate.convertAndSend(
                MessagingConstants.ORDER_EXCHANGE,
                MessagingConstants.ORDER_CONFIRMED_ROUTING_KEY,
                event
        );
        log.info("Evento order.confirmed publicado: orderId={}", event.orderId());
    }

    public void publishOrderCancelled(OrderCancelledEvent event) {
        log.info("Publicando order.cancelled: orderId={}", event.orderId());
        rabbitTemplate.convertAndSend(
                MessagingConstants.ORDER_EXCHANGE,
                MessagingConstants.ORDER_CANCELLED_ROUTING_KEY,
                event
        );
        log.info("Evento order.cancelled publicado: orderId={}", event.orderId());
    }
}