package com.auth.service.messaging;

import com.auth.service.config.MessagingConstants;
import com.auth.service.dto.events.UserRegisteredEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class AuthEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(AuthEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    public AuthEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleUserRegisteredEvent(UserRegisteredEvent event) {
        log.info("Publicando evento para RabbitMQ após commit: userId={}", event.userId());

        rabbitTemplate.convertAndSend(
                MessagingConstants.AUTH_EXCHANGE,
                MessagingConstants.USER_REGISTERED_ROUTING_KEY,
                event
        );

        log.info("Evento publicado com sucesso no exchange {}", MessagingConstants.AUTH_EXCHANGE);
    }
}
