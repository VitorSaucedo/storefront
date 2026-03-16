package com.notification.service.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.notification.service.dto.events.OrderCancelledEvent;
import com.notification.service.dto.events.OrderConfirmedEvent;
import com.notification.service.dto.events.PaymentFailedEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @InjectMocks
    private NotificationService notificationService;

    private ListAppender<ILoggingEvent> listAppender;
    private Logger serviceLogger;

    @BeforeEach
    void setUp() {
        serviceLogger = (Logger) LoggerFactory.getLogger(NotificationService.class);
        listAppender = new ListAppender<>();
        listAppender.start();
        serviceLogger.addAppender(listAppender);
    }

    @AfterEach
    void tearDown() {
        serviceLogger.detachAppender(listAppender);
        listAppender.stop();
    }

    // -------------------------------------------------------------------------
    // notifyOrderConfirmed
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("notifyOrderConfirmed")
    class NotifyOrderConfirmed {

        @Test
        @DisplayName("deve emitir log INFO com orderId, userId e totalAmount")
        void shouldLogOrderConfirmedWithCorrectData() {
            OrderConfirmedEvent event = new OrderConfirmedEvent(123L, 456L, 399.90);

            notificationService.notifyOrderConfirmed(event);

            List<ILoggingEvent> logs = listAppender.list;
            assertThat(logs).hasSize(1);

            ILoggingEvent log = logs.get(0);
            assertThat(log.getLevel()).isEqualTo(Level.INFO);
            assertThat(log.getFormattedMessage())
                    .contains("[NOTIFICATION]")
                    .contains("Order confirmed")
                    .contains("123")
                    .contains("456")
                    .contains("399.9");
        }

        @Test
        @DisplayName("deve emitir exatamente um log por chamada")
        void shouldEmitExactlyOneLog() {
            OrderConfirmedEvent event = new OrderConfirmedEvent(999L, 1L, 1.00);

            notificationService.notifyOrderConfirmed(event);

            assertThat(listAppender.list).hasSize(1);
        }
    }

    // -------------------------------------------------------------------------
    // notifyOrderCancelled
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("notifyOrderCancelled")
    class NotifyOrderCancelled {

        @Test
        @DisplayName("deve emitir log INFO com orderId, userId e reason")
        void shouldLogOrderCancelledWithCorrectData() {
            OrderCancelledEvent event = new OrderCancelledEvent(321L, 654L, "Estoque insuficiente");

            notificationService.notifyOrderCancelled(event);

            List<ILoggingEvent> logs = listAppender.list;
            assertThat(logs).hasSize(1);

            ILoggingEvent log = logs.get(0);
            assertThat(log.getLevel()).isEqualTo(Level.INFO);
            assertThat(log.getFormattedMessage())
                    .contains("[NOTIFICATION]")
                    .contains("Order cancelled")
                    .contains("321")
                    .contains("654")
                    .contains("Estoque insuficiente");
        }

        @Test
        @DisplayName("deve emitir exatamente um log por chamada")
        void shouldEmitExactlyOneLog() {
            OrderCancelledEvent event = new OrderCancelledEvent(1L, 2L, "Timeout");

            notificationService.notifyOrderCancelled(event);

            assertThat(listAppender.list).hasSize(1);
        }
    }

    // -------------------------------------------------------------------------
    // notifyPaymentFailed
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("notifyPaymentFailed")
    class NotifyPaymentFailed {

        @Test
        @DisplayName("deve emitir log INFO com orderId, userId e reason")
        void shouldLogPaymentFailedWithCorrectData() {
            PaymentFailedEvent event = new PaymentFailedEvent(777L, 888L, "Cartão recusado");

            notificationService.notifyPaymentFailed(event);

            List<ILoggingEvent> logs = listAppender.list;
            assertThat(logs).hasSize(1);

            ILoggingEvent log = logs.get(0);
            assertThat(log.getLevel()).isEqualTo(Level.INFO);
            assertThat(log.getFormattedMessage())
                    .contains("[NOTIFICATION]")
                    .contains("Payment failed")
                    .contains("777")
                    .contains("888")
                    .contains("Cartão recusado");
        }

        @Test
        @DisplayName("deve emitir exatamente um log por chamada")
        void shouldEmitExactlyOneLog() {
            PaymentFailedEvent event = new PaymentFailedEvent(111L, 222L, "Saldo insuficiente");

            notificationService.notifyPaymentFailed(event);

            assertThat(listAppender.list).hasSize(1);
        }
    }
}