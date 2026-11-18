package com.ali.security.service;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

@Converter
public class SensitiveDataConverter implements AttributeConverter<String, String> {
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128;
    private static final int GCM_IV_LENGTH = 12;
    private final SecretKeySpec key;

    public SensitiveDataConverter() {
        this(requiredKey());
    }

    SensitiveDataConverter(byte[] keyBytes) {
        if (keyBytes == null || (keyBytes.length != 16 && keyBytes.length != 24 && keyBytes.length != 32)) {
            throw new IllegalArgumentException("AES_DB_KEY must be 16, 24, or 32 bytes");
        }
        this.key = new SecretKeySpec(keyBytes.clone(), "AES");
    }

    @Override
    public String convertToDatabaseColumn(String rawData) {
        if (rawData == null) return null;
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH, iv));

            byte[] cipherText = cipher.doFinal(rawData.getBytes(StandardCharsets.UTF_8));
            byte[] encrypted = new byte[iv.length + cipherText.length];
            System.arraycopy(iv, 0, encrypted, 0, iv.length);
            System.arraycopy(cipherText, 0, encrypted, iv.length, cipherText.length);

            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new IllegalStateException("Critical: Encryption failure", e);
        }
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;
        try {
            byte[] decoded = Base64.getDecoder().decode(dbData);
            byte[] iv = new byte[GCM_IV_LENGTH];
            System.arraycopy(decoded, 0, iv, 0, iv.length);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH, iv));

            return new String(cipher.doFinal(decoded, iv.length, decoded.length - iv.length), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Critical: Decryption failure or payload tampered", e);
        }
    }

    private static byte[] requiredKey() {
        String configuredKey = System.getenv("AES_DB_KEY");
        if (configuredKey == null || configuredKey.isBlank()) {
            throw new IllegalStateException("AES_DB_KEY must be configured");
        }
        byte[] keyBytes = configuredKey.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length != 16 && keyBytes.length != 24 && keyBytes.length != 32) {
            throw new IllegalStateException("AES_DB_KEY must be 16, 24, or 32 UTF-8 bytes");
        }
        return keyBytes;
    }
}