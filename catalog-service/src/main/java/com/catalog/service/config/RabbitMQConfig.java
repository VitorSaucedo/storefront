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

    public static final String CATALOG_EXCHANGE = "catalog.exchange";

    public static final String PRODUCT_UPDATED_QUEUE      = "product.updated.queue";
    public static final String PRODUCT_UPDATED_ROUTING_KEY = "product.updated";

    public static final String ORDER_COMPENSATED_QUEUE       = "order.compensated.queue";
    public static final String ORDER_COMPENSATED_ROUTING_KEY = "order.compensated";

    public static final String ORDER_CONFIRMED_QUEUE       = "order.confirmed.queue";
    public static final String ORDER_EXCHANGE              = "order.exchange";
    public static final String ORDER_CONFIRMED_ROUTING_KEY = "order.confirmed";

    public static final String ORDER_CONFIRMED_DLQ        = "order.confirmed.queue.dlq";
    public static final String DEAD_LETTER_EXCHANGE       = "catalog.dlx";
    public static final String ORDER_CONFIRMED_DLQ_ROUTING_KEY = "order.confirmed.dead";

    @Bean
    public TopicExchange catalogExchange() {
        return new TopicExchange(CATALOG_EXCHANGE);
    }

    @Bean
    public TopicExchange orderExchange() {
        return new TopicExchange(ORDER_EXCHANGE);
    }

    @Bean
    public TopicExchange deadLetterExchange() {
        return new TopicExchange(DEAD_LETTER_EXCHANGE);
    }

    @Bean
    public Queue productUpdatedQueue() {
        return new Queue(PRODUCT_UPDATED_QUEUE, true);
    }

    @Bean
    public Queue orderConfirmedQueue() {
        return QueueBuilder.durable(ORDER_CONFIRMED_QUEUE)
                .withArgument("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", ORDER_CONFIRMED_DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    public Queue orderCompensatedQueue() {
        return new Queue(ORDER_COMPENSATED_QUEUE, true);
    }

    @Bean
    public Queue orderConfirmedDlq() {
        return QueueBuilder.durable(ORDER_CONFIRMED_DLQ).build();
    }

    @Bean
    public Binding productUpdatedBinding() {
        return BindingBuilder
                .bind(productUpdatedQueue())
                .to(catalogExchange())
                .with(PRODUCT_UPDATED_ROUTING_KEY);
    }

    @Bean
    public Binding orderConfirmedBinding() {
        return BindingBuilder
                .bind(orderConfirmedQueue())
                .to(orderExchange())
                .with(ORDER_CONFIRMED_ROUTING_KEY);
    }

    @Bean
    public Binding orderCompensatedBinding() {
        return BindingBuilder
                .bind(orderCompensatedQueue())
                .to(catalogExchange())
                .with(ORDER_COMPENSATED_ROUTING_KEY);
    }

    @Bean
    public Binding orderConfirmedDlqBinding() {
        return BindingBuilder
                .bind(orderConfirmedDlq())
                .to(deadLetterExchange())
                .with(ORDER_CONFIRMED_DLQ_ROUTING_KEY);
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
