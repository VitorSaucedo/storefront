package com.catalog.service.config;

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
    public TopicExchange catalogExchange() {
        return new TopicExchange(MessagingConstants.CATALOG_EXCHANGE);
    }

    @Bean
    public TopicExchange orderExchange() {
        return new TopicExchange(MessagingConstants.ORDER_EXCHANGE);
    }

    @Bean
    public TopicExchange deadLetterExchange() {
        return new TopicExchange(MessagingConstants.DEAD_LETTER_EXCHANGE);
    }

    @Bean
    public Queue productUpdatedQueue() {
        return new Queue(MessagingConstants.PRODUCT_UPDATED_QUEUE, true);
    }

    @Bean
    public Queue orderConfirmedQueue() {
        return QueueBuilder.durable(MessagingConstants.ORDER_CONFIRMED_QUEUE)
                .withArgument("x-dead-letter-exchange", MessagingConstants.DEAD_LETTER_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", MessagingConstants.ORDER_CONFIRMED_DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    public Queue orderCompensatedQueue() {
        return new Queue(MessagingConstants.ORDER_COMPENSATED_QUEUE, true);
    }

    @Bean
    public Queue orderConfirmedDlq() {
        return QueueBuilder.durable(MessagingConstants.ORDER_CONFIRMED_DLQ).build();
    }

    @Bean
    public Binding productUpdatedBinding() {
        return BindingBuilder
                .bind(productUpdatedQueue())
                .to(catalogExchange())
                .with(MessagingConstants.PRODUCT_UPDATED_ROUTING_KEY);
    }

    @Bean
    public Binding orderConfirmedBinding() {
        return BindingBuilder
                .bind(orderConfirmedQueue())
                .to(orderExchange())
                .with(MessagingConstants.ORDER_CONFIRMED_ROUTING_KEY);
    }

    @Bean
    public Binding orderCompensatedBinding() {
        return BindingBuilder
                .bind(orderCompensatedQueue())
                .to(catalogExchange())
                .with(MessagingConstants.ORDER_COMPENSATED_ROUTING_KEY);
    }

    @Bean
    public Binding orderConfirmedDlqBinding() {
        return BindingBuilder
                .bind(orderConfirmedDlq())
                .to(deadLetterExchange())
                .with(MessagingConstants.ORDER_CONFIRMED_DLQ_ROUTING_KEY);
    }

    @Bean
    public Queue orderCancelledQueue() {
        return new Queue(MessagingConstants.ORDER_CANCELLED_QUEUE, true);
    }

    @Bean
    public Binding orderCancelledBinding() {
        return BindingBuilder
                .bind(orderCancelledQueue())
                .to(orderExchange())
                .with(MessagingConstants.ORDER_CANCELLED_ROUTING_KEY);
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
        factory.setDefaultRequeueRejected(false);
        return factory;
    }
}
