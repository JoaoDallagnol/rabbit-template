package com.example.rabbit_template.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.QueueInformation;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import static com.example.rabbit_template.constants.RabbitConstants.PAYMENT_QUEUE;

@Slf4j
@Component
@RequiredArgsConstructor
public class QueueHealthMonitor {

    private final RabbitAdmin rabbitAdmin;

    @Scheduled(fixedDelay = 10000)
    public void monitorQueueHealth() {
        try {
            // Fetches information from the PAYMENT_QUEUE
            QueueInformation queueInfo = rabbitAdmin.getQueueInfo(PAYMENT_QUEUE);

            // Checks if there are 1 or more messages
            assert queueInfo != null;
            if (queueInfo.getMessageCount() > 0) {
                log.warn("PAYMENT_QUEUE with {} messages", queueInfo.getMessageCount() );
            }

            log.info("Queue health check completed");
        } catch (Exception e) {
            log.error("Error monitoring queue health: {}", e.getMessage(), e);
        }
    }
}
