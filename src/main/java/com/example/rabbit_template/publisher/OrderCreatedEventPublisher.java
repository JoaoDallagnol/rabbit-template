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

    // RabbitTemplate é a classe do Spring AMQP que fornece métodos para enviar mensagens ao RabbitMQ
    private final RabbitTemplate rabbitTemplate;

    // Metodo responsável por publicar um evento OrderCreatedEvent no RabbitMQ
    // Recebe um OrderCreatedEvent como parâmetro
    public void publish(OrderCreatedEvent event) {
        try {
            // parseObjectToJson() converte o objeto OrderCreatedEvent em uma String JSON
            //String payloadToSend = parseObjectToJson(event);

            // convertAndSend() é o metodo principal do RabbitTemplate para enviar mensagens
            // payloadToSend - o conteúdo da mensagem em formato JSON
            // A exchange receberá a mensagem e a roteará para filas baseado no routing key
            // Como configuramos o MessageConverter no RabbitTemplate, o content-type será application/json
            this.rabbitTemplate.convertAndSend(ORDER_CREATE_EXCHANGE, ORDER_CREATE_KEY, event);

            log.info("OrderCreatedEventPublisher.publish - END - eventId: {}", event.getEventId());
        } catch (Exception e) {
            // Se ocorrer qualquer exceção durante a publicação, registra um erro no log
            // log.error() escreve a mensagem de erro no arquivo de log da aplicação
            log.error("Failed to publish OrderCreatedEvent: {}", e.getMessage(), e);
            // Relança a exceção para que o chamador (OrderService) saiba que houve falha
            throw e;
        }
    }
}
