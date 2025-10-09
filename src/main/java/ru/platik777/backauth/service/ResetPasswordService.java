package ru.platik777.backauth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.platik777.backauth.dto.response.ResetPasswordCheckResponse;
import ru.platik777.backauth.dto.response.StatusResponse;
import ru.platik777.backauth.dto.response.UserResponse;
import ru.platik777.backauth.entity.User;
import ru.platik777.backauth.repository.UserRepository;
import ru.platik777.backauth.util.EmailMasker;

import java.util.UUID;

/**
 * Сервис восстановления пароля
 * Соответствует resetPassword.go
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResetPasswordService {

    private final JwtService jwtService;
    private final KeyService keyService;
    private final PasswordService passwordService;
    //private final EmailService emailService;
    private final ValidationService validationService;
    private final TokenBlacklistService tokenBlacklistService;
    private final EmailMasker emailMasker;
    private final UserRepository userRepository;

    @Value("${app.base-url}")
    private String baseUrl;

    /**
     * Генерация токена восстановления и отправка на почту
     * Go: func (a *AuthService) ResetPasswordForgot(logger go_logger.Logger,
     *                                                targetEmail, locale string)
     *
     * ВАЖНО: Всегда возвращает успешный ответ, даже если email не найден
     * (защита от перебора email адресов)
     *
     * @param targetEmail Email для восстановления
     * @param locale Язык письма (ru/en)
     * @return StatusResponse с результатом
     */
    @Transactional(readOnly = true)
    public StatusResponse resetPasswordForgot(String targetEmail, String locale) {
        log.debug("ResetPasswordForgot started for email: {}", targetEmail);

        // Валидация email
        try {
            validationService.validateEmail(targetEmail);
        } catch (Exception e) {
            log.warn("Invalid email format: {}", targetEmail);
            // Возвращаем успешный ответ для защиты от перебора
            return createSuccessResponse();
        }

        // Поиск userId по email
        // Go: userId, err := a.db.SearchUserIdByEmail(logger, targetEmail)
        User user = userRepository.findByEmail(targetEmail)
                .orElseGet(() -> {
                    log.error("Failed to get user data for email: {}", targetEmail);
                    return null;
                });
        assert user != null;
        UUID userId = user.getId();

        // Если пользователь не найден, возвращаем успешный ответ (защита от перебора)
        // Go: if errors.Is(err, models.ErrorUserNotFound) { return success }
        if (userId == null) {
            log.info("User not found by email (returning success for security): {}",
                    emailMasker.mask(targetEmail));
            return createSuccessResponse();
        }

        // Получение данных пользователя


        try {
            // Создание токена восстановления пароля
            // Go: token, err := createTokenForResetPassword(logger, userId, a.Key.GetSigningKeyResetPassword())
            String token = jwtService.createResetPasswordToken(userId);

            // Формирование URL восстановления
            // Go: resetPasswordUrl := viper.GetString("baseUrl") + "/auth/forgot-password/update/?token=" + token
            String resetPasswordUrl = baseUrl + "/auth/forgot-password/update/?token=" + token;

            // Определение subject по локали
            String subject = getSubjectByLocale(locale);

            // Отправка письма
            // Go: smtp.SendingMessage(logger, senderEmail, targetEmail, subject, "text/html; charset=UTF-8", bodyHtml)
            // emailService.sendResetPasswordEmail(
            //        targetEmail,
            //        subject,
            //        resetPasswordUrl,
            //        user.getLogin(),
            //        locale != null ? locale : "ru"
            //);

            log.info("Reset password email sent successfully to: {}", emailMasker.mask(targetEmail));

        } catch (Exception e) {
            log.error("Failed to send reset password email to: {}", emailMasker.mask(targetEmail), e);
            // Все равно возвращаем успешный ответ для защиты от перебора
        }

        return createSuccessResponse();
    }

    /**
     * Проверка валидности токена восстановления пароля
     * Go: func (a *AuthService) ResetPasswordCheck(logger go_logger.Logger, token string)
     *
     * @param token Токен восстановления
     * @return ResetPasswordCheckResponse с userId
     * @throws ResetPasswordException если токен невалидный
     */
    @Transactional(readOnly = true)
    public ResetPasswordCheckResponse resetPasswordCheck(String token) {
        log.debug("ResetPasswordCheck started");

        if (token == null || token.trim().isEmpty()) {
            throw new ResetPasswordException("Token cannot be empty");
        }

        // Проверка на нахождение в черном списке
        // Go: if ok := a.tokenBlackList.IsBlacklisted(token); ok
        if (tokenBlacklistService.isBlacklisted(token)) {
            log.warn("Token is in blacklist");
            throw new ResetPasswordException("Token is invalid");
        }

        try {
            // Проверка JWT токена
            // Go: userId, err := parseToken(token, a.Key.GetSigningKeyResetPassword())
            UUID userId = jwtService.parseToken(
                    token,
                    keyService.getSigningKeyResetPassword()
            );

            log.debug("Reset password token validated successfully for userId: {}", userId);

            return ResetPasswordCheckResponse.builder()
                    .userId(userId)
                    .build();

        } catch (JwtService.JwtException e) {
            log.warn("Invalid reset password token: {}", e.getMessage());
            throw new ResetPasswordException("Token is invalid", e);
        }
    }

    /**
     * Обновление пароля через токен восстановления
     * Go: func (a *AuthService) ResetPasswordUpdate(logger go_logger.Logger,
     *                                               token, password string)
     *
     * @param token Токен восстановления
     * @param password Новый пароль
     * @return UserResponse с данными пользователя
     * @throws ResetPasswordException если обновление не удалось
     */
    @Transactional
    public UserResponse resetPasswordUpdate(String token, String password) {
        // TODO: реализовать
        return null;
    }

    // ==================== PRIVATE METHODS ====================

    /**
     * Получение subject письма по локали
     * Go: switch locale
     */
    private String getSubjectByLocale(String locale) {
        if ("en".equalsIgnoreCase(locale)) {
            return "REPEAT: Password Recovery";
        }
        return "REPEAT: Восстановление пароля";
    }

    /**
     * Создание успешного ответа
     */
    private StatusResponse createSuccessResponse() {
        return StatusResponse.builder()
                .status(true)
                .message("An email with password recovery instructions has been sent.")
                .build();
    }

    // ==================== CUSTOM EXCEPTIONS ====================

    /**
     * Исключение при восстановлении пароля
     */
    public static class ResetPasswordException extends RuntimeException {
        public ResetPasswordException(String message) {
            super(message);
        }

        public ResetPasswordException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}