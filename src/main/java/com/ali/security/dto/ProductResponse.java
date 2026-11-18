package com.ali.security.dto;

import com.ali.security.entity.Product;

import java.math.BigDecimal;

public record ProductResponse(Long id, String sku, Integer availableStock, BigDecimal retailPrice) {
    public static ProductResponse from(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getSku(),
                product.getAvailableStock(),
                product.getRetailPrice());
    }
}
