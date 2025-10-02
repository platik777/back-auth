package ru.platik777.backauth.util;

import org.springframework.stereotype.Component;

/**
 * Утилита для извлечения токенов из заголовков
 */
@Component
public class TokenExtractor {

    private static final String BEARER_PREFIX = "Bearer ";

    /**
     * Извлечение токена из Authorization header
     *
     * @param authHeader заголовок Authorization
     * @return токен без префикса "Bearer "
     * @throws IllegalArgumentException если формат header неверный
     */
    public String extractToken(String authHeader) {
        if (authHeader == null || authHeader.trim().isEmpty()) {
            throw new IllegalArgumentException("Authorization header is missing");
        }

        if (!authHeader.startsWith(BEARER_PREFIX)) {
            throw new IllegalArgumentException("Invalid Authorization header format. Expected: Bearer <token>");
        }

        String token = authHeader.substring(BEARER_PREFIX.length()).trim();

        if (token.isEmpty()) {
            throw new IllegalArgumentException("Token is empty");
        }

        return token;
    }

    /**
     * Проверка наличия Bearer префикса
     */
    public boolean isBearerToken(String authHeader) {
        return authHeader != null && authHeader.startsWith(BEARER_PREFIX);
    }
}