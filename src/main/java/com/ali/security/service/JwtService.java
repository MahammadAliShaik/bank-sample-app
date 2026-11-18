package com.ali.security.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;

@Service
public class JwtService {

    private final PrivateKey privateKey;
    private final PublicKey publicKey;
    private final Duration tokenLifetime;
    private final Clock clock;

    @Autowired
    public JwtService(
            @Value("${security.jwt.key-store}") Resource keyStoreResource,
            @Value("${security.jwt.key-store-password}") String keyStorePassword,
            @Value("${security.jwt.key-password}") String keyPassword,
            @Value("${security.jwt.key-alias}") String keyAlias,
            @Value("${security.jwt.expiration-minutes:15}") long expirationMinutes) {
        this(loadKeyPair(keyStoreResource, keyStorePassword, keyPassword, keyAlias),
                Duration.ofMinutes(expirationMinutes), Clock.systemUTC());
    }

    private JwtService(KeyPair keyPair, Duration tokenLifetime, Clock clock) {
        this(keyPair.getPrivate(), keyPair.getPublic(), tokenLifetime, clock);
    }

    public JwtService(PrivateKey privateKey, PublicKey publicKey, Duration tokenLifetime, Clock clock) {
        if (!(privateKey instanceof RSAPrivateKey) || !(publicKey instanceof RSAPublicKey)) {
            throw new IllegalArgumentException("JWT signing keys must be an RSA key pair");
        }
        if (tokenLifetime == null || tokenLifetime.isZero() || tokenLifetime.isNegative()) {
            throw new IllegalArgumentException("JWT lifetime must be positive");
        }
        this.privateKey = privateKey;
        this.publicKey = publicKey;
        this.tokenLifetime = tokenLifetime;
        this.clock = Objects.requireNonNull(clock, "Clock is required");
    }

    public String generateToken(String username) {
        Instant now = clock.instant();
        return Jwts.builder()
                .subject(username)
                .id(UUID.randomUUID().toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(tokenLifetime)))
                .signWith(privateKey, Jwts.SIG.RS256)
                .compact();
    }

    public Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(publicKey)
                .clock(() -> Date.from(clock.instant()))
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public String extractId(String token) {
        return extractClaim(token, Claims::getId);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        return claimsResolver.apply(extractAllClaims(token));
    }

    public boolean validateToken(String token, String username) {
        return validateToken(extractAllClaims(token), username);
    }

    public boolean validateToken(Claims claims, String username) {
        return username != null
                && username.equals(claims.getSubject())
                && claims.getExpiration() != null
                && claims.getExpiration().after(Date.from(clock.instant()));
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private static KeyPair loadKeyPair(Resource resource, String storePassword, String keyPassword, String alias) {
        try (InputStream input = resource.getInputStream()) {
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            keyStore.load(input, storePassword.toCharArray());

            Key key = keyStore.getKey(alias, keyPassword.toCharArray());
            if (!(key instanceof RSAPrivateKey privateKey)) {
                throw new IllegalStateException("JWT keystore alias does not contain a private key");
            }
            if (keyStore.getCertificate(alias) == null) {
                throw new IllegalStateException("JWT keystore alias has no certificate");
            }
            PublicKey publicKey = keyStore.getCertificate(alias).getPublicKey();
            if (!(publicKey instanceof RSAPublicKey)) {
                throw new IllegalStateException("JWT keystore certificate does not contain an RSA public key");
            }
            return new KeyPair(publicKey, privateKey);
        } catch (IOException | GeneralSecurityException e) {
            throw new IllegalStateException("Unable to load JWT signing keystore", e);
        }
    }
}
