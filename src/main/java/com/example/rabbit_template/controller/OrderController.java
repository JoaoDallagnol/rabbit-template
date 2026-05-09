package com.example.rabbit_template.controller;

import com.example.rabbit_template.dto.OrderRequest;
import com.example.rabbit_template.dto.OrderResponse;
import com.example.rabbit_template.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/topic")
    public ResponseEntity<OrderResponse> createOrderTopic(@Valid @RequestBody OrderRequest request) {
        OrderResponse response = orderService.createOrderTopic(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/fanout")
    public ResponseEntity<OrderResponse> createOrderFanout(@Valid @RequestBody OrderRequest request) {
        OrderResponse response = orderService.createOrderFanout(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<OrderResponse>> listOrders() {
        List<OrderResponse> orders = orderService.listOrders();
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrderById(@PathVariable UUID orderId) {
        OrderResponse response = orderService.getOrderById(orderId);
        return ResponseEntity.ok(response);
    }
}
