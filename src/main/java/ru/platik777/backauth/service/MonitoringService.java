package ru.platik777.backauth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Service;
import ru.platik777.backauth.dto.monitoring.MonitoringReport;
import ru.platik777.backauth.dto.monitoring.SystemMetrics;
import ru.platik777.backauth.dto.monitoring.TokenStatistics;
import ru.platik777.backauth.dto.monitoring.UserStatistics;
import ru.platik777.backauth.entity.User;
import ru.platik777.backauth.entity.types.AccountType;
import ru.platik777.backauth.exception.MonitoringException;
import ru.platik777.backauth.repository.UserRepository;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.RuntimeMXBean;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Сервис мониторинга состояния системы
 * <p/>
 * Предоставляет метрики для:
 * - Состояния сервисов
 * - Статистики токенов
 * - Использования ресурсов
 * - Состояния БД
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MonitoringService implements HealthIndicator {

    private final UserRepository userRepository;
    private final TokenBlacklistService tokenBlacklistService;
    private final KeyService keyService;

    private final LocalDateTime startTime = LocalDateTime.now();

    /**
     * Spring Boot Health Check
     */
    @Override
    public Health health() {
        try {
            // Проверка доступности БД
            long userCount = userRepository.count();

            return Health.up()
                    .withDetail("database", "connected")
                    .withDetail("userCount", userCount)
                    .withDetail("keyService", "initialized")
                    .build();

        } catch (Exception e) {
            log.error("Health check failed", e);
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }

    /**
     * Получение общих метрик системы
     */
    public SystemMetrics getSystemMetrics() {
        RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();

        long uptimeMillis = runtimeMXBean.getUptime();
        Duration uptime = Duration.ofMillis(uptimeMillis);

        long heapUsed = memoryMXBean.getHeapMemoryUsage().getUsed();
        long heapMax = memoryMXBean.getHeapMemoryUsage().getMax();
        double heapUsagePercent = (double) heapUsed / heapMax * 100;

        return SystemMetrics.builder()
                .startTime(startTime)
                .uptime(formatDuration(uptime))
                .uptimeSeconds(uptime.getSeconds())
                .heapUsedMB(heapUsed / 1024 / 1024)
                .heapMaxMB(heapMax / 1024 / 1024)
                .heapUsagePercent(Math.round(heapUsagePercent * 100.0) / 100.0)
                .build();
    }

    /**
     * Получение статистики токенов
     */
    public TokenStatistics getTokenStatistics() {
        TokenBlacklistService.BlacklistStats blacklistStats =
                tokenBlacklistService.getStats();

        return TokenStatistics.builder()
                .blacklistedTokens(blacklistStats.totalTokens())
                .activeBlacklistedTokens(blacklistStats.activeTokens())
                .expiredBlacklistedTokens(blacklistStats.expiredTokens())
                .tokenLifetimeMinutes(blacklistStats.tokenLifetimeMinutes())
                .build();
    }

    /**
     * Получение статистики пользователей
     */
    public UserStatistics getUserStatistics() {
        try {
            long totalUsers = userRepository.count();

            // Статистика по типам аккаунтов
            Map<String, Long> accountTypeStats = new HashMap<>();
            for (AccountType type : AccountType.values()) {
                long count = userRepository.findByAccountType(type).size();
                accountTypeStats.put(type.getValue(), count);
            }

            return UserStatistics.builder()
                    .totalUsers(totalUsers)
                    .accountTypeDistribution(accountTypeStats)
                    .build();

        } catch (Exception e) {
            log.error("Error getting user statistics", e);
            throw new MonitoringException("Failed to get user statistics", e);
        }
    }

    /**
     * Получение полного отчета о состоянии системы
     */
    public MonitoringReport getFullReport() {
        return MonitoringReport.builder()
                .timestamp(LocalDateTime.now())
                .system(getSystemMetrics())
                .tokens(getTokenStatistics())
                .users(getUserStatistics())
                .services(getServiceStatus())
                .build();
    }

    /**
     * Проверка состояния всех сервисов
     */
    private Map<String, String> getServiceStatus() {
        Map<String, String> status = new HashMap<>();

        // TokenBlacklistService
        try {
            int blacklistSize = tokenBlacklistService.getSize();
            status.put("tokenBlacklist", "OK (" + blacklistSize + " tokens)");
        } catch (Exception e) {
            status.put("tokenBlacklist", "ERROR: " + e.getMessage());
        }

        // Database
        try {
            userRepository.count();
            status.put("database", "OK");
        } catch (Exception e) {
            status.put("database", "ERROR: " + e.getMessage());
        }

        return status;
    }

    /**
     * Форматирование Duration в читаемую строку
     */
    private String formatDuration(Duration duration) {
        long days = duration.toDays();
        long hours = duration.toHours() % 24;
        long minutes = duration.toMinutes() % 60;
        long seconds = duration.getSeconds() % 60;

        if (days > 0) {
            return String.format("%dd %dh %dm %ds", days, hours, minutes, seconds);
        } else if (hours > 0) {
            return String.format("%dh %dm %ds", hours, minutes, seconds);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds);
        } else {
            return String.format("%ds", seconds);
        }
    }
}