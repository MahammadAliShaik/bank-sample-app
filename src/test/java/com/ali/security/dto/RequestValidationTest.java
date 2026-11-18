package com.ali.security.dto;

import com.ali.security.entity.Product;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class RequestValidationTest {

    private static Validator validator;
    private static AutoCloseable validatorFactory;

    @BeforeAll
    static void setUpValidator() {
        var factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
        validatorFactory = factory::close;
    }

    @AfterAll
    static void closeValidator() throws Exception {
        validatorFactory.close();
    }

    @Test
    void registrationRequiresValidEmailNameAndStrongPassword() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("not-an-email");
        request.setName(" ");
        request.setPassword("short");

        assertThat(validator.validate(request))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("email", "name", "password");
    }

    @Test
    void orderRequestRejectsNonPositiveIdsAndQuantityAndBlankAccount() {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setProductId(0L);
        request.setQuantity(-1);
        request.setSettlementAccountNumber(" ");

        assertThat(validator.validate(request))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("productId", "quantity", "settlementAccountNumber");
    }

    @Test
    void productRejectsNegativeStockAndNonPositivePrice() {
        Product product = new Product();
        product.setSku("SKU-1");
        product.setAvailableStock(-1);
        product.setRetailPrice(new BigDecimal("-0.01"));

        assertThat(validator.validate(product))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("availableStock", "retailPrice");
    }
}
