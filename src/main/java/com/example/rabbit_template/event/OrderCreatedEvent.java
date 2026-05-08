package com.example.rabbit_template.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class OrderCreatedEvent {
    private UUID eventId;
    private String eventType;
    private String version;
    private LocalDateTime occurredAt;
    private UUID orderId;
    private String customerId;
    private Double amount;
    private String status;
}
