package ru.platik777.backauth.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.platik777.backauth.dto.response.StatusResponse;
import ru.platik777.backauth.service.MonitoringService;

/**
 * Контроллер мониторинга системы
 * Соответствует monitor.go endpoints
 *
 * Go routes:
 * - monitorR := r.PathPrefix("/monitor").Subrouter()
 * - monitorR.HandleFunc("/status", h.status).Methods(http.MethodGet)
 * - configR := r.PathPrefix("/config").Subrouter()
 * - configR.HandleFunc("/update", h.updateConfig).Methods(http.MethodPut)
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class MonitoringController {

    private final MonitoringService monitoringService;

    /**
     * GET /monitor/status
     * Проверка статуса сервиса
     * Go: monitorR.HandleFunc("/status", h.status).Methods(http.MethodGet)
     */
    @GetMapping("/monitor/status")
    public ResponseEntity<StatusResponse> getStatus() {
        log.debug("Status check requested");

        // Базовая проверка здоровья
        try {
            MonitoringService.SystemMetrics metrics = monitoringService.getSystemMetrics();

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
     * Go: configR.HandleFunc("/update", h.updateConfig).Methods(http.MethodPut)
     *
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

    // ==================== ДОПОЛНИТЕЛЬНЫЕ ENDPOINTS ДЛЯ МОНИТОРИНГА ====================

    /**
     * GET /monitor/metrics
     * Системные метрики (дополнительно)
     */
    @GetMapping("/monitor/metrics")
    public ResponseEntity<MonitoringService.SystemMetrics> getSystemMetrics() {
        log.debug("System metrics requested");

        MonitoringService.SystemMetrics metrics = monitoringService.getSystemMetrics();

        return ResponseEntity.ok(metrics);
    }

    /**
     * GET /monitor/tokens
     * Статистика токенов (дополнительно)
     */
    @GetMapping("/monitor/tokens")
    public ResponseEntity<MonitoringService.TokenStatistics> getTokenStatistics() {
        log.debug("Token statistics requested");

        MonitoringService.TokenStatistics stats = monitoringService.getTokenStatistics();

        return ResponseEntity.ok(stats);
    }

    /**
     * GET /monitor/users
     * Статистика пользователей (дополнительно)
     */
    @GetMapping("/monitor/users")
    public ResponseEntity<MonitoringService.UserStatistics> getUserStatistics() {
        log.debug("User statistics requested");

        MonitoringService.UserStatistics stats = monitoringService.getUserStatistics();

        return ResponseEntity.ok(stats);
    }

    /**
     * GET /monitor/report
     * Полный отчет о состоянии системы (дополнительно)
     */
    @GetMapping("/monitor/report")
    public ResponseEntity<MonitoringService.MonitoringReport> getFullReport() {
        log.debug("Full monitoring report requested");

        MonitoringService.MonitoringReport report = monitoringService.getFullReport();

        return ResponseEntity.ok(report);
    }
}