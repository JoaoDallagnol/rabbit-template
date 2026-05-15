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
    
    // @Bean creates a Spring-managed bean that will be injected where needed
    // TopicExchange is a RabbitMQ exchange type that routes messages based on routing keys
    // The name "orders.exchange" is the unique identifier of the exchange in RabbitMQ
    @Bean
    TopicExchange createOrderExchange() {
        // Creates and returns a new TopicExchange with the name defined in RabbitConstants
        // This exchange will be responsible for routing OrderCreatedEvent messages
        return new TopicExchange(ORDER_CREATE_EXCHANGE);
    }

    // Queue is a RabbitMQ queue that stores messages until they are consumed
    // QueueBuilder.durable() creates a durable queue (persists even if RabbitMQ restarts)
    @Bean
    Queue paymentQueue() {
        // This queue will receive messages routed by the exchange based on the routing key
        return QueueBuilder.durable(PAYMENT_QUEUE).build();
    }

    // Binding links a Queue to an Exchange with a specific routing key
    // Defines which queue receives messages from which exchange with which routing key
    @Bean
    Binding paymentBinding() {
        return BindingBuilder
                .bind(paymentQueue())
                .to(createOrderExchange())
                .with(ORDER_CREATE_KEY);
    }

    // Creates a MessageConverter that uses Jackson to convert JSON
    // This converter will be used for both sending and receiving messages
    @Bean
    public MessageConverter converter() {
        return new JacksonJsonMessageConverter();
    }

    // Configures the RabbitTemplate to use the JacksonJsonMessageConverter
    // RabbitTemplate is responsible for sending messages to RabbitMQ
    // Without this configuration, the converter would not be used
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        // Creates a new RabbitTemplate instance with the RabbitMQ connection
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        // Sets the MessageConverter to serialize/deserialize messages as JSON
        rabbitTemplate.setMessageConverter(converter());
        // Returns the configured RabbitTemplate
        return rabbitTemplate;
    }


    // --------------- FANOUT EXCHANGE ---------------
    // Same logic but using FanoutExchange for the fanout context
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

    // Fanout binding does not need .with() because in this context we don't have a routing key
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

    // ------------------- RETRY MECHANISM -------------------
    // TopicExchange for retry in the Topic flow (Payment Topic)
    // Stores messages that failed and waits for the configured delay before reprocessing
    // Uses Form 1: deadLetterExchange points to the original exchange (ORDER_CREATE_EXCHANGE)
    // When the message is reprocessed, it goes back to the main queue
    @Bean
    TopicExchange createOrderRetryExchange() {
        return new TopicExchange(ORDER_CREATE_RETRY_EXCHANGE);
    }

    // FanoutExchange for retry in the Fanout flow (Payment Fanout and Notification)
    // When the message is reprocessed, it goes back to the main queues (Payment Fanout and Notification)
    @Bean
    FanoutExchange createOrderFanoutRetryExchange() {
        return new FanoutExchange(ORDER_CREATE_FANOUT_RETRY_EXCHANGE);
    }

    // Retry queue for Payment (Topic): stores messages that failed in the main queue
    // deadLetterExchange points to ORDER_CREATE_EXCHANGE (original exchange)
    // deadLetterRoutingKey specifies the routing key for reprocessing (Form 1)
    // After the delay, the message goes back to the main queue via the original exchange
    @Bean
    Queue paymentRetryQueue () {
        return QueueBuilder.durable(PAYMENT_RETRY_QUEUE)
                .deadLetterExchange(ORDER_CREATE_EXCHANGE)
                .deadLetterRoutingKey(ORDER_CREATE_KEY)
                .build();
    }

    // Retry queue for Payment (Fanout): stores messages that failed in the main queue
    // deadLetterExchange points to ORDER_CREATE_FANOUT_EXCHANGE (original exchange)
    // No deadLetterRoutingKey because it's fanout and doesn't need a routing key (Form 1)
    // After the delay, the message goes back to the main queue via the original exchange
    @Bean
    Queue paymentFanoutRetryQueue() {
        return QueueBuilder.durable(PAYMENT_FANOUT_RETRY_QUEUE)
                .deadLetterExchange(ORDER_CREATE_FANOUT_EXCHANGE)
                .build();
    }

    // Binding of the Payment retry queue (Topic) with the retry exchange (TopicExchange)
    // With .with(ORDER_CREATE_KEY) because it's topic and needs a routing key
    // Messages that fail in the main queue are sent to this retry queue
    @Bean
    Binding paymentRetryBinding() {
        return BindingBuilder
                .bind(paymentRetryQueue())
                .to(createOrderRetryExchange())
                .with(ORDER_CREATE_KEY);
    }

    // Binding of the Payment retry queue (Fanout) with the retry exchange (FanoutExchange)
    // Without .with() because it's fanout and doesn't need a routing key
    // Messages that fail in the main queue are sent to this retry queue
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

    // ------------------- DEAD LETTER QUEUE (DLQ) FOR NOTIFICATION -------------------
    // DLQ is a specific exchange that receives messages that failed after all retries
    // Different from Payment which uses Form 1 (retry goes back to original exchange)
    // Notification uses Form 2 (retry goes to specific DLQ for permanent storage)

    // Specific FanoutExchange for DLQ: receives messages that failed permanently
    @Bean
    FanoutExchange createNotificationDLQExchange() {
        return new FanoutExchange(NOTIFICATION_DLQ_EXCHANGE);
    }

    // DLQ queue: stores messages that failed after all retries
    // These messages will not be reprocessed automatically, only stored for analysis
    @Bean
    Queue notificationDLQQueue() {
        return QueueBuilder.durable(NOTIFICATION_DLQ_QUEUE).build();
    }

    // Binding of the DLQ queue with the DLQ exchange (FanoutExchange)
    // Without .with() because it's fanout and doesn't need a routing key
    // Messages that arrive here have already failed and need manual intervention
    @Bean
    Binding notificationDLQBinding() {
        return BindingBuilder
                .bind(notificationDLQQueue())
                .to(createNotificationDLQExchange());
    }

    // RabbitAdmin provides administrative operations on RabbitMQ
    // Used to monitor queues, exchanges and other management operations
    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        return new RabbitAdmin(connectionFactory);
    }

}
