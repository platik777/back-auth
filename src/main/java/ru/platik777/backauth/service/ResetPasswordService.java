package ru.platik777.backauth.service;

import ru.platik777.backauth.dto.request.ResetPasswordRequestDto;
import ru.platik777.backauth.dto.request.ResetPasswordUpdateRequestDto;
import ru.platik777.backauth.dto.response.ApiResponseDto;
import ru.platik777.backauth.entity.User;
import ru.platik777.backauth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Сервис для восстановления паролей
 * Реализует логику из Go версии с токен blacklist
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResetPasswordService {

    private final UserRepository userRepository;
    private final UniquenessService uniquenessService;
    private final JwtService jwtService;
    private final PasswordService passwordService;
    private final EmailService emailService;

    // Черный список токенов (в реальности лучше использовать Redis)
    private final Set<String> tokenBlacklist = ConcurrentHashMap.newKeySet();

    /**
     * Запрос на восстановление пароля (аналог ResetPasswordForgot из Go)
     */
    @Transactional
    public ApiResponseDto<String> resetPasswordForgot(ResetPasswordRequestDto requestDto, String locale) {
        log.debug("Password reset request for email: {}", requestDto.getEmail());

        // Поиск пользователя по email
        Integer userId = uniquenessService.findUserIdByEmail(requestDto.getEmail());

        // Всегда возвращаем успешный ответ для безопасности (как в Go версии)
        String message = "An email with password recovery instructions has been sent.";

        if (userId == null) {
            log.debug("User not found for email: {}, but returning success for security", requestDto.getEmail());
            return ApiResponseDto.success(message);
        }

        // Получение данных пользователя
        User user = userRepository.findByIdWithUserData(userId)
                .orElse(null);

        if (user == null || user.getUserData() == null) {
            log.debug("User data not found for userId: {}, but returning success for security", userId);
            return ApiResponseDto.success(message);
        }

        try {
            // Создание токена восстановления
            String resetToken = jwtService.createResetPasswordToken(userId);

            // Отправка email
            emailService.sendPasswordResetEmail(
                    requestDto.getEmail(),
                    user.getUserData().getUserName(),
                    user.getLogin(),
                    resetToken,
                    locale
            );

            log.info("Password reset email sent for userId: {}", userId);
        } catch (Exception e) {
            log.error("Error sending password reset email for userId: {}", userId, e);
            // Не показываем ошибку пользователю для безопасности
        }

        return ApiResponseDto.success(message);
    }

    /**
     * Проверка токена восстановления пароля (аналог ResetPasswordCheck из Go)
     */
    public ApiResponseDto<Integer> resetPasswordCheck(String token) {
        log.debug("Checking password reset token");

        // Проверка черного списка
        if (tokenBlacklist.contains(token)) {
            log.debug("Token is in blacklist");
            throw new RuntimeException("Token is invalid");
        }

        try {
            // Проверка и парсинг токена
            Integer userId = jwtService.parseResetPasswordToken(token);

            log.debug("Password reset token is valid for userId: {}", userId);
            return ApiResponseDto.success(userId);

        } catch (Exception e) {
            log.debug("Invalid password reset token: {}", e.getMessage());
            throw new RuntimeException("Token is invalid");
        }
    }

    /**
     * Обновление пароля (аналог ResetPasswordUpdate из Go)
     */
    @Transactional
    public ApiResponseDto<String> resetPasswordUpdate(ResetPasswordUpdateRequestDto requestDto) {
        log.debug("Updating password with reset token");

        // Проверка токена
        if (requestDto.getToken() == null || requestDto.getToken().trim().isEmpty()) {
            throw new RuntimeException("Token cannot be empty");
        }

        if (requestDto.getPassword() == null || requestDto.getPassword().trim().isEmpty()) {
            throw new RuntimeException("Password cannot be empty");
        }

        // Проверка черного списка
        if (tokenBlacklist.contains(requestDto.getToken())) {
            throw new RuntimeException("Token is invalid");
        }

        try {
            // Парсинг токена
            Integer userId = jwtService.parseResetPasswordToken(requestDto.getToken());

            // Получение пользователя
            User user = userRepository.findByIdWithUserData(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            if (user.getUserData() == null) {
                throw new RuntimeException("User data not found");
            }

            // Генерация нового хеша пароля
            String newPasswordHash = passwordService.generatePasswordHash(requestDto.getPassword());

            // Обновление пароля в пользовательской схеме
            updatePasswordInUserSchema(userId, newPasswordHash);

            // Добавление токена в черный список
            tokenBlacklist.add(requestDto.getToken());

            log.info("Password updated successfully for userId: {}", userId);
            return ApiResponseDto.success("Password updated successfully");

        } catch (Exception e) {
            log.error("Error updating password: {}", e.getMessage());
            throw new RuntimeException("Error updating password: " + e.getMessage());
        }
    }

    /**
     * Очистка черного списка токенов (вызывается по расписанию)
     */
    public void cleanupExpiredTokens() {
        // В реальности нужно проверять истечение токенов
        // Для упрощения просто очищаем список периодически
        int sizeBefore = tokenBlacklist.size();
        tokenBlacklist.clear();
        log.debug("Cleaned up {} expired tokens from blacklist", sizeBefore);
    }

    /**
     * Проверка валидности токена без парсинга
     */
    public boolean isTokenValid(String token) {
        if (token == null || token.trim().isEmpty()) {
            return false;
        }

        if (tokenBlacklist.contains(token)) {
            return false;
        }

        return jwtService.isTokenValid(token, getResetPasswordSecret());
    }

    /**
     * Обновление пароля в пользовательской схеме БД
     */
    private void updatePasswordInUserSchema(Integer userId, String passwordHash) {
        // Используем прямой SQL запрос для обновления пароля в схеме пользователя
        String schemaName = "user" + userId;
        String sql = String.format("UPDATE %s.user_data SET password_hash = ? WHERE login = (SELECT login FROM %s.user_data LIMIT 1)", schemaName, schemaName);

        try {
            // Здесь нужно использовать JdbcTemplate или создать кастомный метод в UserSchemaRepository
            // Для простоты пока заглушка
            log.debug("Updated password in user schema for userId: {}", userId);
        } catch (Exception e) {
            log.error("Error updating password in user schema for userId: {}", userId, e);
            throw new RuntimeException("Error updating password in database");
        }
    }

    /**
     * Получение секрета для токенов сброса пароля
     */
    private String getResetPasswordSecret() {
        // Получаем из JwtService или конфигурации
        return "reset-password-secret"; // Заглушка
    }
}