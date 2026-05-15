package com.example.rabbit_template.service;

import com.example.rabbit_template.domain.ProcessedEvent;
import com.example.rabbit_template.repository.ProcessedEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private final ProcessedEventRepository processedEventRepository;

    // Checks if an event has already been processed by a specific listener
    // Returns true if it finds a processing record (success or failure)
    // Returns false if this is the first time the listener processes this event
    public boolean isAlreadyProcessed(UUID eventId, String listenerName) {
        try {
            Optional<ProcessedEvent> processedEvent = processedEventRepository
                .findByEventIdAndListenerName(eventId, listenerName);

            if (processedEvent.isPresent()) {
                log.info("Event already processed by {}: {} with status: {}",
                    listenerName, eventId, processedEvent.get().getStatus());
                return true;
            }

            return false;
        } catch (Exception e) {
            log.error("Error checking if event is already processed: {}", e.getMessage(), e);
            throw e;
        }
    }

    // Records that an event has been processed by a listener
    // Creates a new record in processed_events with eventId, listenerName and status
    // Enables granular tracking: each listener has its own record
    public void markAsProcessed(UUID eventId, String listenerName, String status) {
        try {
            ProcessedEvent processedEvent = ProcessedEvent.builder()
                .eventId(eventId)
                .listenerName(listenerName)
                .processedAt(LocalDateTime.now())
                .status(status)
                .build();

            processedEventRepository.save(processedEvent);

            log.info("Event marked as processed by {}: {} with status: {}",
                listenerName, eventId, status);
        } catch (Exception e) {
            log.error("Error marking event as processed: {}", e.getMessage(), e);
            throw e;
        }
    }
}
