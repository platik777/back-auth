package ru.platik777.backauth.service;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Сервис управления ключами подписи
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KeyService {
    @Value("${app.jwt.constants.app-access-prefix}")
    private String constSigningAppKeyAccess;

    @Value("${app.jwt.constants.app-refresh-prefix}")
    private String constSigningAppKeyRefresh;

    @Value("${app.jwt.constants.base-access-prefix}")
    private String constSigningBaseKeyAccess;

    @Value("${app.jwt.constants.base-refresh-prefix}")
    private String constSigningBaseKeyRefresh;

    @Value("${app.jwt.constants.reset-password-prefix}")
    private String constSigningKeyResetPassword;

    @Value("${app.jwt.constants.salt}")
    private String salt;

    @Value("${app.jwt.api-key.secret}")
    private String signingKeyApiKey;

    /**
     *  Получение ключа для подписи App Access токена
     */
    @Getter
    private String signingAppKeyAccess;
    @Getter
    private String signingAppKeyRefresh;
    @Getter
    private String signingBaseKeyAccess;
    @Getter
    private String signingBaseKeyRefresh;
    @Getter
    private String signingKeyResetPassword;

    /**
     * Инициализация ключей с уникальной солью из БД
     */
    @PostConstruct
    public void initialize() {
        try {
            // Формируем ключи: константа из конфига + соль из БД
            this.signingAppKeyAccess = constSigningAppKeyAccess + salt;
            this.signingAppKeyRefresh = constSigningAppKeyRefresh + salt;
            this.signingBaseKeyAccess = constSigningBaseKeyAccess + salt;
            this.signingBaseKeyRefresh = constSigningBaseKeyRefresh + salt;
            this.signingKeyResetPassword = constSigningKeyResetPassword + salt;

            if (signingKeyApiKey == null || signingKeyApiKey.isEmpty()) {
                log.error("API Key secret is not configured");
                throw new IllegalStateException("API Key secret must be configured");
            }

            log.info("KeyService initialized successfully");
            log.debug("Keys formed from config constants + DB salt (salt length: {})", salt.length());
            logKeyInfo();

        } catch (Exception e) {
            log.error("Failed to initialize KeyService", e);
            throw new IllegalStateException("KeyService initialization failed", e);
        }
    }

    /**
     * Логирование информации о ключах (без раскрытия секретов)
     */
    private void logKeyInfo() {
        log.debug("App Access Key length: {}", signingAppKeyAccess.length());
        log.debug("App Refresh Key length: {}", signingAppKeyRefresh.length());
        log.debug("Base Access Key length: {}", signingBaseKeyAccess.length());
        log.debug("Base Refresh Key length: {}", signingBaseKeyRefresh.length());
        log.debug("Reset Password Key length: {}", signingKeyResetPassword.length());
        log.debug("API Key length: {}", signingKeyApiKey.length());

        if (signingAppKeyAccess.length() < 32) {
            log.warn("App Access Key is too short (< 32 chars)");
        }
    }

    /**
     * Получение ключа для API токенов
     */
    public String getSigningKeyApiKey() {
        if (signingKeyApiKey == null || signingKeyApiKey.isEmpty()) {
            throw new IllegalStateException("API Key is not configured");
        }
        return signingKeyApiKey;
    }
}