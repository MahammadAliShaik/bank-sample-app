package com.ali.security.controller;

import com.ali.security.dto.ProductResponse;
import com.ali.security.entity.Product;
import com.ali.security.service.InventoryOrderService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/api/v1/inventory")
public class ProductController {
    private final InventoryOrderService service;
    public ProductController(InventoryOrderService service) { this.service = service; }

    @GetMapping("/public/products")
    public ResponseEntity<List<ProductResponse>> getPublicProducts() {
        return ResponseEntity.ok(service.getAllAvailableProducts().stream().map(ProductResponse::from).toList());
    }

    @PostMapping("/admin/products")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody Product product) {
        return new ResponseEntity<>(ProductResponse.from(service.addProduct(product)), HttpStatus.CREATED);
    }
}

