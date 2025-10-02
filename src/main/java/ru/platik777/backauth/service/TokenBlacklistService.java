package ru.platik777.backauth.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Сервис черного списка токенов
 * Соответствует TokenBlacklist из jwt.go
 *
 * Используется для инвалидации токенов при:
 * - Сбросе пароля (токен сброса пароля становится одноразовым)
 * - Выходе из системы (будущая функциональность)
 *
 * Go: type TokenBlacklist struct { ... }
 */
@Slf4j
@Service
public class TokenBlacklistService {

    // Map для хранения токенов: token -> время добавления
    // Go: tokenBlackList map[string]time.Time
    private final Map<String, LocalDateTime> tokenBlacklist = new ConcurrentHashMap<>();

    // Время жизни access токена из конфигурации (в миллисекундах)
    // Go: models.TimeAToken = 10 * time.Minute
    @Value("${app.jwt.app.access.expiration:600000}")
    private long accessTokenExpirationMs;

    // Интервал очистки из конфигурации (в миллисекундах)
    // Go: models.CleanUpInterval
    @Value("${app.jwt.cleanup-interval:600000}")
    private long cleanupIntervalMs;

    /**
     * Инициализация сервиса
     * Go: func NewTokenBlacklist(cleanupInterval time.Duration)
     */
    @PostConstruct
    public void initialize() {
        long accessTokenMinutes = accessTokenExpirationMs / 60000;
        long cleanupMinutes = cleanupIntervalMs / 60000;

        log.info("TokenBlacklistService initialized:");
        log.info("  - Access token lifetime: {} minutes ({} ms)",
                accessTokenMinutes, accessTokenExpirationMs);
        log.info("  - Cleanup interval: {} minutes ({} ms)",
                cleanupMinutes, cleanupIntervalMs);
        log.info("  - Using ConcurrentHashMap for thread safety");
    }

    /**
     * Добавление токена в черный список
     * Go: func (a *TokenBlacklist) AddToken(token string)
     *
     * @param token JWT токен для инвалидации
     */
    public void addToken(String token) {
        if (token == null || token.isEmpty()) {
            log.warn("Attempted to add null or empty token to blacklist");
            return;
        }

        LocalDateTime addedAt = LocalDateTime.now();
        tokenBlacklist.put(token, addedAt);

        log.debug("Token added to blacklist at {}. Total tokens: {}",
                addedAt, tokenBlacklist.size());
    }

    /**
     * Проверка нахождения токена в черном списке
     * Go: func (a *TokenBlacklist) IsBlacklisted(token string) bool
     *
     * @param token JWT токен для проверки
     * @return true если токен в черном списке
     */
    public boolean isBlacklisted(String token) {
        if (token == null || token.isEmpty()) {
            return false;
        }

        boolean isBlacklisted = tokenBlacklist.containsKey(token);

        if (isBlacklisted) {
            log.debug("Token found in blacklist: {}",
                    maskToken(token));
        }

        return isBlacklisted;
    }

    /**
     * Фоновая очистка устаревших токенов
     * Go: func (a *TokenBlacklist) CleanUp()
     *
     * ВАЖНО: В Go используется TimeAToken (10 минут) для определения устаревших токенов
     * Комментарий из Go: "Из-за того, что используется TimeAToken в создании токена,
     * ту же константу использую здесь"
     *
     * Выполняется с интервалом из конфигурации (по умолчанию 10 минут)
     */
    @Scheduled(fixedRateString = "${app.jwt.cleanup-interval:600000}")
    public void cleanUp() {
        try {
            // Вычисляем порог: текущее время - время жизни токена
            // Go: if time.Since(createdAt) > models.TimeAToken
            LocalDateTime threshold = LocalDateTime.now()
                    .minusNanos(accessTokenExpirationMs * 1_000_000);

            int initialSize = tokenBlacklist.size();

            // Удаляем токены старше порога
            tokenBlacklist.entrySet().removeIf(entry ->
                    entry.getValue().isBefore(threshold)
            );

            int removed = initialSize - tokenBlacklist.size();

            if (removed > 0) {
                log.info("Cleaned up {} expired tokens from blacklist. Remaining: {}",
                        removed, tokenBlacklist.size());
            } else {
                log.debug("No expired tokens to clean. Current size: {}",
                        tokenBlacklist.size());
            }

        } catch (Exception e) {
            log.error("Error during token blacklist cleanup", e);
            // Не пробрасываем исключение, чтобы не остановить scheduled task
        }
    }

    /**
     * Получение количества токенов в черном списке
     * Полезно для мониторинга и метрик
     */
    public int getSize() {
        return tokenBlacklist.size();
    }

    /**
     * Очистка всего черного списка
     * ВНИМАНИЕ: Использовать только для тестирования!
     * В production это может привести к проблемам безопасности
     */
    public void clear() {
        int size = tokenBlacklist.size();
        tokenBlacklist.clear();
        log.warn("Token blacklist manually cleared. {} tokens removed", size);
    }

    /**
     * Получение статистики черного списка
     * Полезно для мониторинга
     */
    public BlacklistStats getStats() {
        LocalDateTime threshold = LocalDateTime.now()
                .minusNanos(accessTokenExpirationMs * 1_000_000);

        long expiredCount = tokenBlacklist.values().stream()
                .filter(timestamp -> timestamp.isBefore(threshold))
                .count();

        long activeCount = tokenBlacklist.size() - expiredCount;

        return new BlacklistStats(
                tokenBlacklist.size(),
                activeCount,
                expiredCount,
                accessTokenExpirationMs / 60000 // в минутах
        );
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Маскирование токена для логирования
     * Показывает только первые и последние 4 символа
     */
    private String maskToken(String token) {
        if (token == null || token.length() < 12) {
            return "****";
        }

        return token.substring(0, 4) + "..." +
                token.substring(token.length() - 4);
    }

    // ==================== INNER CLASSES ====================

    /**
     * DTO для статистики черного списка
     */
    public record BlacklistStats(
            int totalTokens,
            long activeTokens,
            long expiredTokens,
            long tokenLifetimeMinutes
    ) {}
}