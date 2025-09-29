package ru.platik777.backauth.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Конфигурационные свойства приложения
 * Соответствует конфигурации из Go проекта
 */
@Data
@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    /**
     * Базовый URL приложения
     */
    private String baseUrl;

    /**
     * Настройки JWT
     */
    private JwtProperties jwt;

    /**
     * Соль для хеширования паролей
     */
    private String salt;

    /**
     * Настройки SMTP
     */
    private SmtpProperties smtp;

    /**
     * Настройки back-access сервиса
     */
    private BackAccessProperties backAccess;

    /**
     * Настройки back-log сервиса
     */
    private BackLogProperties backLog;

    // ============================================
    // Вложенные классы конфигурации
    // ============================================

    @Data
    public static class JwtProperties {
        private TokenPairProperties app;
        private TokenPairProperties base;
        private TokenProperties resetPassword;
        private TokenProperties apiKey;
    }

    @Data
    public static class TokenPairProperties {
        private TokenProperties access;
        private TokenProperties refresh;
    }

    @Data
    public static class TokenProperties {
        private String secret;
        private Long expiration;
    }

    @Data
    public static class SmtpProperties {
        /**
         * Флаг отправки в логи вместо реальной отправки (для тестирования)
         */
        private boolean sendToLogs;

        /**
         * Email для отправки в поддержку
         */
        private String supportEmail;

        /**
         * Email продукта
         */
        private String productEmail;

        /**
         * Email отправителя
         */
        private String senderEmail;
    }

    @Data
    public static class BackAccessProperties {
        private String host;
        private String port;
        private String keyEditAccess;
    }

    @Data
    public static class BackLogProperties {
        private String host;
        private String port;
    }
}