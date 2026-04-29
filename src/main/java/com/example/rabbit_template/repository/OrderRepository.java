package com.example.rabbit_template.repository;

import com.example.rabbit_template.domain.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {
    // No custom methods — inherits findAll, findById, save, etc.
}
