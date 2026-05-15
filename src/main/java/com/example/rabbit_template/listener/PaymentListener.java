package com.example.rabbit_template.listener;

import com.example.rabbit_template.domain.Order;
import com.example.rabbit_template.event.OrderCreatedEvent;
import com.example.rabbit_template.mapper.OrderMapper;
import com.example.rabbit_template.repository.OrderRepository;
import com.example.rabbit_template.service.IdempotencyService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import static com.example.rabbit_template.constants.RabbitConstants.*;
import static com.example.rabbit_template.constants.Status.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentListener {

    private final OrderRepository orderRepository;
    private final OrderMapper mapper;
    private final IdempotencyService idempotencyService;

    // @RabbitListener marks this method as a RabbitMQ message listener
    // queues = PAYMENT_QUEUE specifies which queue this listener consumes from
    // The method will be called automatically when a message arrives in the queue
    @RabbitListener(queues = PAYMENT_QUEUE)
    @Transactional
    public void paymentTopicListener(OrderCreatedEvent event) {
        try {
            log.info("PaymentListener.paymentTopicListener - Start");
            // Checks if this listener has already processed this event (idempotency per listener)
            boolean isAlreadyProcessed =  idempotencyService.isAlreadyProcessed(event.getEventId(), PAYMENT_LISTENER_NAME);
            if (isAlreadyProcessed) {
                log.info("Event already processed by PaymentListener for eventId: {}", event.getEventId());
                return;
            }

            event.setStatus(PROCESSING.name());
            Order order = mapper.toOrder(event);
            orderRepository.save(order);
            // Marks the event as successfully processed by this listener
            idempotencyService.markAsProcessed(event.getEventId(), PAYMENT_LISTENER_NAME, SUCCESS.name());
            log.info("PaymentListener.paymentTopicListener - END - status: {}", event.getStatus());
        } catch (Exception e) {
            log.error("Failed to listen to  OrderCreatedEvent: {}", e.getMessage(), e);
            // Marks the event as failed by this listener
            idempotencyService.markAsProcessed(event.getEventId(), PAYMENT_LISTENER_NAME, FAILED.name());
            throw e;
        }
    }

    @RabbitListener(queues = PAYMENT_FANOUT_QUEUE)
    @Transactional
    public void paymentFanoutListener(OrderCreatedEvent event) {
        try {
            log.info("PaymentListener.paymentFanoutListener - Start");
            // Checks if this listener has already processed this event (idempotency per listener)
            boolean isAlreadyProcessed =  idempotencyService.isAlreadyProcessed(event.getEventId(), PAYMENT_FANOUT_LISTENER_NAME);
            if (isAlreadyProcessed) {
                log.info("Event already processed by PaymentFanoutListener for eventId: {}", event.getEventId());
                return;
            }

            event.setStatus(PROCESSING.name());
            Order order = mapper.toOrder(event);
            orderRepository.save(order);
            // Marks the event as successfully processed by this listener
            idempotencyService.markAsProcessed(event.getEventId(), PAYMENT_FANOUT_LISTENER_NAME, SUCCESS.name());
            log.info("PaymentListener.paymentFanoutListener - END - status: {}", event.getStatus());
        } catch (Exception e) {
            log.error("Failed to listen to  OrderCreatedEvent: {}", e.getMessage(), e);
            // Marks the event as failed by this listener
            idempotencyService.markAsProcessed(event.getEventId(), PAYMENT_FANOUT_LISTENER_NAME, FAILED.name());
            throw e;
        }
    }
}
