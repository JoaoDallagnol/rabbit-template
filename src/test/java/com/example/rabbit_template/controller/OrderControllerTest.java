package com.example.rabbit_template.controller;

import com.example.rabbit_template.dto.OrderItemRequest;
import com.example.rabbit_template.dto.OrderRequest;
import com.example.rabbit_template.dto.OrderResponse;
import com.example.rabbit_template.exception.GlobalExceptionHandler;
import com.example.rabbit_template.exception.OrderNotFoundException;
import com.example.rabbit_template.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Feature: rabbit-template, Property 6: POST /orders with valid body returns HTTP 201 with correct response shape
// Feature: rabbit-template, Property 7: POST /orders with invalid body returns HTTP 400
// Feature: rabbit-template, Property 8: GET /orders returns HTTP 200 with all persisted orders
// Feature: rabbit-template, Property 9: GET /orders/{orderId} round-trip
@ExtendWith(MockitoExtension.class)
class OrderControllerTest {

    @Mock
    private OrderService orderService;

    @InjectMocks
    private OrderController orderController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(orderController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    // Property 6: POST /orders with valid body returns HTTP 201 with correct response shape
    @Test
    void postOrdersWithValidBodyReturns201() throws Exception {
        for (int i = 0; i < 20; i++) {
            String customerId = "customer" + i;
            double amount = 1.0 + i * 10;
            String productId = "product" + i;

            OrderItemRequest item = OrderItemRequest.builder().productId(productId).quantity(i + 1).build();
            OrderRequest request = OrderRequest.builder()
                    .customerId(customerId).amount(amount).items(List.of(item)).build();

            UUID id = UUID.randomUUID();
            OrderResponse response = OrderResponse.builder()
                    .orderId(id).status("CREATED").amount(amount).createdAt(LocalDateTime.now()).build();

            when(orderService.createOrder(any())).thenReturn(response);

            mockMvc.perform(post("/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.status").value("CREATED"))
                    .andExpect(jsonPath("$.orderId").isNotEmpty());
        }
    }

    // Property 7: POST /orders with invalid body returns HTTP 400
    @Test
    void postOrdersWithNullCustomerIdReturns400() throws Exception {
        OrderRequest request = OrderRequest.builder()
                .customerId(null).amount(10.0)
                .items(List.of(OrderItemRequest.builder().productId("p1").quantity(1).build()))
                .build();
        mockMvc.perform(post("/orders").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void postOrdersWithBlankCustomerIdReturns400() throws Exception {
        OrderRequest request = OrderRequest.builder()
                .customerId("   ").amount(10.0)
                .items(List.of(OrderItemRequest.builder().productId("p1").quantity(1).build()))
                .build();
        mockMvc.perform(post("/orders").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void postOrdersWithNullAmountReturns400() throws Exception {
        OrderRequest request = OrderRequest.builder()
                .customerId("customer1").amount(null)
                .items(List.of(OrderItemRequest.builder().productId("p1").quantity(1).build()))
                .build();
        mockMvc.perform(post("/orders").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void postOrdersWithEmptyItemsReturns400() throws Exception {
        OrderRequest request = OrderRequest.builder()
                .customerId("customer1").amount(10.0).items(new ArrayList<>()).build();
        mockMvc.perform(post("/orders").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void postOrdersWithZeroQuantityReturns400() throws Exception {
        OrderRequest request = OrderRequest.builder()
                .customerId("customer1").amount(10.0)
                .items(List.of(OrderItemRequest.builder().productId("p1").quantity(0).build()))
                .build();
        mockMvc.perform(post("/orders").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // Property 8: GET /orders returns HTTP 200 with all persisted orders
    @Test
    void getOrdersReturnsEmptyList() throws Exception {
        when(orderService.listOrders()).thenReturn(List.of());
        mockMvc.perform(get("/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void getOrdersReturnsAllOrders() throws Exception {
        for (int n = 1; n <= 10; n++) {
            List<OrderResponse> orders = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                orders.add(OrderResponse.builder().orderId(UUID.randomUUID())
                        .status("CREATED").amount(10.0 * i).createdAt(LocalDateTime.now()).build());
            }
            when(orderService.listOrders()).thenReturn(orders);
            mockMvc.perform(get("/orders"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(n));
        }
    }

    // Property 9: GET /orders/{orderId} round-trip
    @Test
    void getOrderByIdRoundTrip() throws Exception {
        for (int i = 0; i < 20; i++) {
            UUID id = UUID.randomUUID();
            String status = "CREATED";
            OrderResponse response = OrderResponse.builder()
                    .orderId(id).status(status).amount(10.0 * i).createdAt(LocalDateTime.now()).build();

            when(orderService.getOrderById(id)).thenReturn(response);

            mockMvc.perform(get("/orders/{orderId}", id))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.orderId").value(id.toString()))
                    .andExpect(jsonPath("$.status").value(status));
        }
    }

    // Requirement 7.6: GET /orders/{randomUUID} not in system returns HTTP 404
    @Test
    void getOrderByIdReturns404ForUnknownId() throws Exception {
        UUID unknownId = UUID.randomUUID();
        when(orderService.getOrderById(unknownId)).thenThrow(new OrderNotFoundException(unknownId));

        mockMvc.perform(get("/orders/{orderId}", unknownId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").exists());
    }
}
