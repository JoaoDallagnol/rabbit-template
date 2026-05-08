package com.example.rabbit_template.service;

import com.example.rabbit_template.constants.EventType;
import com.example.rabbit_template.domain.Order;
import com.example.rabbit_template.domain.OrderItem;
import com.example.rabbit_template.dto.OrderRequest;
import com.example.rabbit_template.dto.OrderResponse;
import com.example.rabbit_template.event.OrderCreatedEvent;
import com.example.rabbit_template.exception.OrderNotFoundException;
import com.example.rabbit_template.mapper.OrderMapper;
import com.example.rabbit_template.publisher.OrderCreatedEventPublisher;
import com.example.rabbit_template.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final OrderCreatedEventPublisher orderCreatedEventPublisher;

    public OrderResponse createOrder(OrderRequest request) {
        List<OrderItem> items = request.getItems().stream()
                .map(orderMapper::toOrderItem)
                .collect(Collectors.toList());

        Order order = Order.builder()
                .customerId(request.getCustomerId())
                .amount(request.getAmount())
                .status("CREATED")
                .createdAt(LocalDateTime.now())
                .items(items)
                .build();

        items.forEach(item -> item.setOrder(order));

        Order savedOrder = orderRepository.save(order);

        // TODO: Build OrderCreatedEvent (eventId, eventType, version, occurredAt, orderId, customerId, amount, status)
        OrderCreatedEvent event = new OrderCreatedEvent(
                UUID.randomUUID(),
                EventType.ORDER_CREATED.name(),
                "v1",
                savedOrder.getCreatedAt(),
                savedOrder.getOrderId(),
                savedOrder.getCustomerId(),
                savedOrder.getAmount(),
                savedOrder.getStatus()
        );

        // TODO: Publish OrderCreatedEvent to RabbitMQ exchange "orders.exchange" with routing key "order.created"
        orderCreatedEventPublisher.publish(event);

        // TODO: PaymentListener and NotificationListener will each receive a copy via their bound queues (fanout)
        // TODO: Implement retry with exponential backoff and Dead Letter Queue (DLQ) on consumer failure
        // TODO: Consumers must check eventId for idempotency before processing (at-least-once delivery)

        return orderMapper.toOrderResponse(savedOrder);
    }

    public List<OrderResponse> listOrders() {
        return orderRepository.findAll().stream()
                .map(orderMapper::toOrderResponse)
                .collect(Collectors.toList());
    }

    public OrderResponse getOrderById(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        return orderMapper.toOrderResponse(order);
    }
}
