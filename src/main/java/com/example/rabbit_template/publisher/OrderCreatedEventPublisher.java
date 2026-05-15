package com.example.rabbit_template.publisher;

import com.example.rabbit_template.event.OrderCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import static com.example.rabbit_template.constants.RabbitConstants.*;
import static com.example.rabbit_template.utils.JsonUtils.parseObjectToJson;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCreatedEventPublisher {

    // RabbitTemplate is the Spring AMQP class that provides methods to send messages to RabbitMQ
    private final RabbitTemplate rabbitTemplate;

    // Method responsible for publishing an OrderCreatedEvent to RabbitMQ
    // Receives an OrderCreatedEvent as a parameter
    public void publish(OrderCreatedEvent event) {
        try {
            // convertAndSend() is the main RabbitTemplate method to send messages
            // The exchange will receive the message and route it to queues based on the routing key
            // Since we configured the MessageConverter in RabbitTemplate, the content-type will be application/json
            this.rabbitTemplate.convertAndSend(ORDER_CREATE_EXCHANGE, ORDER_CREATE_KEY, event);

            log.info("OrderCreatedEventPublisher.publish - END - eventId: {}", event.getEventId());
        } catch (Exception e) {
            log.error("Failed to publish OrderCreatedEvent: {}", e.getMessage(), e);
            throw e;
        }
    }

    public void publishFanout(OrderCreatedEvent event) {
        try {
            this.rabbitTemplate.convertAndSend(ORDER_CREATE_FANOUT_EXCHANGE, "", event);
            log.info("OrderCreatedEventPublisher.publishFanout - END - eventId: {}", event.getEventId());

        } catch (Exception e) {
            log.error("Failed to publishFanout OrderCreatedEvent: {}", e.getMessage(), e);
            throw  e;
        }
    }
}
