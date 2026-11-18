package com.ali.security.controller;

import com.ali.security.service.JwtService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.Date;

@RestController
@RequestMapping("/api/v1/auth")
public class LogoutController {

    private final StringRedisTemplate redisTemplate;
    private final JwtService jwtService;

    public LogoutController(StringRedisTemplate redisTemplate, JwtService jwtService) {
        this.redisTemplate = redisTemplate;
        this.jwtService = jwtService;
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(@RequestHeader("Authorization") String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            Date expiration = jwtService.extractExpiration(token);
            long remainingSeconds = (expiration.getTime() - System.currentTimeMillis()) / 1000;

            if (remainingSeconds > 0) {
                // Instantly writes the token to Redis with an automatic expiration matching the JWT
                redisTemplate.opsForValue().set("blacklist:" + token, "revoked", Duration.ofSeconds(remainingSeconds));
            }
        }
        return ResponseEntity.ok("Successfully logged out. Token destroyed.");
    }
}