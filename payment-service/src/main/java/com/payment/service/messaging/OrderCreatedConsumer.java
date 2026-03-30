package com.payment.service.messaging;

import com.payment.service.config.MessagingConstants;
import com.payment.service.config.RabbitMQConfig;
import com.payment.service.dto.events.OrderCreatedEvent;
import com.payment.service.service.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class OrderCreatedConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderCreatedConsumer.class);

    private final PaymentService paymentService;

    public OrderCreatedConsumer(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @RabbitListener(queues = MessagingConstants.ORDER_CREATED_QUEUE)
    public void handleOrderCreated(OrderCreatedEvent event) {
        log.info("Received order.created: orderId={}, userId={}, amount={}",
                event.orderId(), event.userId(), event.totalAmount());
        paymentService.processPayment(event);
    }
}

