package com.example.rabbit_template.mapper;

import com.example.rabbit_template.domain.Order;
import com.example.rabbit_template.domain.OrderItem;
import com.example.rabbit_template.dto.OrderItemRequest;
import com.example.rabbit_template.dto.OrderResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface OrderMapper {

    OrderResponse toOrderResponse(Order order);

    OrderItem toOrderItem(OrderItemRequest request);
}
