package com.example.rabbit_template.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
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

    // ------------------ RETRY ------------------
    // TopicExchange de retry para o fluxo de Topic (Payment Topic)
    // Armazena mensagens que falharam e aguarda o delay configurado antes de reprocessar
    // Usa Forma 1: deadLetterExchange aponta para a exchange original (ORDER_CREATE_EXCHANGE)
    // Quando a mensagem é reprocessada, volta para a fila principal
    @Bean
    TopicExchange createOrderRetryExchange() {
        return new TopicExchange(ORDER_CREATE_RETRY_EXCHANGE);
    }

    // FanoutExchange de retry para o fluxo de Fanout (Payment Fanout e Notification)
    // Quando a mensagem é reprocessada, volta para as filas principais (Payment Fanout e Notification)
    @Bean
    FanoutExchange createOrderFanoutRetryExchange() {
        return new FanoutExchange(ORDER_CREATE_FANOUT_RETRY_EXCHANGE);
    }

    // Fila de retry para Payment (Topic): armazena mensagens que falharam na fila principal
    // deadLetterExchange aponta para ORDER_CREATE_EXCHANGE (exchange original)
    // deadLetterRoutingKey especifica o routing key para reprocessamento (Forma 1)
    // Após o delay, a mensagem volta para a fila principal via exchange original
    @Bean
    Queue paymentRetryQueue () {
        return QueueBuilder.durable(PAYMENT_RETRY_QUEUE)
                .deadLetterExchange(ORDER_CREATE_EXCHANGE)
                .deadLetterRoutingKey(ORDER_CREATE_KEY)
                .build();
    }

    // Fila de retry para Payment (Fanout): armazena mensagens que falharam na fila principal
    // deadLetterExchange aponta para ORDER_CREATE_FANOUT_EXCHANGE (exchange original)
    // Sem deadLetterRoutingKey pois é fanout e não precisa de routing key (Forma 1)
    // Após o delay, a mensagem volta para a fila principal via exchange original
    @Bean
    Queue paymentFanoutRetryQueue() {
        return QueueBuilder.durable(PAYMENT_FANOUT_RETRY_QUEUE)
                .deadLetterExchange(ORDER_CREATE_FANOUT_EXCHANGE)
                .build();
    }

    // Binding da fila de retry do Payment (Topic) com a retry exchange (TopicExchange)
    // Com .with(ORDER_CREATE_KEY) pois é topic e precisa de routing key
    // Mensagens que falham na fila principal são enviadas para esta fila de retry
    @Bean
    Binding paymentRetryBinding() {
        return BindingBuilder
                .bind(paymentRetryQueue())
                .to(createOrderRetryExchange())
                .with(ORDER_CREATE_KEY);
    }

    // Binding da fila de retry do Payment (Fanout) com a retry exchange (FanoutExchange)
    // Sem .with() pois é fanout e não precisa de routing key
    // Mensagens que falham na fila principal são enviadas para esta fila de retry
    @Bean
    Binding paymentFanoutRetryBinding() {
        return BindingBuilder
                .bind(paymentFanoutRetryQueue())
                .to(createOrderFanoutRetryExchange());
    }

    @Bean
    Queue notificationRetryQueue() {
        return QueueBuilder.durable(NOTIFICATION_RETRY_QUEUE)
                .deadLetterExchange(NOTIFICATION_DLQ_EXCHANGE)
                .build();
    }

    @Bean
    Binding notificationRetryBinding() {
        return BindingBuilder
                .bind(notificationRetryQueue())
                .to(createOrderFanoutRetryExchange());
    }

    // --------------- Dead Letter Queue (DLQ) para Notification ------------
    // DLQ é uma exchange específica que recebe mensagens que falharam após todos os retries
    // Diferente do Payment que usa Forma 1 (retry volta para exchange original)
    // Notification usa Forma 2 (retry vai para DLQ específica para armazenamento permanente)

    // FanoutExchange específica para DLQ: recebe mensagens que falharam permanentemente
    @Bean
    FanoutExchange createNotificationDLQExchange() {
        return new FanoutExchange(NOTIFICATION_DLQ_EXCHANGE);
    }

    // Fila de DLQ: armazena mensagens que falharam após todos os retries
    // Estas mensagens não serão reprocessadas automaticamente, apenas armazenadas para análise
    @Bean
    Queue notificationDLQQueue() {
        return QueueBuilder.durable(NOTIFICATION_DLQ_QUEUE).build();
    }

    // Binding da fila de DLQ com a DLQ exchange (FanoutExchange)
    // Sem .with() pois é fanout e não precisa de routing key
    // Mensagens que chegam aqui já falharam e precisam de intervenção manual
    @Bean
    Binding notificationDLQBinding() {
        return BindingBuilder
                .bind(notificationDLQQueue())
                .to(createNotificationDLQExchange());
    }

    // RabbitAdmin fornece operações administrativas no RabbitMQ
    // Usado para monitorar filas, exchanges e outras operações de gerenciamento
    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        return new RabbitAdmin(connectionFactory);
    }

}
