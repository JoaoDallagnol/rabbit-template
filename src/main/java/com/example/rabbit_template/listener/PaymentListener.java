package com.example.rabbit_template.listener;

import com.example.rabbit_template.event.OrderCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import static com.example.rabbit_template.constants.RabbitConstants.PAYMENT_QUEUE;
import static com.example.rabbit_template.constants.Status.PROCESSING;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentListener {


    // @RabbitListener marca este metodo como um listener de mensagens RabbitMQ
    // queues = PAYMENT_QUEUE especifica qual fila este listener consome
    // O metodo será chamado automaticamente quando uma mensagem chegar na fila
    @RabbitListener(queues = PAYMENT_QUEUE)
    public void paymentTopicListener(OrderCreatedEvent event) {
        try {
            log.info("PaymentListener.paymentTopicListener - Start");
            event.setStatus(PROCESSING.name());
            log.info("PaymentListener.paymentTopicListener - END - status: {}", event.getStatus());
        } catch (Exception e) {
            log.error("Failed to listen to  OrderCreatedEvent: {}", e.getMessage(), e);
            throw e;
        }

    }
}
