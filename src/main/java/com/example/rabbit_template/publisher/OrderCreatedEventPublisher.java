package com.example.rabbit_template.publisher;

import com.example.rabbit_template.event.OrderCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import static com.example.rabbit_template.constants.RabbitConstants.ORDER_CREATE_EXCHANGE;
import static com.example.rabbit_template.constants.RabbitConstants.ORDER_CREATE_KEY;
import static com.example.rabbit_template.utils.JsonUtils.parseObjectToJson;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCreatedEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publish(OrderCreatedEvent event) {
        try {
            String payloadToSend = parseObjectToJson(event);

            this.rabbitTemplate.convertAndSend(ORDER_CREATE_EXCHANGE, ORDER_CREATE_KEY, payloadToSend);
        } catch (Exception e) {
            log.error("Failed to publish OrderCreatedEvent: {}", e.getMessage(), e);
            throw e;
        }
    }
}
