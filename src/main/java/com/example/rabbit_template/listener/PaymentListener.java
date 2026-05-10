package com.example.rabbit_template.listener;

import com.example.rabbit_template.domain.Order;
import com.example.rabbit_template.event.OrderCreatedEvent;
import com.example.rabbit_template.mapper.OrderMapper;
import com.example.rabbit_template.repository.OrderRepository;
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
public class PaymentListener {

    private final OrderRepository orderRepository;
    private final OrderMapper mapper;
    private final IdempotencyService idempotencyService;

    // @RabbitListener marca este metodo como um listener de mensagens RabbitMQ
    // queues = PAYMENT_QUEUE especifica qual fila este listener consome
    // O metodo será chamado automaticamente quando uma mensagem chegar na fila
    @RabbitListener(queues = PAYMENT_QUEUE)
    public void paymentTopicListener(OrderCreatedEvent event) {
        try {
            log.info("PaymentListener.paymentTopicListener - Start");
            boolean isAlreadyProcessed =  idempotencyService.isAlreadyProcessed(event.getOrderId(), PAYMENT_LISTENER_NAME);
            if (isAlreadyProcessed) {
                log.info("Event already processed by PaymentListener for orderId: {}", event.getOrderId());
                return;
            }

            event.setStatus(PROCESSING.name());
            Order order = mapper.toOrder(event);
            orderRepository.save(order);

            idempotencyService.markAsProcessed(event.getOrderId(), PAYMENT_LISTENER_NAME, SUCCESS.name());
            log.info("PaymentListener.paymentTopicListener - END - status: {}", event.getStatus());
        } catch (Exception e) {
            log.error("Failed to listen to  OrderCreatedEvent: {}", e.getMessage(), e);
            idempotencyService.markAsProcessed(event.getOrderId(), PAYMENT_LISTENER_NAME, FAILED.name());
            throw e;
        }
    }

    @RabbitListener(queues = PAYMENT_FANOUT_QUEUE)
    public void paymentFanoutListener(OrderCreatedEvent event) {
        try {
            log.info("PaymentListener.paymentFanoutListener - Start");
            boolean isAlreadyProcessed =  idempotencyService.isAlreadyProcessed(event.getOrderId(), PAYMENT_FANOUT_LISTENER_NAME);
            if (isAlreadyProcessed) {
                log.info("Event already processed by PaymentFanoutListener for orderId: {}", event.getOrderId());
                return;
            }

            event.setStatus(PROCESSING.name());
            Order order = mapper.toOrder(event);
            orderRepository.save(order);

            idempotencyService.markAsProcessed(event.getOrderId(), PAYMENT_FANOUT_LISTENER_NAME, SUCCESS.name());
            log.info("PaymentListener.paymentFanoutListener - END - status: {}", event.getStatus());
        } catch (Exception e) {
            log.error("Failed to listen to  OrderCreatedEvent: {}", e.getMessage(), e);
            idempotencyService.markAsProcessed(event.getOrderId(), PAYMENT_FANOUT_LISTENER_NAME, FAILED.name());
            throw e;
        }
    }
}
