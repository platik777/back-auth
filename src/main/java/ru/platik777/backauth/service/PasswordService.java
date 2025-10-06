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
 * Соответствует части service.go (generatePasswordHash)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordService {
    @Value("${app.jwt.constants.salt}")
    private String salt;

    /**
     * Генерация хеша пароля
     * Go: func generatePasswordHash(logger go_logger.Logger, password string)
     *
     * ВАЖНО: Алгоритм должен точно соответствовать Go версии:
     * 1. Хешируем пароль SHA-1
     * 2. Добавляем соль К БАЙТАМ хеша (не к hex строке!)
     * 3. Конвертируем результат в hex
     */
    public String generatePasswordHash(String password) {
        try {
            // Получаем соль из БД (аналог models.Salt в Go)

            // Создаем SHA-1 хеш
            MessageDigest digest = MessageDigest.getInstance("SHA-1");

            // Хешируем пароль
            digest.update(password.getBytes(StandardCharsets.UTF_8));

            // КРИТИЧНО: Добавляем соль к байтам хеша, а не к hex строке!
            // В Go: hash.Sum([]byte(models.Salt))
            byte[] finalHash = digest.digest(salt.getBytes(StandardCharsets.UTF_8));

            // Конвертируем в hex (аналог fmt.Sprintf("%x", ...))
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
     * Аналог fmt.Sprintf("%x", bytes) в Go
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
}