package com.ali.security.entity;

import com.ali.security.service.SensitiveDataConverter;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Entity
@Getter
@Setter
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    @NotBlank
    @Size(max = 100)
    private String sku;

    @NotNull
    @PositiveOrZero
    private Integer availableStock;

    @NotNull
    @DecimalMin("0.01")
    private BigDecimal retailPrice;

    // BANKING SECURITY: Encrypt wholesale pricing
    @Convert(converter = SensitiveDataConverter.class)
    @Column(name = "wholesale_cost")
    private String wholesaleCost;
}