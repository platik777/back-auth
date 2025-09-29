package ru.platik777.backauth.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

@Data
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private String baseUrl;
    private String salt;

    @NestedConfigurationProperty
    private JwtProperties jwt = new JwtProperties();

    @NestedConfigurationProperty
    private SmtpProperties smtp = new SmtpProperties();

    @NestedConfigurationProperty
    private BackAccessProperties backAccess = new BackAccessProperties();

    @NestedConfigurationProperty
    private BackLogProperties backLog = new BackLogProperties();

    @Data
    public static class JwtProperties {
        @NestedConfigurationProperty
        private TokenConfig app = new TokenConfig();

        @NestedConfigurationProperty
        private TokenConfig base = new TokenConfig();

        @NestedConfigurationProperty
        private SimpleTokenConfig resetPassword = new SimpleTokenConfig();

        @NestedConfigurationProperty
        private SimpleTokenConfig apiKey = new SimpleTokenConfig();

        @Data
        public static class TokenConfig {
            @NestedConfigurationProperty
            private SimpleTokenConfig access = new SimpleTokenConfig();

            @NestedConfigurationProperty
            private SimpleTokenConfig refresh = new SimpleTokenConfig();
        }

        @Data
        public static class SimpleTokenConfig {
            private String secret;
            private long expiration;
        }
    }

    @Data
    public static class SmtpProperties {
        private boolean sendToLogs;
        private String supportEmail;
        private String productEmail;
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