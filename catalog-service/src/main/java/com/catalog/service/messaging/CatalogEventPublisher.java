package com.catalog.service.messaging;

import com.catalog.service.config.RabbitMQConfig;
import com.catalog.service.dto.events.OrderCompensatedEvent;
import com.catalog.service.dto.events.ProductUpdatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class CatalogEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(CatalogEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    public CatalogEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishProductUpdated(ProductUpdatedEvent event) {
        log.info("Publicando produto.atualizado: productId={}", event.productId());
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.CATALOG_EXCHANGE,
                RabbitMQConfig.PRODUCT_UPDATED_ROUTING_KEY,
                event
        );
        log.info("Evento produto.atualizado publicado com sucesso: productId={}", event.productId());
    }

    public void publishOrderCompensated(OrderCompensatedEvent event) {
        log.warn("Publicando order.compensated: orderId={}", event.orderId());
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.CATALOG_EXCHANGE,
                RabbitMQConfig.ORDER_COMPENSATED_ROUTING_KEY,
                event
        );
        log.warn("Evento order.compensated publicado: orderId={}", event.orderId());
    }
}
