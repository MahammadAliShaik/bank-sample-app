package com.ali.security.dto;

import lombok.Data;

@Data
public class CreateOrderRequest {
    private Long productId;
    private Integer quantity;
    private String settlementAccountNumber;
}