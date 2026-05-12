package com.example.rabbit_template.listener;

import com.example.rabbit_template.event.OrderCreatedEvent;
import com.example.rabbit_template.service.IdempotencyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import static com.example.rabbit_template.constants.RabbitConstants.*;
import static com.example.rabbit_template.constants.Status.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationListener {

    private final IdempotencyService idempotencyService;

    @RabbitListener(queues = NOTIFICATION_QUEUE)
    public void notificationListener(OrderCreatedEvent event) {
        try {
            log.info("NotificationListener.notificationListener - Start");
            boolean isAlreadyProcessed = idempotencyService.isAlreadyProcessed(event.getEventId(), NOTIFICATION_LISTENER_NAME);
            if (isAlreadyProcessed) {
                log.info("Event already processed by NotificationListener for eventId: {}", event.getEventId());
                return;
            }

            log.info("NotificationListener.notificationListener - Processing notification for eventId: {}", event.getEventId());
            idempotencyService.markAsProcessed(event.getEventId(), NOTIFICATION_LISTENER_NAME, SUCCESS.name());
            log.info("NotificationListener.notificationListener - END - eventId: {}", event.getEventId());
        } catch (Exception e) {
            log.error("Failed to listen to OrderCreatedEvent: {}", e.getMessage(), e);
            idempotencyService.markAsProcessed(event.getEventId(), NOTIFICATION_LISTENER_NAME, FAILED.name());
            throw e;
        }
    }

    @RabbitListener(queues = NOTIFICATION_DLQ_QUEUE)
    public void notificationDLQListener(OrderCreatedEvent event) {
        try {
            log.info("NotificationListener.notificationDLQListener - Start");
            boolean isAlreadyProcessed = idempotencyService.isAlreadyProcessed(event.getEventId(), NOTIFICATION_DLQ_LISTENER_NAME);
            if (isAlreadyProcessed) {
                log.info("Event already processed by NotificationDLQListener for eventId: {}", event.getEventId());
                return;
            }

            log.info("NotificationListener.notificationDLQListener - Processing notification for eventId: {}", event.getEventId());
            // Marca o evento como processado com falha por este listener (DLQ sempre é falha)
            idempotencyService.markAsProcessed(event.getEventId(), NOTIFICATION_DLQ_LISTENER_NAME, FAILED.name());
            log.info("NotificationListener.notificationDLQListener - END - eventId: {}", event.getEventId());
        } catch (Exception e) {
            log.error("Failed to listen to OrderCreatedEvent: {}", e.getMessage(), e);
            idempotencyService.markAsProcessed(event.getEventId(), NOTIFICATION_DLQ_LISTENER_NAME, FAILED.name());
            throw e;
        }
    }
}
