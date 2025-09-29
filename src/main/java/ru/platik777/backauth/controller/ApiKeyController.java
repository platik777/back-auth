package ru.platik777.backauth.controller;

import ru.platik777.backauth.dto.ApiKeyDto;
import ru.platik777.backauth.dto.request.ApiKeyRequestDto;
import ru.platik777.backauth.dto.response.ApiResponseDto;
import ru.platik777.backauth.dto.response.AuthorizationResponseDto;
import ru.platik777.backauth.service.ApiKeyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Контроллер для работы с API ключами
 * Реализует эндпоинты из handler.go (apikey.go)
 *
 * Paths:
 * - /api/v1/key - управление API ключами
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/key")
@RequiredArgsConstructor
public class ApiKeyController {

    private final ApiKeyService apiKeyService;

    /**
     * Создание нового API ключа
     * POST /api/v1/key
     * Аналог apiKeyCreate из Go handler
     */
    @PostMapping
    public ResponseEntity<ApiResponseDto<ApiKeyDto>> createApiKey(
            @Valid @RequestBody ApiKeyRequestDto requestDto,
            @RequestHeader("userId") Integer userId) {

        log.info("Create API key request for userId: {}", userId);

        requestDto.setUserId(userId);
        ApiKeyDto apiKey = apiKeyService.createApiKey(requestDto);

        return ResponseEntity.ok(ApiResponseDto.success(apiKey));
    }

    /**
     * Получение списка API ключей пользователя
     * GET /api/v1/key
     * Аналог apiKeyGet из Go handler
     */
    @GetMapping
    public ResponseEntity<ApiResponseDto<List<ApiKeyDto>>> getApiKeys(
            @RequestHeader("userId") Integer userId) {

        log.info("Get API keys request for userId: {}", userId);

        List<ApiKeyDto> apiKeys = apiKeyService.getApiKeys(userId);

        return ResponseEntity.ok(ApiResponseDto.success(apiKeys));
    }

    /**
     * Удаление API ключа
     * DELETE /api/v1/key/{token}
     * Аналог apiKeyDelete из Go handler
     */
    @DeleteMapping("/{token}")
    public ResponseEntity<ApiResponseDto<String>> deleteApiKey(
            @PathVariable String token,
            @RequestHeader("userId") Integer userId) {

        log.info("Delete API key request for userId: {}", userId);

        ApiResponseDto<String> response = apiKeyService.deleteApiKey(userId, token);

        return ResponseEntity.ok(response);
    }

    /**
     * Проверка авторизации по API ключу
     * POST /api/v1/key/check
     * Аналог checkApiKeyAuthorization из Go handler
     */
    @PostMapping("/check")
    public ResponseEntity<ApiResponseDto<AuthorizationResponseDto>> checkApiKeyAuthorization(
            @RequestBody CheckApiKeyRequest request) {

        log.debug("Check API key authorization request");

        AuthorizationResponseDto response = apiKeyService.checkApiKeyAuthorization(request.apikey());

        return ResponseEntity.ok(ApiResponseDto.success(response));
    }

    /**
     * DTO для проверки API ключа
     */
    public record CheckApiKeyRequest(String apikey) {}
}