package com.example.rabbit_template.repository;

import com.example.rabbit_template.domain.ProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

// Repository para acessar registros de eventos processados
// Permite verificar se um evento já foi processado por um listener específico
@Repository
public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, UUID> {

    Optional<ProcessedEvent> findByEventIdAndListenerName(UUID eventId, String listenerName);
}
