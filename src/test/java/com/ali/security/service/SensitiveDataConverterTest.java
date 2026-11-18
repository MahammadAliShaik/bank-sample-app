package com.ali.security.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SensitiveDataConverterTest {

    private SensitiveDataConverter converter;

    @BeforeEach
    void setUp() {
        converter = new SensitiveDataConverter("0123456789abcdef".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void encryptedDataRoundTripsAndUsesFreshIv() {
        String first = converter.convertToDatabaseColumn("account-1234");
        String second = converter.convertToDatabaseColumn("account-1234");

        assertThat(first).isNotEqualTo(second);
        assertThat(converter.convertToEntityAttribute(first)).isEqualTo("account-1234");
        assertThat(converter.convertToEntityAttribute(second)).isEqualTo("account-1234");
    }

    @Test
    void nullValuesRemainNull() {
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
        assertThat(converter.convertToEntityAttribute(null)).isNull();
    }

    @Test
    void tamperedCiphertextIsRejected() {
        byte[] encrypted = Base64.getDecoder().decode(converter.convertToDatabaseColumn("account-1234"));
        encrypted[encrypted.length - 1] ^= 1;
        String tampered = Base64.getEncoder().encodeToString(encrypted);

        assertThatThrownBy(() -> converter.convertToEntityAttribute(tampered))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void invalidAesKeyLengthIsRejected() {
        assertThatThrownBy(() -> new SensitiveDataConverter(new byte[15]))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
