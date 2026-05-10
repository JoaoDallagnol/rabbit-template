package com.example.rabbit_template.service;

import com.example.rabbit_template.domain.Order;
import com.example.rabbit_template.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

import static com.example.rabbit_template.constants.Status.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private final OrderRepository orderRepository;

    public boolean isAlreadyProcessed(UUID eventId, String listenerName) {
        try {
            Optional<Order> order = orderRepository.findById(eventId);

            if (order.isEmpty()) {
                log.info("Event not found in database: {}", eventId);
                return false;
            }

            Order existingOrder = order.get();
            boolean isProcessed = !existingOrder.getStatus().equals(CREATED.name());

            if (isProcessed) {
                log.info("Event already processed by {}: {} with status: {}", 
                    listenerName, eventId, existingOrder.getStatus());
            }

            return isProcessed;
        } catch (Exception e) {
            log.error("Error checking if event is already processed: {}", e.getMessage(), e);
            return false;
        }
    }

    public void markAsProcessed(UUID eventId, String listenerName, String status) {
        try {
            Optional<Order> order = orderRepository.findById(eventId);

            if (order.isEmpty()) {
                log.warn("Order not found to mark as processed: {}", eventId);
                return;
            }

            Order existingOrder = order.get();

            if (SUCCESS.name().equals(status)) {
                existingOrder.setStatus(COMPLETED.name());
            } else if (FAILED.name().equals(status)) {
                existingOrder.setStatus(FAILED.name());
            }

            orderRepository.save(existingOrder);

            log.info("Event marked as processed by {}: {} with status: {}", 
                listenerName, eventId, existingOrder.getStatus());
        } catch (Exception e) {
            log.error("Error marking event as processed: {}", e.getMessage(), e);
            throw e;
        }
    }
}
