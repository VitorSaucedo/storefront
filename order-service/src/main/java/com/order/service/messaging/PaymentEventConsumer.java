package com.order.service.messaging;

import com.order.service.config.MessagingConstants;
import com.order.service.dto.events.PaymentFailedEvent;
import com.order.service.dto.events.PaymentProcessedEvent;
import com.order.service.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class PaymentEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventConsumer.class);

    private final OrderService orderService;

    public PaymentEventConsumer(OrderService orderService) {
        this.orderService = orderService;
    }

    @RabbitListener(queues = MessagingConstants.PAYMENT_PROCESSED_QUEUE)
    public void onPaymentProcessed(PaymentProcessedEvent event) {
        log.info("Evento payment.processed recebido: orderId={}", event.orderId());
        try {
            orderService.confirmOrder(event.orderId());
        } catch (Exception ex) {
            log.error("Erro ao confirmar pedido: orderId={}, erro={}", event.orderId(), ex.getMessage());
        }
    }

    @RabbitListener(queues = MessagingConstants.PAYMENT_FAILED_QUEUE)
    public void onPaymentFailed(PaymentFailedEvent event) {
        log.info("Evento payment.failed recebido: orderId={}, reason={}", event.orderId(), event.reason());
        try {
            orderService.cancelOrder(event.orderId(), event.reason());
        } catch (Exception ex) {
            log.error("Erro ao cancelar pedido: orderId={}, erro={}", event.orderId(), ex.getMessage());
        }
    }
}