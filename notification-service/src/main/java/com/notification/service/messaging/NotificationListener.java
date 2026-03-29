package com.notification.service.messaging;

import com.notification.service.config.MessagingConstants;
import com.notification.service.dto.events.OrderCancelledEvent;
import com.notification.service.dto.events.OrderConfirmedEvent;
import com.notification.service.dto.events.PaymentFailedEvent;
import com.notification.service.service.NotificationService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class NotificationListener {

    private final NotificationService notificationService;

    public NotificationListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @RabbitListener(queues = MessagingConstants.ORDER_CONFIRMED_QUEUE)
    public void onOrderConfirmed(OrderConfirmedEvent event) {
        notificationService.notifyOrderConfirmed(event);
    }

    @RabbitListener(queues = MessagingConstants.ORDER_CANCELLED_QUEUE)
    public void onOrderCancelled(OrderCancelledEvent event) {
        notificationService.notifyOrderCancelled(event);
    }

    @RabbitListener(queues = MessagingConstants.PAYMENT_FAILED_QUEUE)
    public void onPaymentFailed(PaymentFailedEvent event) {
        notificationService.notifyPaymentFailed(event);
    }
}
