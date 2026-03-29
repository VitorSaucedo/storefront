package com.catalog.service.messaging;

import com.catalog.service.config.MessagingConstants;
import com.catalog.service.dto.OrderItem;
import com.catalog.service.dto.events.OrderCancelledEvent;
import com.catalog.service.dto.events.OrderCompensatedEvent;
import com.catalog.service.dto.events.OrderConfirmedEvent;
import com.catalog.service.service.ProductService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class OrderEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderEventConsumer.class);

    private final ProductService productService;
    private final CatalogEventPublisher eventPublisher;

    public OrderEventConsumer(ProductService productService,
                              CatalogEventPublisher eventPublisher) {
        this.productService = productService;
        this.eventPublisher = eventPublisher;
    }

    @RabbitListener(queues = MessagingConstants.ORDER_CONFIRMED_QUEUE)
    public void onOrderConfirmed(OrderConfirmedEvent event) {
        log.info("Pedido confirmado com sucesso: orderId={}. Estoque já garantido na reserva.", event.orderId());
    }

    @RabbitListener(queues = MessagingConstants.ORDER_CANCELLED_QUEUE)
    public void onOrderCancelled(OrderCancelledEvent event) {
        log.info("Cancelamento recebido. Iniciando devolução de estoque para o pedido: {}", event.orderId());

        for (OrderItem item : event.items()) {
            try {
                productService.incrementStock(item.productId(), item.quantity());
            } catch (Exception ex) {
                log.error("Falha ao devolver estoque do produto {} do pedido {}: {}",
                        item.productId(), event.orderId(), ex.getMessage());
            }
        }
    }

    private void compensate(List<OrderConfirmedEvent.OrderItem> decremented, Long orderId) {
        if (decremented.isEmpty()) return;

        log.warn("Iniciando compensação para orderId={}, itens afetados={}",
                orderId, decremented.size());

        for (OrderConfirmedEvent.OrderItem item : decremented) {
            try {
                productService.incrementStock(item.productId(), item.quantity());
                log.info("Compensação aplicada: productId={}, quantidade={}",
                        item.productId(), item.quantity());
            } catch (Exception ex) {
                log.error("CRÍTICO — falha na compensação: productId={}, orderId={}, erro={}. " +
                                "Intervenção manual necessária.",
                        item.productId(), orderId, ex.getMessage());
            }
        }

        eventPublisher.publishOrderCompensated(
                new OrderCompensatedEvent(orderId, LocalDateTime.now()));
    }
}
