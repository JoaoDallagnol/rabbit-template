package com.example.rabbit_template.constants;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class RabbitConstants {

    public static final String ORDER_CREATE_EXCHANGE = "orders.exchange";
    public static final String ORDER_CREATE_KEY = "orders.created";
}
