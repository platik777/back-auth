package ru.platik777.backauth.service;

import io.jsonwebtoken.ExpiredJwtException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.platik777.backauth.dto.response.ApiKeyAuthResponse;
import ru.platik777.backauth.dto.response.ApiKeyResponse;
import ru.platik777.backauth.dto.response.StatusResponse;
import ru.platik777.backauth.entity.ApiKey;
import ru.platik777.backauth.entity.User;
import ru.platik777.backauth.repository.ApiKeyRepository;
import ru.platik777.backauth.repository.UserRepository;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Сервис работы с API ключами
 * Соответствует методам ApiKey* из auth.go:
 * - ApiKeyCreate
 * - ApiKeyDelete
 * - ApiKeyGet
 * - CheckApiKeyAuthorization
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApiKeyService {

    private final ApiKeyRepository apiKeyRepository;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final KeyService keyService;

    // Константы для валидации
    private static final String DEFAULT_EXPIRE_DATE = "2099-01-01";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final int MAX_VALIDITY_YEARS = 100;

    /**
     * Создание нового API ключа
     * Go: func (a *AuthService) ApiKeyCreate(logger go_logger.Logger, resp *models.ApiKeyReq)
     *
     * Алгоритм:
     * 1. Валидация входных данных
     * 2. Парсинг даты истечения (дефолт: 2099-01-01)
     * 3. Создание JWT токена
     * 4. Сохранение в БД
     *
     * @param userId ID пользователя
     * @param name Название API ключа
     * @param expireAt Дата истечения (формат: yyyy-MM-dd)
     * @return ApiKeyResponse с созданным ключом
     * @throws ApiKeyException если создание не удалось
     */
    @Transactional
    public ApiKeyResponse createApiKey(UUID userId, String name, String expireAt) {
        log.debug("Creating API key for userId: {}, name: {}", userId, name);

        validateUserId(userId);

        if (name == null || name.trim().isEmpty()) {
            throw new ApiKeyException("API key name cannot be empty", "name");
        }

        if (expireAt == null || expireAt.trim().isEmpty()) {
            expireAt = DEFAULT_EXPIRE_DATE;
            log.debug("Using default expiration date: {}", DEFAULT_EXPIRE_DATE);
        }

        LocalDateTime expireAtDateTime = parseAndValidateExpirationDate(expireAt);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("User not found: {}", userId);
                    return new ApiKeyException("User not found: " + userId, "userId");
                });

        try {
            String apiKeyToken = jwtService.createApiKeyToken(userId, expireAtDateTime);

            // Создание ApiKey entity
            ApiKey apiKey = new ApiKey();
            apiKey.setApiKey(apiKeyToken);
            apiKey.setUser(user);
            apiKey.setExpireAt(Instant.from(expireAtDateTime));
            apiKey.setName(name.trim());
            apiKey.setIsDeleted(false);

            ApiKey savedApiKey = apiKeyRepository.save(apiKey);

            log.info("API key created successfully for userId: {}, name: {}", userId, name);

            return ApiKeyResponse.fromApiKey(savedApiKey);

        } catch (Exception e) {
            log.error("Error creating API key for userId: {}", userId, e);
            throw new ApiKeyException("Failed to create API key", e);
        }
    }

    /**
     * Мягкое удаление API ключа
     * Go: func (a *AuthService) ApiKeyDelete(logger go_logger.Logger, userId int, apikey string)
     *
     * @param userId ID пользователя
     * @param apiKey API ключ для удаления
     * @return StatusResponse с результатом
     * @throws ApiKeyException если ключ не найден
     */
    @Transactional
    public StatusResponse deleteApiKey(UUID userId, String apiKey) {
        log.debug("Deleting API key for userId: {}", userId);

        validateUserId(userId);

        // Go: if apikey == ""
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new ApiKeyException("API key cannot be empty", "apiKey");
        }

        // Мягкое удаление (установка isDeleted = true)
        // Go: UPDATE ApiKey ak SET ak.isDeleted = true WHERE ak.apiKey = :apiKey AND ak.user.id = :userId
        int deleted = apiKeyRepository.softDeleteByApiKeyAndUserId(apiKey, userId);

        if (deleted == 0) {
            log.warn("API key not found or already deleted. UserId: {}", userId);
            throw new ApiKeyException("API key not found or already deleted", "apiKey");
        }

        log.info("API key deleted successfully for userId: {}", userId);

        // Go: return []byte(fmt.Sprintf(`{"status":"OK"}`)), nil
        return StatusResponse.builder()
                .status(true)
                .message("API key deleted")
                .build();
    }

    /**
     * Получение всех активных API ключей пользователя
     * Go: func (a *AuthService) ApiKeyGet(logger go_logger.Logger, userId int)
     *
     * @param userId ID пользователя
     * @return Список активных API ключей
     */
    @Transactional(readOnly = true)
    public List<ApiKeyResponse> getApiKeys(UUID userId) {
        log.debug("Getting API keys for userId: {}", userId);

        validateUserId(userId);

        // Go: response, err := a.db.GetApiKeys(logger, userId)
        List<ApiKey> apiKeys = apiKeyRepository.findByUserIdAndIsDeletedFalse(userId);

        log.debug("Found {} active API keys for userId: {}", apiKeys.size(), userId);

        return apiKeys.stream()
                .map(ApiKeyResponse::fromApiKey)
                .collect(Collectors.toList());
    }

    /**
     * Проверка авторизации по API ключу
     * Go: func (a *AuthService) CheckApiKeyAuthorization(logger go_logger.Logger, token string)
     *
     * Алгоритм:
     * 1. Валидация токена не пустой
     * 2. Парсинг JWT токена
     * 3. Проверка наличия ключа в БД
     * 4. Возврат userId
     *
     * @param apiKeyToken API ключ для проверки
     * @return ApiKeyAuthResponse с userId
     * @throws ApiKeyException если ключ невалидный
     */
    @Transactional(readOnly = true)
    public ApiKeyAuthResponse checkApiKeyAuthorization(String apiKeyToken) {
        log.debug("Checking API key authorization");

        // Go: if token == ""
        if (apiKeyToken == null || apiKeyToken.trim().isEmpty()) {
            throw new ApiKeyException("API key cannot be empty", "apiKey");
        }

        UUID userId;

        try {
            // Парсинг токена и получение userId
            // Go: userId, err := parseToken(token, models.SigningKeyApiKey)
            userId = jwtService.parseToken(
                    apiKeyToken,
                    keyService.getSigningKeyApiKey()
            );

        } catch (ExpiredJwtException e) {
            // Go: if errorsToken.Errors == jwt.ValidationErrorExpired
            log.warn("API key expired: {}", e.getMessage());
            throw new ApiKeyException("API key expired", "apiKey");

        } catch (JwtService.JwtException e) {
            log.warn("Invalid API key: {}", e.getMessage());
            throw new ApiKeyException("Invalid API key", "apiKey");

        } catch (Exception e) {
            log.error("Error parsing API key", e);
            throw new ApiKeyException("Invalid API key", e);
        }

        // Проверка наличия ключа в БД
        boolean exists = apiKeyRepository.existsByApiKeyAndUserIdAndIsDeletedFalse(
                apiKeyToken,
                userId
        );

        if (!exists) {
            log.warn("API key not found in database. UserId: {}", userId);
            throw new ApiKeyException("Invalid API key", "apiKey");
        }

        log.debug("API key authorized successfully for userId: {}", userId);

        // Go: responseAuth := struct { UserId int }{ UserId: userId }
        return ApiKeyAuthResponse.builder()
                .userId(userId)
                .build();
    }

    // ==================== PRIVATE METHODS ====================

    /**
     * Валидация userId
     */
    private void validateUserId(UUID userId) {
        if (userId == null) {
            throw new ApiKeyException("Invalid userId", "userId");
        }
    }

    /**
     * Парсинг и валидация даты истечения
     *
     * Go: expireAt, err := time.Parse("2006-01-02", resp.ExpireAt)
     *     expireAt = expireAt.Add(23*time.Hour + 59*time.Minute + 59*time.Second)
     *
     * @param expireAtStr Строка с датой (yyyy-MM-dd)
     * @return LocalDateTime с временем 23:59:59
     * @throws ApiKeyException если формат неверный
     */
    private LocalDateTime parseAndValidateExpirationDate(String expireAtStr) {
        try {
            // Парсинг даты
            LocalDate date = LocalDate.parse(expireAtStr, DATE_FORMATTER);

            // Добавление времени до конца дня (23:59:59)
            // Go: expireAt.Add(23*time.Hour + 59*time.Minute + 59*time.Second)
            LocalDateTime expireAt = date.atTime(23, 59, 59);

            // Проверка что дата в будущем
            if (expireAt.isBefore(LocalDateTime.now())) {
                throw new ApiKeyException(
                        "Expiration date must be in the future",
                        "expireAt"
                );
            }

            // Проверка что дата не слишком далеко в будущем
            LocalDateTime maxDate = LocalDateTime.now().plusYears(MAX_VALIDITY_YEARS);
            if (expireAt.isAfter(maxDate)) {
                throw new ApiKeyException(
                        String.format("Expiration date too far in future (max %d years)",
                                MAX_VALIDITY_YEARS),
                        "expireAt"
                );
            }

            log.debug("Parsed expiration date: {}", expireAt);
            return expireAt;

        } catch (DateTimeParseException e) {
            log.error("Invalid date format: {}", expireAtStr, e);
            throw new ApiKeyException(
                    "Invalid expireAt format. Expected: yyyy-MM-dd (e.g., 2025-12-31)",
                    "expireAt"
            );
        }
    }

    // ==================== CUSTOM EXCEPTIONS ====================

    /**
     * Исключение при работе с API ключами
     */
    @Getter
    public static class ApiKeyException extends RuntimeException {
        private final String fieldName;

        public ApiKeyException(String message, String fieldName) {
            super(message);
            this.fieldName = fieldName;
        }

        public ApiKeyException(String message, Throwable cause) {
            super(message, cause);
            this.fieldName = null;
        }

    }
}