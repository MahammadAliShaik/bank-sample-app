package com.ali.security.service;

import com.ali.security.dto.CreateOrderRequest;
import com.ali.security.dto.OrderResponse;
import com.ali.security.entity.Order;
import com.ali.security.entity.Product;
import com.ali.security.repository.OrderRepository;
import com.ali.security.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class InventoryOrderServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private InventoryOrderService service;

    private Product product;

    @BeforeEach
    void setUp() {
        product = new Product();
        product.setId(7L);
        product.setAvailableStock(10);
        product.setRetailPrice(new BigDecimal("12.50"));
    }

    @Test
    void placesOrderAndDecrementsStock() {
        given(productRepository.findByIdWithLock(7L)).willReturn(Optional.of(product));
        given(productRepository.save(any(Product.class))).willAnswer(invocation -> invocation.getArgument(0));
        given(orderRepository.save(any(Order.class))).willAnswer(invocation -> invocation.getArgument(0));

        OrderResponse response = service.placeOrder("customer@example.com", request(3));

        assertThat(product.getAvailableStock()).isEqualTo(7);
        assertThat(response.getProductId()).isEqualTo(7L);
        assertThat(response.getQuantity()).isEqualTo(3);
        assertThat(response.getTotalAmount()).isEqualByComparingTo("37.50");
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void publicCatalogQueriesOnlyProductsWithStock() {
        given(productRepository.findByAvailableStockGreaterThan(0)).willReturn(List.of(product));

        assertThat(service.getAllAvailableProducts()).containsExactly(product);
        verify(productRepository).findByAvailableStockGreaterThan(0);
    }

    @Test
    void rejectsZeroOrNegativeQuantityWithoutChangingStock() {
        for (int quantity : new int[]{0, -2}) {
            assertThatThrownBy(() -> service.placeOrder("customer@example.com", request(quantity)))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(error -> ((ResponseStatusException) error).getStatusCode())
                    .isEqualTo(BAD_REQUEST);
        }

        assertThat(product.getAvailableStock()).isEqualTo(10);
        verify(productRepository, never()).save(any(Product.class));
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void rejectsQuantityAboveAvailableStock() {
        given(productRepository.findByIdWithLock(7L)).willReturn(Optional.of(product));

        assertThatThrownBy(() -> service.placeOrder("customer@example.com", request(11)))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(error -> ((ResponseStatusException) error).getStatusCode())
                .isEqualTo(CONFLICT);

        assertThat(product.getAvailableStock()).isEqualTo(10);
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void rejectsMissingProduct() {
        given(productRepository.findByIdWithLock(7L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.placeOrder("customer@example.com", request(1)))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(error -> ((ResponseStatusException) error).getStatusCode())
                .isEqualTo(NOT_FOUND);

        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void getOrdersReturnsOnlyResponseFieldsForTheRequestedCustomer() {
        Order order = Order.builder()
                .orderNumber("ORD-123")
                .customerUsername("customer@example.com")
                .productId(7L)
                .quantity(2)
                .totalAmount(new BigDecimal("25.00"))
                .settlementAccountNumber("sensitive-account")
                .createdAt(Instant.parse("2026-01-01T00:00:00Z"))
                .build();
        given(orderRepository.findByCustomerUsername("customer@example.com")).willReturn(List.of(order));

        List<OrderResponse> result = service.getOrdersByUser("customer@example.com");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getOrderNumber()).isEqualTo("ORD-123");
        assertThat(result.get(0).getTotalAmount()).isEqualByComparingTo("25.00");
    }

    private CreateOrderRequest request(int quantity) {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setProductId(7L);
        request.setQuantity(quantity);
        request.setSettlementAccountNumber("account-1234");
        return request;
    }
}
