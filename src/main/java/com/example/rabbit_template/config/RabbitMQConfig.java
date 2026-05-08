package com.example.rabbit_template.config;

import com.example.rabbit_template.constants.RabbitConstants;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {
    @Bean
    TopicExchange createOrderExchange() {
        return new TopicExchange(RabbitConstants.ORDER_CREATE_EXCHANGE);
    }
}
