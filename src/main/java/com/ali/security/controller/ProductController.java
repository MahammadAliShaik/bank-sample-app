package com.ali.security.controller;

import com.ali.security.dto.CreateOrderRequest;
import com.ali.security.dto.OrderResponse;
import com.ali.security.entity.Order;
import com.ali.security.entity.Product;
import com.ali.security.service.InventoryOrderService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/inventory")
public class ProductController {
    private final InventoryOrderService service;
    public ProductController(InventoryOrderService service) { this.service = service; }

    @GetMapping("/public/products")
    public ResponseEntity<List<Product>> getPublicProducts() {
        return ResponseEntity.ok(service.getAllAvailableProducts());
    }

    @PostMapping("/admin/products")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Product> createProduct(@RequestBody Product product) {
        return new ResponseEntity<>(service.addProduct(product), HttpStatus.CREATED);
    }
}

