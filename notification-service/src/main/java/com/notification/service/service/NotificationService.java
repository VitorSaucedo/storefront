package com.notification.service.service;

import com.notification.service.dto.events.OrderCancelledEvent;
import com.notification.service.dto.events.OrderConfirmedEvent;
import com.notification.service.dto.events.PaymentFailedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    public void notifyOrderConfirmed(OrderConfirmedEvent event) {
        log.info("[NOTIFICATION] Order confirmed — orderId={}, userId={}, total={}",
                event.orderId(), event.userId(), event.totalAmount());
    }

    public void notifyOrderCancelled(OrderCancelledEvent event) {
        log.info("[NOTIFICATION] Order cancelled — orderId={}, userId={}, reason={}",
                event.orderId(), event.userId(), event.reason());
    }

    public void notifyPaymentFailed(PaymentFailedEvent event) {
        log.info("[NOTIFICATION] Payment failed — orderId={}, userId={}, reason={}",
                event.orderId(), event.userId(), event.reason());
    }
}
