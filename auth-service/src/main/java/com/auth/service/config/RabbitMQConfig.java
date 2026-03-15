package com.auth.service.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String AUTH_EXCHANGE = "auth.exchange";
    public static final String USER_REGISTERED_QUEUE = "user.registered.queue";
    public static final String USER_REGISTERED_ROUTING_KEY = "user.registered";

    @Bean
    public TopicExchange authExchange() {
        return new TopicExchange(AUTH_EXCHANGE);
    }

    @Bean
    public Queue userRegisteredQueue() {
        return QueueBuilder.durable(USER_REGISTERED_QUEUE).build();
    }

    @Bean
    public Binding userRegisteredBinding(Queue userRegisteredQueue, TopicExchange authExchange) {
        return BindingBuilder
                .bind(userRegisteredQueue)
                .to(authExchange)
                .with(USER_REGISTERED_ROUTING_KEY);
    }

    @Bean
    public JacksonJsonMessageConverter messageConverter() {
        return new JacksonJsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        return template;
    }
}
