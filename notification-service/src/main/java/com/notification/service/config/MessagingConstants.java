package com.notification.service.config;

public final class MessagingConstants {

    private MessagingConstants() {}

    public static final String ORDER_CONFIRMED_QUEUE    = "notification.order.confirmed";
    public static final String ORDER_CANCELLED_QUEUE    = "notification.order.cancelled";
    public static final String PAYMENT_FAILED_QUEUE     = "notification.payment.failed";

    public static final String ORDER_EXCHANGE   = "order.exchange";
    public static final String PAYMENT_EXCHANGE = "payment.exchange";

    public static final String ORDER_CONFIRMED_KEY  = "order.confirmed";
    public static final String ORDER_CANCELLED_KEY  = "order.cancelled";
    public static final String PAYMENT_FAILED_KEY   = "payment.failed";

}