package ru.platik777.backauth.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.platik777.backauth.dto.request.ApiKeyCheckRequest;
import ru.platik777.backauth.dto.request.ApiKeyCreateRequest;
import ru.platik777.backauth.dto.response.ApiKeyAuthResponse;
import ru.platik777.backauth.dto.response.ApiKeyResponse;
import ru.platik777.backauth.dto.response.StatusResponse;
import ru.platik777.backauth.service.ApiKeyService;

import java.util.List;

/**
 * Контроллер управления API ключами
 * Соответствует auth.go API key endpoints
 *
 * Базовый путь: /api/v1/key
 * Go:
 * - apiv1R.HandleFunc("/key", h.apiKeyCreate).Methods(http.MethodPost)
 * - apiv1R.HandleFunc("/key/{token}", h.apiKeyDelete).Methods(http.MethodDelete)
 * - apiv1R.HandleFunc("/key", h.apiKeyGet).Methods(http.MethodGet)
 * - apiv1R.HandleFunc("/key/check", h.checkApiKeyAuthorization).Methods(http.MethodPost)
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/key")
@RequiredArgsConstructor
public class ApiKeyController {

    private final ApiKeyService apiKeyService;

    /**
     * POST /api/v1/key
     * Создание нового API ключа
     * Go: apiv1R.HandleFunc("/key", h.apiKeyCreate).Methods(http.MethodPost)
     * ВАЖНО: userId передается через header
     */
    @PostMapping
    public ResponseEntity<ApiKeyResponse> createApiKey(
            @RequestHeader("userId") Integer userId,
            @RequestBody ApiKeyCreateRequest request) {

        log.info("Creating API key for userId: {}, name: {}", userId, request.getName());

        ApiKeyResponse response = apiKeyService.createApiKey(
                userId,
                request.getName(),
                request.getExpireAt()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * DELETE /api/v1/key/{token}
     * Мягкое удаление API ключа
     * Go: apiv1R.HandleFunc("/key/{token}", h.apiKeyDelete).Methods(http.MethodDelete)
     * ВАЖНО: userId передается через header, token через path
     */
    @DeleteMapping("/{token}")
    public ResponseEntity<StatusResponse> deleteApiKey(
            @RequestHeader("userId") Integer userId,
            @PathVariable String token) {

        log.info("Deleting API key for userId: {}", userId);

        StatusResponse response = apiKeyService.deleteApiKey(userId, token);

        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/v1/key
     * Получение всех активных API ключей пользователя
     * Go: apiv1R.HandleFunc("/key", h.apiKeyGet).Methods(http.MethodGet)
     * ВАЖНО: userId передается через header
     */
    @GetMapping
    public ResponseEntity<List<ApiKeyResponse>> getApiKeys(
            @RequestHeader("userId") Integer userId) {

        log.debug("Getting API keys for userId: {}", userId);

        List<ApiKeyResponse> response = apiKeyService.getApiKeys(userId);

        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/v1/key/check
     * Проверка авторизации по API ключу
     * Go: apiv1R.HandleFunc("/key/check", h.checkApiKeyAuthorization).Methods(http.MethodPost)
     */
    @PostMapping("/check")
    public ResponseEntity<ApiKeyAuthResponse> checkApiKeyAuthorization(
            @RequestBody ApiKeyCheckRequest request) {

        log.debug("Checking API key authorization");

        ApiKeyAuthResponse response = apiKeyService.checkApiKeyAuthorization(request.getToken());

        return ResponseEntity.ok(response);
    }
}