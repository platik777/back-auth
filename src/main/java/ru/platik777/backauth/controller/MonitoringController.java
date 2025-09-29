package ru.platik777.backauth.controller;

import ru.platik777.backauth.dto.response.ApiResponseDto;
import ru.platik777.backauth.service.MonitoringService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Контроллер для мониторинга состояния сервиса
 * Реализует эндпоинты из handler.go
 *
 * Paths:
 * - /monitor/status - статус сервиса
 * - /config/update - обновление конфигурации (TODO)
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class MonitoringController {

    private final MonitoringService monitoringService;

    /**
     * Получение статуса сервиса
     * GET /monitor/status
     * Аналог status из Go handler
     */
    @GetMapping("/monitor/status")
    public ResponseEntity<ApiResponseDto<String>> getStatus() {
        log.debug("Get service status request");

        ApiResponseDto<String> response = monitoringService.getServiceStatus();
        return ResponseEntity.ok(response);
    }

    /**
     * Получение детального статуса здоровья сервиса
     * GET /monitor/health
     */
    @GetMapping("/monitor/health")
    public ResponseEntity<ApiResponseDto<Map<String, Object>>> getHealth() {
        log.debug("Get service health request");

        ApiResponseDto<Map<String, Object>> response = monitoringService.getHealthStatus();
        return ResponseEntity.ok(response);
    }

    /**
     * Получение информации о сервисе
     * GET /monitor/info
     */
    @GetMapping("/monitor/info")
    public ResponseEntity<ApiResponseDto<Map<String, Object>>> getInfo() {
        log.debug("Get service info request");

        ApiResponseDto<Map<String, Object>> response = monitoringService.getServiceInfo();
        return ResponseEntity.ok(response);
    }

    /**
     * Обновление конфигурации (динамическое)
     * PUT /config/update
     * Аналог updateConfig из Go handler
     *
     * TODO: Реализовать динамическое обновление конфигурации
     * В Go используется viper для перезагрузки конфигов
     * В Spring Boot можно использовать @RefreshScope или Spring Cloud Config
     */
    @PutMapping("/config/update")
    public ResponseEntity<ApiResponseDto<String>> updateConfig() {
        log.warn("Config update endpoint called - not implemented yet");

        // TODO: Реализовать обновление конфигурации
        return ResponseEntity.ok(
                ApiResponseDto.success("Config update not implemented yet")
        );
    }
}