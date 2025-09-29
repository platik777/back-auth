package ru.platik777.backauth.service;

import ru.platik777.backauth.dto.ApiKeyDto;
import ru.platik777.backauth.dto.request.ApiKeyRequestDto;
import ru.platik777.backauth.dto.response.ApiResponseDto;
import ru.platik777.backauth.dto.response.AuthorizationResponseDto;
import ru.platik777.backauth.entity.ApiKey;
import ru.platik777.backauth.entity.User;
import ru.platik777.backauth.mapper.ApiKeyMapper;
import ru.platik777.backauth.repository.ApiKeyRepository;
import ru.platik777.backauth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Сервис для работы с API ключами
 * Реализует логику из Go версии
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApiKeyService {

    private final ApiKeyRepository apiKeyRepository;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final ApiKeyMapper apiKeyMapper;

    /**
     * Создание API ключа (аналог ApiKeyCreate из Go)
     */
    @Transactional
    public ApiKeyDto createApiKey(ApiKeyRequestDto requestDto) {
        log.debug("Creating API key for userId: {}", requestDto.getUserId());

        // Валидация
        if (requestDto.getName() == null || requestDto.getName().trim().isEmpty()) {
            throw new RuntimeException("Name cannot be empty");
        }

        // Поиск пользователя
        User user = userRepository.findById(requestDto.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Парсинг даты истечения
        LocalDateTime expireAt = parseExpireDate(requestDto.getExpireAt());

        // Создание JWT токена для API ключа
        String apiKeyToken = jwtService.createApiKeyToken(requestDto.getUserId(), expireAt);

        // Создание сущности API ключа
        ApiKey apiKey = apiKeyMapper.toEntity(requestDto, user, apiKeyToken);
        apiKey = apiKeyRepository.save(apiKey);

        log.info("API key created with ID: {} for userId: {}", apiKey.getId(), requestDto.getUserId());
        return apiKeyMapper.toDto(apiKey);
    }

    /**
     * Получение API ключей пользователя (аналог ApiKeyGet из Go)
     */
    public List<ApiKeyDto> getApiKeys(Integer userId) {
        log.debug("Getting API keys for userId: {}", userId);

        List<ApiKey> apiKeys = apiKeyRepository.findByUserIdAndIsDeletedFalse(userId);
        return apiKeys.stream()
                .map(apiKeyMapper::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Удаление API ключа (аналог ApiKeyDelete из Go)
     */
    @Transactional
    public ApiResponseDto<String> deleteApiKey(Integer userId, String apiKeyToken) {
        log.debug("Deleting API key for userId: {}", userId);

        if (apiKeyToken == null || apiKeyToken.trim().isEmpty()) {
            throw new RuntimeException("API key token cannot be empty");
        }

        int deletedCount = apiKeyRepository.softDeleteByApiKeyAndUserId(apiKeyToken, userId);

        if (deletedCount == 0) {
            throw new RuntimeException("API key not found or already deleted");
        }

        log.info("API key soft deleted for userId: {}", userId);
        return ApiResponseDto.success("API key deleted successfully");
    }

    /**
     * Проверка авторизации по API ключу (аналог CheckApiKeyAuthorization из Go)
     */
    public AuthorizationResponseDto checkApiKeyAuthorization(String apiKeyToken) {
        log.debug("Checking API key authorization");

        if (apiKeyToken == null || apiKeyToken.trim().isEmpty()) {
            throw new RuntimeException("API key cannot be empty");
        }

        try {
            // Парсинг токена
            Integer userId = jwtService.parseApiKeyToken(apiKeyToken);

            // Проверка существования и валидности ключа в БД
            boolean isValid = apiKeyRepository.existsByApiKeyAndUserIdAndNotDeleted(apiKeyToken, userId);

            if (!isValid) {
                throw new RuntimeException("Invalid API key");
            }

            // Дополнительная проверка истечения
            boolean isNotExpired = apiKeyRepository.isApiKeyValid(apiKeyToken, LocalDateTime.now());

            if (!isNotExpired) {
                throw new RuntimeException("API key expired");
            }

            log.debug("API key authorization successful for userId: {}", userId);
            return AuthorizationResponseDto.success(userId);

        } catch (Exception e) {
            log.debug("API key authorization failed: {}", e.getMessage());
            throw new RuntimeException("Invalid API key");
        }
    }

    /**
     * Очистка просроченных API ключей (запускается по расписанию)
     */
    @Scheduled(fixedRate = 3600000) // Каждый час
    @Transactional
    public void cleanupExpiredApiKeys() {
        log.debug("Starting cleanup of expired API keys");

        int deletedCount = apiKeyRepository.markExpiredKeysAsDeleted(LocalDateTime.now());

        if (deletedCount > 0) {
            log.info("Marked {} expired API keys as deleted", deletedCount);
        }
    }

    /**
     * Проверка валидности API ключа без авторизации
     */
    public boolean isApiKeyValid(String apiKeyToken) {
        if (apiKeyToken == null || apiKeyToken.trim().isEmpty()) {
            return false;
        }

        try {
            // Проверка токена
            Integer userId = jwtService.parseApiKeyToken(apiKeyToken);

            // Проверка в БД
            return apiKeyRepository.isApiKeyValid(apiKeyToken, LocalDateTime.now());

        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Получение информации об API ключе
     */
    public ApiKeyDto getApiKeyInfo(String apiKeyToken) {
        ApiKey apiKey = apiKeyRepository.findByApiKeyWithUser(apiKeyToken)
                .orElseThrow(() -> new RuntimeException("API key not found"));

        return apiKeyMapper.toDto(apiKey);
    }

    /**
     * Парсинг даты истечения из строки
     */
    private LocalDateTime parseExpireDate(String expireAtStr) {
        if (expireAtStr == null || expireAtStr.equals("2099-01-01")) {
            // Бесконечный ключ
            return LocalDateTime.of(2099, 1, 1, 23, 59, 59);
        }

        try {
            // Парсинг даты и добавление времени до конца дня
            String[] parts = expireAtStr.split("-");
            int year = Integer.parseInt(parts[0]);
            int month = Integer.parseInt(parts[1]);
            int day = Integer.parseInt(parts[2]);

            return LocalDateTime.of(year, month, day, 23, 59, 59);

        } catch (Exception e) {
            log.error("Error parsing expire date: {}", expireAtStr, e);
            throw new RuntimeException("Invalid expire date format");
        }
    }

    /**
     * Получение статистики API ключей пользователя
     */
    public ApiResponseDto<?> getApiKeyStats(Integer userId) {
        List<ApiKey> allKeys = apiKeyRepository.findByUserId(userId);
        long activeKeys = allKeys.stream().filter(key -> !key.getIsDeleted()).count();
        long expiredKeys = allKeys.stream()
                .filter(key -> !key.getIsDeleted() && key.getExpireAt().isBefore(LocalDateTime.now()))
                .count();

        return ApiResponseDto.success(new ApiKeyStats(allKeys.size(), (int) activeKeys, (int) expiredKeys));
    }

    // Внутренний класс для статистики
    public record ApiKeyStats(int total, int active, int expired) {}
}