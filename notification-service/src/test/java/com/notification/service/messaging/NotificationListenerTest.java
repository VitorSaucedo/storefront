package com.notification.service.messaging;

import com.notification.service.dto.events.OrderCancelledEvent;
import com.notification.service.dto.events.OrderConfirmedEvent;
import com.notification.service.dto.events.PaymentFailedEvent;
import com.notification.service.service.NotificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class NotificationListenerTest {

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private NotificationListener notificationListener;

    @Test
    @DisplayName("onOrderConfirmed deve delegar ao NotificationService com o evento correto")
    void shouldDelegateOrderConfirmedToService() {
        OrderConfirmedEvent event = new OrderConfirmedEvent(123L, 456L, 399.90);

        notificationListener.onOrderConfirmed(event);

        verify(notificationService).notifyOrderConfirmed(event);
        verifyNoMoreInteractions(notificationService);
    }

    @Test
    @DisplayName("onOrderCancelled deve delegar ao NotificationService com o evento correto")
    void shouldDelegateOrderCancelledToService() {
        OrderCancelledEvent event = new OrderCancelledEvent(321L, 654L, "Estoque insuficiente");

        notificationListener.onOrderCancelled(event);

        verify(notificationService).notifyOrderCancelled(event);
        verifyNoMoreInteractions(notificationService);
    }

    @Test
    @DisplayName("onPaymentFailed deve delegar ao NotificationService com o evento correto")
    void shouldDelegatePaymentFailedToService() {
        PaymentFailedEvent event = new PaymentFailedEvent(777L, 888L, "Cartão recusado");

        notificationListener.onPaymentFailed(event);

        verify(notificationService).notifyPaymentFailed(event);
        verifyNoMoreInteractions(notificationService);
    }
}