package com.ali.security.service;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private KeyPair keyPair;
    private Clock clock;
    private JwtService jwtService;

    @BeforeEach
    void setUp() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        keyPair = generator.generateKeyPair();
        clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);
        jwtService = new JwtService(keyPair.getPrivate(), keyPair.getPublic(), Duration.ofMinutes(15), clock);
    }

    @Test
    void generatedTokenHasSubjectIdAndConfiguredLifetime() {
        String token = jwtService.generateToken("customer@example.com");

        assertThat(jwtService.extractUsername(token)).isEqualTo("customer@example.com");
        assertThat(jwtService.extractId(token)).isNotBlank();
        assertThat(jwtService.extractExpiration(token).toInstant())
                .isEqualTo(Instant.parse("2026-01-01T00:15:00Z"));
        assertThat(jwtService.validateToken(token, "customer@example.com")).isTrue();
        assertThat(jwtService.validateToken(token, "another@example.com")).isFalse();
    }

    @Test
    void expiredTokenIsRejected() {
        String token = jwtService.generateToken("customer@example.com");
        JwtService afterExpiry = new JwtService(
                keyPair.getPrivate(),
                keyPair.getPublic(),
                Duration.ofMinutes(15),
                Clock.fixed(Instant.parse("2026-01-01T00:16:00Z"), ZoneOffset.UTC));

        assertThatThrownBy(() -> afterExpiry.extractAllClaims(token))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void tokenWithInvalidSignatureIsRejected() {
        String token = jwtService.generateToken("customer@example.com");
        String[] parts = token.split("\\.");
        byte[] signature = Base64.getUrlDecoder().decode(parts[2]);
        signature[0] ^= 1;
        parts[2] = Base64.getUrlEncoder().withoutPadding().encodeToString(signature);
        String tamperedToken = String.join(".", parts);

        assertThatThrownBy(() -> jwtService.extractAllClaims(tamperedToken))
                .isInstanceOf(JwtException.class);
    }
}
