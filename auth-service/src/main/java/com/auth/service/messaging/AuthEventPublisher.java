package com.auth.service.messaging;

import com.auth.service.config.RabbitMQConfig;
import com.auth.service.dto.events.UserRegisteredEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class AuthEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(AuthEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    public AuthEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishUserRegistered(UserRegisteredEvent event) {
        log.info("Publicando evento user.registered para userId={}", event.getUserId());

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.AUTH_EXCHANGE,
                RabbitMQConfig.USER_REGISTERED_ROUTING_KEY,
                event
        );

        log.info("Evento user.registered publicado com sucesso para userId={}", event.getUserId());
    }
}
