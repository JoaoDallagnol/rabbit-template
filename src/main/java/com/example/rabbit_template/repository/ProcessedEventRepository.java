package com.example.rabbit_template.repository;

import com.example.rabbit_template.domain.ProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

// Repository to access processed event records
// Allows checking if an event has already been processed by a specific listener
@Repository
public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, UUID> {

    Optional<ProcessedEvent> findByEventIdAndListenerName(UUID eventId, String listenerName);
}
