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

    public boolean isAlreadyProcessed(UUID id, String listenerName) {
        try {
            Optional<Order> order = orderRepository.findById(id);

            if (order.isEmpty()) {
                log.info("Order not found in database: {}", id);
                return false;
            }

            Order existingOrder = order.get();
            boolean isProcessed = !existingOrder.getStatus().equals(CREATED.name());

            if (isProcessed) {
                log.info("Order already processed by {}: {} with status: {}",
                    listenerName, id, existingOrder.getStatus());
            }

            return isProcessed;
        } catch (Exception e) {
            log.error("Error checking if event is already processed: {}", e.getMessage(), e);
            return false;
        }
    }

    public void markAsProcessed(UUID id, String listenerName, String status) {
        try {
            Optional<Order> order = orderRepository.findById(id);

            if (order.isEmpty()) {
                log.warn("Order not found to mark as processed: {}", id);
                return;
            }

            Order existingOrder = order.get();

            if (SUCCESS.name().equals(status)) {
                existingOrder.setStatus(COMPLETED.name());
            } else if (FAILED.name().equals(status)) {
                existingOrder.setStatus(FAILED.name());
            }

            orderRepository.save(existingOrder);

            log.info("Order marked as processed by {}: {} with status: {}",
                listenerName, id, existingOrder.getStatus());
        } catch (Exception e) {
            log.error("Error marking event as processed: {}", e.getMessage(), e);
            throw e;
        }
    }
}
