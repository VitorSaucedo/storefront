package com.notification.service.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.json.JsonMapper;

@Configuration
public class RabbitMQConfig {

    public static final String ORDER_CONFIRMED_QUEUE    = "notification.order.confirmed";
    public static final String ORDER_CANCELLED_QUEUE    = "notification.order.cancelled";
    public static final String PAYMENT_FAILED_QUEUE     = "notification.payment.failed";

    public static final String ORDER_EXCHANGE   = "order.exchange";
    public static final String PAYMENT_EXCHANGE = "payment.exchange";

    public static final String ORDER_CONFIRMED_KEY  = "order.confirmed";
    public static final String ORDER_CANCELLED_KEY  = "order.cancelled";
    public static final String PAYMENT_FAILED_KEY   = "payment.failed";

    @Bean
    public Queue orderConfirmedNotificationQueue() {
        return new Queue(ORDER_CONFIRMED_QUEUE, true);
    }

    @Bean
    public Queue orderCancelledNotificationQueue() {
        return new Queue(ORDER_CANCELLED_QUEUE, true);
    }

    @Bean
    public Queue paymentFailedNotificationQueue() {
        return new Queue(PAYMENT_FAILED_QUEUE, true);
    }

    @Bean
    public TopicExchange orderExchange() {
        return new TopicExchange(ORDER_EXCHANGE);
    }

    @Bean
    public TopicExchange paymentExchange() {
        return new TopicExchange(PAYMENT_EXCHANGE);
    }

    @Bean
    public Binding bindOrderConfirmed() {
        return BindingBuilder
                .bind(orderConfirmedNotificationQueue())
                .to(orderExchange())
                .with(ORDER_CONFIRMED_KEY);
    }

    @Bean
    public Binding bindOrderCancelled() {
        return BindingBuilder
                .bind(orderCancelledNotificationQueue())
                .to(orderExchange())
                .with(ORDER_CANCELLED_KEY);
    }

    @Bean
    public Binding bindPaymentFailed() {
        return BindingBuilder
                .bind(paymentFailedNotificationQueue())
                .to(paymentExchange())
                .with(PAYMENT_FAILED_KEY);
    }

    @Bean
    public JacksonJsonMessageConverter messageConverter() {
        return new JacksonJsonMessageConverter(JsonMapper.builder().build());
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
