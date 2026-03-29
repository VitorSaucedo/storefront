package com.catalog.service.config;

public final class MessagingConstants {

    private MessagingConstants() {}

    // Exchanges
    public static final String CATALOG_EXCHANGE = "catalog.exchange";
    public static final String ORDER_EXCHANGE = "order.exchange";
    public static final String DEAD_LETTER_EXCHANGE = "catalog.dlx";

    // Queues
    public static final String PRODUCT_UPDATED_QUEUE = "product.updated.queue";
    public static final String ORDER_COMPENSATED_QUEUE = "order.compensated.queue";
    public static final String ORDER_CONFIRMED_QUEUE = "order.confirmed.queue";
    public static final String ORDER_CONFIRMED_DLQ = "order.confirmed.queue.dlq";

    // Routing Keys
    public static final String PRODUCT_UPDATED_ROUTING_KEY = "product.updated";
    public static final String ORDER_COMPENSATED_ROUTING_KEY = "order.compensated";
    public static final String ORDER_CONFIRMED_ROUTING_KEY = "order.confirmed";
    public static final String ORDER_CONFIRMED_DLQ_ROUTING_KEY = "order.confirmed.dead";
}
