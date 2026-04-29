package com.example.rabbit_template.mapper;

import com.example.rabbit_template.domain.Order;
import com.example.rabbit_template.domain.OrderItem;
import com.example.rabbit_template.dto.OrderItemRequest;
import com.example.rabbit_template.dto.OrderResponse;
import net.jqwik.api.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

// Feature: rabbit-template, Property 1: Order-to-OrderResponse mapping preserves all fields
// Feature: rabbit-template, Property 2: OrderItemRequest-to-OrderItem mapping preserves all fields
@SpringBootTest
class OrderMapperTest {

    @Autowired
    private OrderMapper orderMapper;

    // Property 1: Order-to-OrderResponse mapping preserves all fields
    @Test
    void orderToOrderResponseMappingPreservesAllFields_edgeCase() {
        Order order = Order.builder()
                .orderId(UUID.randomUUID())
                .customerId("customer1")
                .amount(99.99)
                .status("CREATED")
                .createdAt(LocalDateTime.of(2025, 6, 15, 12, 0))
                .items(List.of())
                .build();

        OrderResponse response = orderMapper.toOrderResponse(order);

        assertThat(response.getOrderId()).isEqualTo(order.getOrderId());
        assertThat(response.getStatus()).isEqualTo(order.getStatus());
        assertThat(response.getAmount()).isEqualTo(order.getAmount());
        assertThat(response.getCreatedAt()).isEqualTo(order.getCreatedAt());
    }

    @Test
    void orderToOrderResponseMappingPreservesAllFields_multipleValues() {
        for (int i = 0; i < 50; i++) {
            UUID id = UUID.randomUUID();
            double amount = 0.01 + i * 10.0;
            String status = "STATUS_" + i;
            LocalDateTime createdAt = LocalDateTime.of(2024, 1, 1, 0, 0).plusDays(i);

            Order order = Order.builder()
                    .orderId(id)
                    .customerId("customer" + i)
                    .amount(amount)
                    .status(status)
                    .createdAt(createdAt)
                    .items(List.of())
                    .build();

            OrderResponse response = orderMapper.toOrderResponse(order);

            assertThat(response.getOrderId()).isEqualTo(id);
            assertThat(response.getStatus()).isEqualTo(status);
            assertThat(response.getAmount()).isEqualTo(amount);
            assertThat(response.getCreatedAt()).isEqualTo(createdAt);
        }
    }

    // Property 2: OrderItemRequest-to-OrderItem mapping preserves all fields
    @Test
    void orderItemRequestToOrderItemMappingPreservesAllFields_edgeCase() {
        OrderItemRequest request = OrderItemRequest.builder()
                .productId("product-1")
                .quantity(5)
                .build();

        OrderItem item = orderMapper.toOrderItem(request);

        assertThat(item.getProductId()).isEqualTo(request.getProductId());
        assertThat(item.getQuantity()).isEqualTo(request.getQuantity());
    }

    @Test
    void orderItemRequestToOrderItemMappingPreservesAllFields_multipleValues() {
        for (int i = 1; i <= 50; i++) {
            String productId = "product-" + i;
            int quantity = i;

            OrderItemRequest request = OrderItemRequest.builder()
                    .productId(productId)
                    .quantity(quantity)
                    .build();

            OrderItem item = orderMapper.toOrderItem(request);

            assertThat(item.getProductId()).isEqualTo(productId);
            assertThat(item.getQuantity()).isEqualTo(quantity);
        }
    }
}
