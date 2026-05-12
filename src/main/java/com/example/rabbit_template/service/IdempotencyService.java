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

    // Verifica se um evento já foi processado por um listener específico
    // Retorna true se encontrar registro de processamento (sucesso ou falha)
    // Retorna false se for a primeira vez que o listener processa este evento
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
            return false;
        }
    }

    // Registra que um evento foi processado por um listener
    // Cria um novo registro em processed_events com eventId, listenerName e status
    // Permite rastreamento granular: cada listener tem seu próprio registro
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
