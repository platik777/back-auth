package ru.platik777.backauth.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Сервис работы с ролями и тарифами
 * Соответствует методам из auth.go:
 * - getRole
 * - setTariff
 * - CheckRoleAdmin
 *
 * Взаимодействует с микросервисом back-access для управления доступом
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoleService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.back-access.host}")
    private String backAccessHost;

    @Value("${app.back-access.port}")
    private String backAccessPort;

    @Value("${app.back-access.key-edit-access}")
    private String keyEditAccess;

    // Timeout для HTTP запросов (30 секунд, как в Go)
    // Go: ctx2, cancel := context.WithTimeout(context.Background(), 30*time.Second)
    @Value("${app.back-access.timeout:30000}")
    private int requestTimeoutMs;

    /**
     * Получение доступных ролей для нового пользователя
     * Go: func (a *AuthService) getRole(ctx context.Context, logger go_logger.Logger)
     *
     * Возвращает:
     * - roles: список доступных ролей
     * - useModules: список модулей для активации
     *
     * @return Map с ключами "roles" и "useModules"
     * @throws BackAccessException если не удалось получить данные
     */
    @Retryable(
            retryFor = {RestClientException.class},
            backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public RolesResponse getAvailableRoles() {
        log.debug("Getting available roles from back-access");

        String url = buildUrl("/api/v1/access/newUser/getAvailableRoles");

        try {
            HttpHeaders headers = createHeaders();
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    new org.springframework.core.ParameterizedTypeReference<>() {}
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> body = response.getBody();

                // Извлекаем данные из ответа
                // Go: roleResp := struct { Roles []TypeRole; UseModules []any }
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> roles = (List<Map<String, Object>>) body.get("roles");
                @SuppressWarnings("unchecked")
                List<Object> useModules = (List<Object>) body.get("useModules");

                log.info("Successfully retrieved {} roles and {} modules",
                        roles != null ? roles.size() : 0,
                        useModules != null ? useModules.size() : 0);

                return new RolesResponse(roles, useModules);
            } else {
                String errorMsg = String.format(
                        "Failed to get roles. Status: %s",
                        response.getStatusCode()
                );
                log.error(errorMsg);
                throw new BackAccessException(errorMsg);
            }

        } catch (RestClientException e) {
            log.error("Error communicating with back-access for roles", e);
            throw new BackAccessException("Failed to get roles from back-access", e);
        } catch (Exception e) {
            log.error("Unexpected error getting roles", e);
            throw new BackAccessException("Unexpected error during role retrieval", e);
        }
    }

    /**
     * Асинхронная установка тарифа для нового пользователя
     * Go: func (a *AuthService) setTariff(ctx context.Context, logger, userId int, useModules []any)
     *
     * ВАЖНО: В Go это вызывается в отдельной горутине с timeout'ом 30 секунд:
     * go func() {
     *     ctx2, cancel := context.WithTimeout(context.Background(), 30*time.Second)
     *     defer cancel()
     *     a.setTariff(ctx2, l, userId, modules)
     * }()
     *
     * @param userId ID пользователя
     * @param useModules Список модулей для активации
     * @return CompletableFuture для отслеживания выполнения
     */
    @Async
    public CompletableFuture<Void> setTariffAsync(UUID userId, List<Object> useModules) {
        log.debug("Setting tariff asynchronously for userId: {}", userId);

        // Проверка входных данных
        if (userId == null) {
            log.error("Invalid userId");
            return CompletableFuture.failedFuture(
                    new IllegalArgumentException("Invalid userId")
            );
        }

        if (useModules == null || useModules.isEmpty()) {
            log.warn("No modules to set for userId: {}. Skipping tariff setup.", userId);
            return CompletableFuture.completedFuture(null);
        }

        String url = buildUrl("/api/v1/access/newUser/setTariffNewUser");

        try {
            // Формирование тела запроса
            // Go: bodyStruct := struct { UserId int; UseModules []any }
            TariffRequest requestBody = new TariffRequest(userId, useModules);
            String jsonBody = objectMapper.writeValueAsString(requestBody);

            log.debug("Sending tariff request for userId: {} with {} modules",
                    userId, useModules.size());

            // Создание заголовков
            HttpHeaders headers = createHeaders();
            HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);

            // Отправка запроса
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                log.info("Tariff set successfully for userId: {}", userId);
                return CompletableFuture.completedFuture(null);
            } else {
                String errorMsg = String.format(
                        "Failed to set tariff. Status: %s, UserId: %s",
                        response.getStatusCode(), userId
                );
                log.error(errorMsg);
                return CompletableFuture.failedFuture(
                        new BackAccessException(errorMsg)
                );
            }

        } catch (Exception e) {
            log.error("Error setting tariff for userId: {}", userId, e);
            // В Go ошибки игнорируются, т.к. это не критично
            // Но логируем для отслеживания проблем
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * Проверка наличия роли администратора у пользователя
     * Go: func (a *AuthService) CheckRoleAdmin(ctx context.Context, logger, userId int) bool
     *
     * В Go это делегируется SDK:
     * return a.sdkdb.RoleIsAdmin(ctx, logger, userId)
     *
     * TODO: Реализовать интеграцию с SDK базой данных
     * Варианты:
     * 1. Прямой запрос к БД SDK
     * 2. HTTP запрос к back-access
     * 3. Использование Spring Security с ролями из БД
     *
     * @param userId ID пользователя
     * @return true если пользователь администратор
     */
    public boolean checkRoleAdmin(UUID userId) {
        log.debug("Checking admin role for userId: {}", userId);

        if (userId == null) {
            log.warn("Invalid userId for admin check");
            return false;
        }

        try {
            // ВАРИАНТ 1: Запрос к back-access
            String url = buildUrl("/api/v1/access/user/" + userId + "/isAdmin");

            HttpHeaders headers = createHeaders();
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    new org.springframework.core.ParameterizedTypeReference<>() {}
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Boolean isAdmin = (Boolean) response.getBody().get("isAdmin");
                log.debug("User {} admin status: {}", userId, isAdmin);
                return Boolean.TRUE.equals(isAdmin);
            }

            log.warn("Failed to check admin role. Status: {}", response.getStatusCode());
            return false;

        } catch (Exception e) {
            log.error("Error checking admin role for userId: {}", userId, e);
            // В случае ошибки возвращаем false для безопасности
            return false;
        }
    }

    // ==================== PRIVATE METHODS ====================

    /**
     * Построение полного URL для запроса
     */
    private String buildUrl(String path) {
        return String.format("http://%s:%s%s", backAccessHost, backAccessPort, path);
    }

    /**
     * Создание стандартных заголовков для запросов
     * Go: req.Header.Set("keyAccess", models.KeyEditAccess)
     */
    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("keyAccess", keyEditAccess);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    // ==================== DTO CLASSES ====================

    /**
     * Ответ с ролями и модулями
     * Go: roleResp := struct { Roles []TypeRole; UseModules []any }
     */
    public record RolesResponse(
            List<Map<String, Object>> roles,
            List<Object> useModules
    ) {}

    /**
     * Запрос на установку тарифа
     * Go: bodyStruct := struct { UserId int; UseModules []any }
     */
    private record TariffRequest(
            UUID userId,
            List<Object> useModules
    ) {}

    // ==================== CUSTOM EXCEPTIONS ====================

    /**
     * Исключение при ошибке взаимодействия с back-access
     */
    public static class BackAccessException extends RuntimeException {
        public BackAccessException(String message) {
            super(message);
        }

        public BackAccessException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}