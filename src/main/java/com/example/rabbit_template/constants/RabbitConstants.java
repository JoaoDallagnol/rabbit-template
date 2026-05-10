package com.example.rabbit_template.constants;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class RabbitConstants {

    //Topic
    public static final String ORDER_CREATE_EXCHANGE = "orders.exchange";
    public static final String ORDER_CREATE_KEY = "orders.created";
    public static final String PAYMENT_QUEUE = "payment.queue.topic";
    public static final String PAYMENT_LISTENER_NAME = "PaymentListener";

    //Fanout
    public static final String ORDER_CREATE_FANOUT_EXCHANGE = "orders.fanout.exchange";
    public static final String PAYMENT_FANOUT_QUEUE = "payment.queue.fanout";
    public static final String PAYMENT_FANOUT_LISTENER_NAME = "PaymentFanoutListener";
    public static final String NOTIFICATION_QUEUE = "notification.queue.fanout";
    public static final String NOTIFICATION_LISTENER_NAME = "NotificationListener";
}
