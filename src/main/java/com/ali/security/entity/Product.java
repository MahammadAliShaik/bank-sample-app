package com.ali.security.entity;

import com.ali.security.service.SensitiveDataConverter;
import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

@Entity
@Data
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String sku;
    private Integer availableStock;
    private BigDecimal retailPrice;

    // BANKING SECURITY: Encrypt wholesale pricing
    @Convert(converter = SensitiveDataConverter.class)
    @Column(name = "wholesale_cost")
    private String wholesaleCost;
}