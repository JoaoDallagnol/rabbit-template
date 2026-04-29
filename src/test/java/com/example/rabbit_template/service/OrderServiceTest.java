package com.example.rabbit_template.service;

import com.example.rabbit_template.domain.Order;
import com.example.rabbit_template.domain.OrderItem;
import com.example.rabbit_template.dto.OrderItemRequest;
import com.example.rabbit_template.dto.OrderRequest;
import com.example.rabbit_template.dto.OrderResponse;
import com.example.rabbit_template.exception.OrderNotFoundException;
import com.example.rabbit_template.mapper.OrderMapper;
import com.example.rabbit_template.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

// Feature: rabbit-template, Property 3: createOrder always returns status CREATED with a non-null orderId and matching amount
// Feature: rabbit-template, Property 4: listOrders returns all persisted orders
// Feature: rabbit-template, Property 5: getOrderById round-trip
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderMapper orderMapper;

    @InjectMocks
    private OrderService orderService;

    // Property 3: createOrder always returns status CREATED with a non-null orderId and matching amount
    @Test
    void createOrderAlwaysReturnsStatusCreatedWithNonNullOrderIdAndMatchingAmount() {
        for (int i = 0; i < 50; i++) {
            double amount = 0.01 + i * 5.0;
            String customerId = "customer" + i;
            String productId = "product" + i;

            OrderItemRequest itemRequest = OrderItemRequest.builder()
                    .productId(productId).quantity(i + 1).build();
            OrderRequest request = OrderRequest.builder()
                    .customerId(customerId).amount(amount).items(List.of(itemRequest)).build();

            UUID generatedId = UUID.randomUUID();
            Order savedOrder = Order.builder()
                    .orderId(generatedId).customerId(customerId).amount(amount)
                    .status("CREATED").createdAt(LocalDateTime.now()).items(List.of()).build();
            OrderResponse expectedResponse = OrderResponse.builder()
                    .orderId(generatedId).status("CREATED").amount(amount)
                    .createdAt(savedOrder.getCreatedAt()).build();

            when(orderMapper.toOrderItem(any())).thenReturn(
                    OrderItem.builder().productId(productId).quantity(i + 1).build());
            when(orderRepository.save(any())).thenReturn(savedOrder);
            when(orderMapper.toOrderResponse(any())).thenReturn(expectedResponse);

            OrderResponse response = orderService.createOrder(request);

            assertThat(response.getStatus()).isEqualTo("CREATED");
            assertThat(response.getOrderId()).isNotNull();
            assertThat(response.getCreatedAt()).isNotNull();
            assertThat(response.getAmount()).isEqualTo(amount);
        }
    }

    // Property 4: listOrders returns all persisted orders
    @Test
    void listOrdersReturnsAllPersistedOrders_empty() {
        when(orderRepository.findAll()).thenReturn(List.of());
        List<OrderResponse> result = orderService.listOrders();
        assertThat(result).isEmpty();
    }

    @Test
    void listOrdersReturnsAllPersistedOrders_multipleOrders() {
        List<Order> orders = new ArrayList<>();
        List<OrderResponse> expectedResponses = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            UUID id = UUID.randomUUID();
            Order order = Order.builder().orderId(id).customerId("c" + i)
                    .amount(10.0 * i).status("CREATED").createdAt(LocalDateTime.now())
                    .items(new ArrayList<>()).build();
            OrderResponse resp = OrderResponse.builder().orderId(id).status("CREATED")
                    .amount(10.0 * i).createdAt(order.getCreatedAt()).build();
            orders.add(order);
            expectedResponses.add(resp);
        }

        when(orderRepository.findAll()).thenReturn(orders);
        for (int i = 0; i < orders.size(); i++) {
            when(orderMapper.toOrderResponse(orders.get(i))).thenReturn(expectedResponses.get(i));
        }

        List<OrderResponse> result = orderService.listOrders();

        assertThat(result).hasSize(orders.size());
        assertThat(result.stream().map(OrderResponse::getOrderId).collect(Collectors.toList()))
                .containsExactlyInAnyOrderElementsOf(
                        orders.stream().map(Order::getOrderId).collect(Collectors.toList()));
    }

    // Property 5: getOrderById round-trip
    @Test
    void getOrderByIdRoundTrip() {
        for (int i = 0; i < 20; i++) {
            UUID id = UUID.randomUUID();
            double amount = 1.0 + i;
            String status = "CREATED";
            LocalDateTime createdAt = LocalDateTime.now();

            Order order = Order.builder().orderId(id).customerId("c" + i)
                    .amount(amount).status(status).createdAt(createdAt)
                    .items(new ArrayList<>()).build();
            OrderResponse expected = OrderResponse.builder().orderId(id)
                    .status(status).amount(amount).createdAt(createdAt).build();

            when(orderRepository.findById(id)).thenReturn(Optional.of(order));
            when(orderMapper.toOrderResponse(order)).thenReturn(expected);

            OrderResponse result = orderService.getOrderById(id);

            assertThat(result.getOrderId()).isEqualTo(id);
            assertThat(result.getStatus()).isEqualTo(status);
            assertThat(result.getAmount()).isEqualTo(amount);
            assertThat(result.getCreatedAt()).isEqualTo(createdAt);
        }
    }

    // Requirement 6.6: getOrderById throws OrderNotFoundException for unknown UUID
    @Test
    void getOrderByIdThrowsOrderNotFoundExceptionForUnknownId() {
        UUID unknownId = UUID.randomUUID();
        when(orderRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrderById(unknownId))
                .isInstanceOf(OrderNotFoundException.class)
                .hasMessageContaining(unknownId.toString());
    }
}
