package ru.platik777.backauth.service;

import ru.platik777.backauth.config.properties.AppProperties;
import ru.platik777.backauth.repository.SaltRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Сервис для работы с паролями
 * Реализует логику хеширования из Go версии с использованием SHA1 + соль
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordService {

    private final AppProperties appProperties;
    private final SaltRepository saltRepository;

    /**
     * Генерация хеша пароля (аналог Go функции generatePasswordHash)
     * Использует SHA1 + статическая соль + динамическая соль из БД
     */
    public String generatePasswordHash(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");

            // Добавляем пароль
            digest.update(password.getBytes(StandardCharsets.UTF_8));

            // Добавляем статическую соль из конфигурации (аналог models.Salt)
            digest.update(appProperties.getSalt().getBytes(StandardCharsets.UTF_8));

            // Получаем хеш
            byte[] hashBytes = digest.digest();

            // Конвертируем в hex строку
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }

            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
            log.error("SHA-1 algorithm not available", e);
            throw new RuntimeException("Error generating password hash", e);
        }
    }

    /**
     * Проверка пароля
     */
    public boolean checkPassword(String rawPassword, String hashedPassword) {
        String computedHash = generatePasswordHash(rawPassword);
        return computedHash.equals(hashedPassword);
    }

    /**
     * Получение соли из БД (для совместимости с Go версией)
     */
    public String getSaltFromDatabase() {
        return saltRepository.getSalt();
    }

    /**
     * Генерация хеша с учетом соли из БД (расширенная версия)
     */
    public String generatePasswordHashWithDbSalt(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");

            // Добавляем пароль
            digest.update(password.getBytes(StandardCharsets.UTF_8));

            // Добавляем статическую соль из конфигурации
            digest.update(appProperties.getSalt().getBytes(StandardCharsets.UTF_8));

            // Добавляем динамическую соль из БД
            String dbSalt = getSaltFromDatabase();
            digest.update(dbSalt.getBytes(StandardCharsets.UTF_8));

            // Получаем хеш
            byte[] hashBytes = digest.digest();

            // Конвертируем в hex строку
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }

            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
            log.error("SHA-1 algorithm not available", e);
            throw new RuntimeException("Error generating password hash", e);
        }
    }

    /**
     * Валидация сложности пароля
     */
    public boolean isPasswordValid(String password) {
        if (password == null || password.trim().isEmpty()) {
            return false;
        }

        // Минимальные требования (можно расширить)
        return password.length() >= 6;
    }

    /**
     * Генерация случайного пароля
     */
    public String generateRandomPassword(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*";
        StringBuilder password = new StringBuilder();

        for (int i = 0; i < length; i++) {
            int index = (int) (Math.random() * chars.length());
            password.append(chars.charAt(index));
        }

        return password.toString();
    }
}