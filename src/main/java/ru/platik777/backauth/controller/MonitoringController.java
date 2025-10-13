package ru.platik777.backauth.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.platik777.backauth.dto.monitoring.MonitoringReport;
import ru.platik777.backauth.dto.monitoring.SystemMetrics;
import ru.platik777.backauth.dto.monitoring.TokenStatistics;
import ru.platik777.backauth.dto.monitoring.UserStatistics;
import ru.platik777.backauth.dto.response.StatusResponse;
import ru.platik777.backauth.service.MonitoringService;

/**
 * Контроллер мониторинга системы
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class MonitoringController {

    private final MonitoringService monitoringService;

    /**
     * GET /monitor/status
     * Проверка статуса сервиса
     */
    @GetMapping("/monitor/status")
    public ResponseEntity<StatusResponse> getStatus() {
        log.debug("Status check requested");

        // Базовая проверка здоровья
        try {
            SystemMetrics metrics = monitoringService.getSystemMetrics();

            return ResponseEntity.ok(
                    StatusResponse.builder()
                            .status(true)
                            .message("Service is running. Uptime: " + metrics.getUptime())
                            .build()
            );
        } catch (Exception e) {
            log.error("Status check failed", e);
            return ResponseEntity.status(503).body(
                    StatusResponse.builder()
                            .status(false)
                            .message("Service is unhealthy: " + e.getMessage())
                            .build()
            );
        }
    }

    /**
     * PUT /config/update
     * Обновление конфигурации
     * ПРИМЕЧАНИЕ: В Spring Boot обычно используется Spring Cloud Config
     * или actuator/refresh для перезагрузки конфигурации
     */
    @PutMapping("/config/update")
    public ResponseEntity<StatusResponse> updateConfig() {
        log.info("Config update requested");

        // TODO: Реализовать логику обновления конфигурации
        // Опции:
        // 1. @RefreshScope для бинов
        // 2. Spring Cloud Config
        // 3. Actuator /actuator/refresh endpoint

        return ResponseEntity.ok(
                StatusResponse.builder()
                        .status(true)
                        .message("Config update endpoint not implemented. Use Spring Cloud Config or /actuator/refresh")
                        .build()
        );
    }

    /**
     * GET /monitor/metrics
     * Системные метрики (дополнительно)
     */
    @GetMapping("/monitor/metrics")
    public ResponseEntity<SystemMetrics> getSystemMetrics() {
        log.debug("System metrics requested");

        SystemMetrics metrics = monitoringService.getSystemMetrics();

        return ResponseEntity.ok(metrics);
    }

    /**
     * GET /monitor/tokens
     * Статистика токенов (дополнительно)
     */
    @GetMapping("/monitor/tokens")
    public ResponseEntity<TokenStatistics> getTokenStatistics() {
        log.debug("Token statistics requested");

        TokenStatistics stats = monitoringService.getTokenStatistics();

        return ResponseEntity.ok(stats);
    }

    /**
     * GET /monitor/users
     * Статистика пользователей (дополнительно)
     */
    @GetMapping("/monitor/users")
    public ResponseEntity<UserStatistics> getUserStatistics() {
        log.debug("User statistics requested");

        UserStatistics stats = monitoringService.getUserStatistics();

        return ResponseEntity.ok(stats);
    }

    /**
     * GET /monitor/report
     * Полный отчет о состоянии системы (дополнительно)
     */
    @GetMapping("/monitor/report")
    public ResponseEntity<MonitoringReport> getFullReport() {
        log.debug("Full monitoring report requested");

        MonitoringReport report = monitoringService.getFullReport();

        return ResponseEntity.ok(report);
    }
}