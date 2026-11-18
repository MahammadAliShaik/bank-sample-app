package com.ali.security.controller;


import com.ali.security.dto.CreateOrderRequest;
import com.ali.security.dto.OrderResponse;
import com.ali.security.service.InventoryOrderService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {
    private final InventoryOrderService service;

    public OrderController(InventoryOrderService service) {
        this.service = service;
    }

    @PostMapping("/create")
    public ResponseEntity<OrderResponse> placeOrder(Authentication authentication, @Valid @RequestBody CreateOrderRequest request) {
        OrderResponse response = service.placeOrder(authentication.getName(), request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/my-orders")
    public ResponseEntity<List<OrderResponse>> getMyOrders(Authentication authentication) {
        return ResponseEntity.ok(service.getOrdersByUser(authentication.getName()));
    }
}