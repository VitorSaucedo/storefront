package com.notification.service.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.json.JsonMapper;

@Configuration
public class RabbitMQConfig {

    @Bean
    public Queue orderConfirmedNotificationQueue() {
        return new Queue(MessagingConstants.ORDER_CONFIRMED_QUEUE, true);
    }

    @Bean
    public Queue orderCancelledNotificationQueue() {
        return new Queue(MessagingConstants.ORDER_CANCELLED_QUEUE, true);
    }

    @Bean
    public Queue paymentFailedNotificationQueue() {
        return new Queue(MessagingConstants.PAYMENT_FAILED_QUEUE, true);
    }

    @Bean
    public TopicExchange orderExchange() {
        return new TopicExchange(MessagingConstants.ORDER_EXCHANGE);
    }

    @Bean
    public TopicExchange paymentExchange() {
        return new TopicExchange(MessagingConstants.PAYMENT_EXCHANGE);
    }

    @Bean
    public Binding bindOrderConfirmed() {
        return BindingBuilder
                .bind(orderConfirmedNotificationQueue())
                .to(orderExchange())
                .with(MessagingConstants.ORDER_CONFIRMED_KEY);
    }

    @Bean
    public Binding bindOrderCancelled() {
        return BindingBuilder
                .bind(orderCancelledNotificationQueue())
                .to(orderExchange())
                .with(MessagingConstants.ORDER_CANCELLED_KEY);
    }

    @Bean
    public Binding bindPaymentFailed() {
        return BindingBuilder
                .bind(paymentFailedNotificationQueue())
                .to(paymentExchange())
                .with(MessagingConstants.PAYMENT_FAILED_KEY);
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

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter());
        return factory;
    }

    @Bean
    public Queue orderConfirmedQueue() {
        return QueueBuilder.durable(MessagingConstants.ORDER_CONFIRMED_QUEUE)
                .withArgument("x-dead-letter-exchange", "notification.dlx")
                .withArgument("x-dead-letter-routing-key", "notification.dlq")
                .build();
    }
}
