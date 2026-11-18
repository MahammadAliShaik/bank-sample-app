package com.ali.security.service;

import com.ali.security.dto.CreateOrderRequest;
import com.ali.security.dto.OrderResponse;
import com.ali.security.entity.Order;
import com.ali.security.entity.OrderStatus;
import com.ali.security.entity.Product;
import com.ali.security.repository.OrderRepository;
import com.ali.security.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class InventoryOrderService {

    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;

    public InventoryOrderService(ProductRepository productRepository, OrderRepository orderRepository) {
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
    }

    public List<Product> getAllAvailableProducts() { return productRepository.findByAvailableStockGreaterThan(0); }

    public Product addProduct(Product product) { return productRepository.save(product); }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public OrderResponse placeOrder(String username, CreateOrderRequest request) {
        if (request == null || request.getProductId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Product ID is required");
        }
        if (request.getQuantity() == null || request.getQuantity() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quantity must be positive");
        }
        if (request.getSettlementAccountNumber() == null || request.getSettlementAccountNumber().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Settlement account number is required");
        }

        Product product = productRepository.findByIdWithLock(request.getProductId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));

        if (product.getAvailableStock() < request.getQuantity()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Insufficient stock");
        }

        product.setAvailableStock(product.getAvailableStock() - request.getQuantity());
        productRepository.save(product);

        BigDecimal total = product.getRetailPrice().multiply(BigDecimal.valueOf(request.getQuantity()));

        Order order = Order.builder()
                .orderNumber("ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .customerUsername(username)
                .productId(product.getId())
                .quantity(request.getQuantity())
                .totalAmount(total)
                .settlementAccountNumber(request.getSettlementAccountNumber())
                .status(OrderStatus.CONFIRMED)
                .build();

        Order saved = orderRepository.save(order);

        return toResponse(saved);
    }

    public List<OrderResponse> getOrdersByUser(String username) {
        return orderRepository.findByCustomerUsername(username).stream()
                .map(this::toResponse)
                .toList();
    }

    private OrderResponse toResponse(Order order) {
        return OrderResponse.builder()
                .orderNumber(order.getOrderNumber())
                .productId(order.getProductId())
                .quantity(order.getQuantity())
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus())
                .createdAt(order.getCreatedAt())
                .build();
    }
}