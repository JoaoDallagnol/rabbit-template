package com.example.rabbit_template.service;

import com.example.rabbit_template.domain.Order;
import com.example.rabbit_template.domain.OrderItem;
import com.example.rabbit_template.dto.OrderRequest;
import com.example.rabbit_template.dto.OrderResponse;
import com.example.rabbit_template.exception.OrderNotFoundException;
import com.example.rabbit_template.mapper.OrderMapper;
import com.example.rabbit_template.repository.OrderRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;

    public OrderService(OrderRepository orderRepository, OrderMapper orderMapper) {
        this.orderRepository = orderRepository;
        this.orderMapper = orderMapper;
    }

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
        // TODO: Publish OrderCreated event to RabbitMQ exchange (not implemented)
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
