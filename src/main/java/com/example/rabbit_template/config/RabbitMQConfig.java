package com.example.rabbit_template.config;

import com.example.rabbit_template.constants.RabbitConstants;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {
    
    // @Bean cria um bean gerenciado pelo Spring que será injetado onde necessário
    // TopicExchange é um tipo de exchange do RabbitMQ que roteia mensagens baseado em routing keys
    // O nome "orders.exchange" é o identificador único da exchange no RabbitMQ
    @Bean
    TopicExchange createOrderExchange() {
        // Cria e retorna uma nova TopicExchange com o nome definido em RabbitConstants
        // Esta exchange será responsável por rotear mensagens OrderCreatedEvent
        return new TopicExchange(RabbitConstants.ORDER_CREATE_EXCHANGE);
    }
}
