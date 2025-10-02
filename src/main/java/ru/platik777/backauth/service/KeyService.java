package ru.platik777.backauth.service;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.platik777.backauth.repository.SaltRepository;

/**
 * Сервис управления ключами подписи
 * Соответствует key.go и KeyerInterface
 *
 * ВАЖНО: Ключи формируются динамически из константы (конфиг) + соль (БД)
 * Это точное соответствие Go версии, но с константами в конфигурации
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KeyService {
    // Константы префиксов из конфигурации (соответствуют models.Const* в Go)
    @Value("${app.jwt.constants.app-access-prefix:app_access_key_}")
    private String constSigningAppKeyAccess;

    @Value("${app.jwt.constants.app-refresh-prefix:app_refresh_key_}")
    private String constSigningAppKeyRefresh;

    @Value("${app.jwt.constants.base-access-prefix:base_access_key_}")
    private String constSigningBaseKeyAccess;

    @Value("${app.jwt.constants.base-refresh-prefix:base_refresh_key_}")
    private String constSigningBaseKeyRefresh;

    @Value("${app.jwt.constants.reset-password-prefix:reset_password_key_}")
    private String constSigningKeyResetPassword;

    @Value("${app.jwt.constants.salt}")
    private String salt;

    // ВАЖНО: API Key - статическая константа (НЕ конкатенируется с солью!)
    // В Go: models.SigningKeyApiKey используется напрямую без соли
    @Value("${app.jwt.api-key.secret}")
    private String signingKeyApiKey;

    // Динамически формируемые ключи (константа + соль из БД)
    private String signingAppKeyAccess;
    private String signingAppKeyRefresh;
    private String signingBaseKeyAccess;
    private String signingBaseKeyRefresh;
    private String signingKeyResetPassword;

    /**
     * -- GETTER --
     *  Проверка инициализации сервиса
     */
    // Флаг инициализации
    @Getter
    private volatile boolean initialized = false;

    /**
     * Инициализация ключей с уникальной солью из БД
     * Go: func NewKeyInitialization(uniqueSalt string)
     *
     * Формула: ключ = константа_из_конфига + соль_из_бд
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

            // Валидация API Key
            if (signingKeyApiKey == null || signingKeyApiKey.isEmpty()) {
                log.error("API Key secret is not configured");
                throw new IllegalStateException("API Key secret must be configured");
            }

            this.initialized = true;

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

        // Проверка минимальной длины для безопасности
        if (signingAppKeyAccess.length() < 32) {
            log.warn("App Access Key is too short (< 32 chars)");
        }
    }

    // ==================== GETTERS ====================

    /**
     * Получение ключа для подписи App Access токена
     * Go: GetSigningAppKeyAccess()
     */
    public String getSigningAppKeyAccess() {
        ensureInitialized();
        return signingAppKeyAccess;
    }

    /**
     * Получение ключа для подписи App Refresh токена
     * Go: GetSigningAppKeyRefresh()
     */
    public String getSigningAppKeyRefresh() {
        ensureInitialized();
        return signingAppKeyRefresh;
    }

    /**
     * Получение ключа для подписи Base Access токена
     * Go: GetSigningBaseKeyAccess()
     */
    public String getSigningBaseKeyAccess() {
        ensureInitialized();
        return signingBaseKeyAccess;
    }

    /**
     * Получение ключа для подписи Base Refresh токена
     * Go: GetSigningBaseKeyRefresh()
     */
    public String getSigningBaseKeyRefresh() {
        ensureInitialized();
        return signingBaseKeyRefresh;
    }

    /**
     * Получение ключа для токена сброса пароля
     * Go: GetSigningKeyResetPassword()
     */
    public String getSigningKeyResetPassword() {
        ensureInitialized();
        return signingKeyResetPassword;
    }

    /**
     * Получение ключа для API токенов
     *
     * ВАЖНО: В Go версии это СТАТИЧЕСКАЯ константа models.SigningKeyApiKey
     * НЕ формируется динамически с солью из БД!
     *
     * В auth.go:
     * - parseToken(token, models.SigningKeyApiKey)
     * - Token.SignedString([]byte(models.SigningKeyApiKey))
     *
     * Go: GetSigningKeyApiKey() - возвращает статическую константу
     */
    public String getSigningKeyApiKey() {
        // API Key не требует проверки инициализации, т.к. это просто поле из конфига
        if (signingKeyApiKey == null || signingKeyApiKey.isEmpty()) {
            throw new IllegalStateException("API Key is not configured");
        }
        return signingKeyApiKey;
    }

    /**
     * Проверка инициализации перед использованием динамических ключей
     */
    private void ensureInitialized() {
        if (!initialized) {
            throw new IllegalStateException(
                    "KeyService is not initialized yet. Keys are not available."
            );
        }
    }
}