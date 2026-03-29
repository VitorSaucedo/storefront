package com.order.service.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Bean
    public TopicExchange orderExchange() {
        return new TopicExchange(MessagingConstants.ORDER_EXCHANGE);
    }

    @Bean
    public TopicExchange paymentExchange() {
        return new TopicExchange(MessagingConstants.PAYMENT_EXCHANGE);
    }

    @Bean
    public Queue orderCreatedQueue() {
        return new Queue(MessagingConstants.ORDER_CREATED_QUEUE, true);
    }

    @Bean
    public Queue orderConfirmedQueue() {
        return new Queue(MessagingConstants.ORDER_CONFIRMED_QUEUE, true);
    }

    @Bean
    public Queue orderCancelledQueue() {
        return new Queue(MessagingConstants.ORDER_CANCELLED_QUEUE, true);
    }

    @Bean
    public Queue paymentProcessedQueue() {
        return new Queue(MessagingConstants.PAYMENT_PROCESSED_QUEUE, true);
    }

    @Bean
    public Queue paymentFailedQueue() {
        return new Queue(MessagingConstants.PAYMENT_FAILED_QUEUE, true);
    }

    @Bean
    public Binding orderCreatedBinding() {
        return BindingBuilder
                .bind(orderCreatedQueue())
                .to(orderExchange())
                .with(MessagingConstants.ORDER_CREATED_ROUTING_KEY);
    }

    @Bean
    public Binding orderConfirmedBinding() {
        return BindingBuilder
                .bind(orderConfirmedQueue())
                .to(orderExchange())
                .with(MessagingConstants.ORDER_CONFIRMED_ROUTING_KEY);
    }

    @Bean
    public Binding orderCancelledBinding() {
        return BindingBuilder
                .bind(orderCancelledQueue())
                .to(orderExchange())
                .with(MessagingConstants.ORDER_CANCELLED_ROUTING_KEY);
    }

    @Bean
    public Binding paymentProcessedBinding() {
        return BindingBuilder
                .bind(paymentProcessedQueue())
                .to(paymentExchange())
                .with(MessagingConstants.PAYMENT_PROCESSED_ROUTING_KEY);
    }

    @Bean
    public Binding paymentFailedBinding() {
        return BindingBuilder
                .bind(paymentFailedQueue())
                .to(paymentExchange())
                .with(MessagingConstants.PAYMENT_FAILED_ROUTING_KEY);
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
}