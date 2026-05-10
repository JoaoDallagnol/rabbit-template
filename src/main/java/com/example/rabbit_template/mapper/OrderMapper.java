package com.example.rabbit_template.mapper;

import com.example.rabbit_template.domain.Order;
import com.example.rabbit_template.domain.OrderItem;
import com.example.rabbit_template.dto.OrderItemRequest;
import com.example.rabbit_template.dto.OrderResponse;
import com.example.rabbit_template.event.OrderCreatedEvent;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface OrderMapper {

    OrderResponse toOrderResponse(Order order);

    OrderItem toOrderItem(OrderItemRequest request);

    @Mapping(source = "orderId", target = "orderId")
    @Mapping(source = "customerId", target = "customerId")
    @Mapping(source = "amount", target = "amount")
    @Mapping(source = "status", target = "status")
    @Mapping(source = "occurredAt", target = "createdAt")
    Order toOrder(OrderCreatedEvent event);
}
