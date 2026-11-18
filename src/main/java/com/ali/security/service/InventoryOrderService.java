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

    public List<Product> getAllAvailableProducts() { return productRepository.findAll(); }

    public Product addProduct(Product product) { return productRepository.save(product); }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public OrderResponse placeOrder(String username, CreateOrderRequest request) {
        Product product = productRepository.findByIdWithLock(request.getProductId())
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));

        if (product.getAvailableStock() < request.getQuantity()) {
            throw new IllegalStateException("Insufficient stock");
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

        return OrderResponse.builder()
                .orderNumber(saved.getOrderNumber())
                .productId(saved.getProductId())
                .quantity(saved.getQuantity())
                .totalAmount(saved.getTotalAmount())
                .status(saved.getStatus())
                .createdAt(saved.getCreatedAt())
                .build();
    }

    public List<Order> getOrdersByUser(String username) {
        return orderRepository.findByCustomerUsername(username);
    }
}