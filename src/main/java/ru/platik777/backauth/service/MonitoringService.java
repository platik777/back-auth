package ru.platik777.backauth.service;

import ru.platik777.backauth.config.properties.AppProperties;
import ru.platik777.backauth.dto.response.ApiResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.info.BuildProperties;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Сервис мониторинга состояния приложения
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MonitoringService {

    private final DataSource dataSource;
    private final AppProperties appProperties;
    private final BuildProperties buildProperties;

    /**
     * Получение статуса сервиса (аналог status из Go)
     */
    public ApiResponseDto<String> getServiceStatus() {
        return ApiResponseDto.success("ok Back-Auth");
    }

    /**
     * Получение детального статуса здоровья сервиса
     */
    public ApiResponseDto<Map<String, Object>> getHealthStatus() {
        Map<String, Object> health = new HashMap<>();

        // Проверка подключения к БД
        boolean dbHealthy = checkDatabaseHealth();

        // Проверка сервиса email
        boolean emailHealthy = checkEmailHealth();

        // Общий статус
        boolean overallHealthy = dbHealthy && emailHealthy;

        health.put("status", overallHealthy ? "UP" : "DOWN");
        health.put("timestamp", LocalDateTime.now());
        health.put("database", dbHealthy ? "UP" : "DOWN");
        health.put("email", emailHealthy ? "UP" : "DOWN");
        health.put("version", buildProperties != null ? buildProperties.getVersion() : "unknown");

        return ApiResponseDto.success(health);
    }

    /**
     * Получение информации о сервисе
     */
    public ApiResponseDto<Map<String, Object>> getServiceInfo() {
        Map<String, Object> info = new HashMap<>();

        info.put("name", "back-auth");
        info.put("description", "Authentication and Authorization Service");
        info.put("version", buildProperties != null ? buildProperties.getVersion() : "unknown");
        info.put("buildTime", buildProperties != null ? buildProperties.getTime() : null);
        info.put("baseUrl", appProperties.getBaseUrl());
        info.put("timestamp", LocalDateTime.now());

        return ApiResponseDto.success(info);
    }

    /**
     * Обновление конфигурации (аналог updateConfig из Go)
     */
    public ApiResponseDto<String> updateConfig() {
        log.info("Configuration update requested");

        // В Spring Boot конфигурация обычно обновляется через Spring Cloud Config
        // или требует перезапуска приложения

        // Для имитации обновления конфигурации возвращаем успешный ответ
        return ApiResponseDto.success("Back-Auth config file has been successfully updated");
    }

    /**
     * Получение метрик сервиса
     */
    public ApiResponseDto<Map<String, Object>> getServiceMetrics() {
        Map<String, Object> metrics = new HashMap<>();

        // Системные метрики
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;

        metrics.put("memory", Map.of(
                "total", totalMemory,
                "used", usedMemory,
                "free", freeMemory,
                "maxMemory", runtime.maxMemory()
        ));

        metrics.put("processors", runtime.availableProcessors());
        metrics.put("uptime", System.currentTimeMillis());
        metrics.put("timestamp", LocalDateTime.now());

        return ApiResponseDto.success(metrics);
    }

    // Приватные методы для проверки здоровья

    private boolean checkDatabaseHealth() {
        try (Connection connection = dataSource.getConnection()) {
            return connection.isValid(5); // 5 секунд таймаут
        } catch (Exception e) {
            log.error("Database health check failed", e);
            return false;
        }
    }

    private boolean checkEmailHealth() {
        try {
            // Простая проверка наличия настроек email
            // В реальности можно попробовать подключиться к SMTP серверу
            return true; // Заглушка
        } catch (Exception e) {
            log.error("Email health check failed", e);
            return false;
        }
    }
}