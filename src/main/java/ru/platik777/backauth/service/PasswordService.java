package ru.platik777.backauth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Сервис работы с паролями
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordService {
    @Value("${app.jwt.constants.salt}")
    private String salt;

    /**
     * Генерация хеша пароля
     */
    public String generatePasswordHash(String password) {
        try {
            // Создаем SHA-1 хеш
            MessageDigest digest = MessageDigest.getInstance("SHA-1");

            // Хешируем пароль
            digest.update(password.getBytes(StandardCharsets.UTF_8));

            byte[] finalHash = digest.digest(salt.getBytes(StandardCharsets.UTF_8));

            return bytesToHex(finalHash);

        } catch (NoSuchAlgorithmException e) {
            log.error("Error generating password hash", e);
            throw new RuntimeException("Failed to generate password hash", e);
        }
    }

    /**
     * Проверка соответствия пароля хешу
     */
    public boolean verifyPassword(String password, String hash) {
        if (password == null || hash == null) {
            return false;
        }

        try {
            String computedHash = generatePasswordHash(password);
            return computedHash.equals(hash);
        } catch (Exception e) {
            log.error("Error verifying password", e);
            return false;
        }
    }

    /**
     * Конвертация байтов в hex строку
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
}