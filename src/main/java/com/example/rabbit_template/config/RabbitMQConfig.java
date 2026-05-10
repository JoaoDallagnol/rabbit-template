package com.example.rabbit_template.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.example.rabbit_template.constants.RabbitConstants.*;

@Configuration
public class RabbitMQConfig {
    
    // @Bean cria um bean gerenciado pelo Spring que será injetado onde necessário
    // TopicExchange é um tipo de exchange do RabbitMQ que roteia mensagens baseado em routing keys
    // O nome "orders.exchange" é o identificador único da exchange no RabbitMQ
    @Bean
    TopicExchange createOrderExchange() {
        // Cria e retorna uma nova TopicExchange com o nome definido em RabbitConstants
        // Esta exchange será responsável por rotear mensagens OrderCreatedEvent
        return new TopicExchange(ORDER_CREATE_EXCHANGE);
    }

    // Queue é uma fila do RabbitMQ que armazena mensagens até serem consumidas
    // QueueBuilder.durable() cria uma fila durável (persiste mesmo se RabbitMQ reiniciar)
    @Bean
    Queue paymentQueue() {
        // Esta fila receberá mensagens roteadas pela exchange baseado no routing key
        return QueueBuilder.durable(PAYMENT_QUEUE).build();
    }

    // Binding vincula uma Queue a uma Exchange com um routing key específico
    // Define qual fila recebe mensagens de qual exchange com qual routing key
    @Bean
    Binding paymentBinding() {
        return BindingBuilder
                .bind(paymentQueue())
                .to(createOrderExchange())
                .with(ORDER_CREATE_KEY);
    }

    // Cria um MessageConverter que usa Jackson para converter JSON
    // Este converter será usado tanto para enviar quanto para receber mensagens
    @Bean
    public MessageConverter converter() {
        return new JacksonJsonMessageConverter();
    }

    // Configura o RabbitTemplate para usar o JacksonJsonMessageConverter
    // RabbitTemplate é responsável por enviar mensagens ao RabbitMQ
    // Sem esta configuração, o converter não seria utilizado
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        // Cria uma nova instância de RabbitTemplate com a conexão do RabbitMQ
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        // Define o MessageConverter para serializar/desserializar mensagens em JSON
        rabbitTemplate.setMessageConverter(converter());
        // Retorna o RabbitTemplate configurado
        return rabbitTemplate;
    }


    // --------------- FANOUT -------------
    // Mesma logica mas utilizando o FanoutExchange pois é do contexto de fanout
    @Bean
    FanoutExchange createOrderFanoutExchange() {
        return new FanoutExchange(ORDER_CREATE_FANOUT_EXCHANGE);
    }


    @Bean
    Queue paymentFanoutQueue() {
        return QueueBuilder.durable(PAYMENT_FANOUT_QUEUE).build();
    }

    @Bean
    Queue notificationQueue() {
        return QueueBuilder.durable(NOTIFICATION_QUEUE).build();
    }

    // Binding de fanout nao precisa de .with() pois nesse contexto n temos routing key
    @Bean
    Binding paymentFanoutBinding() {
        return BindingBuilder
                .bind(paymentFanoutQueue())
                .to(createOrderFanoutExchange());
    }

    @Bean
    Binding notificationBinding() {
        return BindingBuilder
                .bind(notificationQueue())
                .to(createOrderFanoutExchange());
    }
}
