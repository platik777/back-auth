package ru.platik777.backauth.service;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Сервис работы с JWT токенами
 * Соответствует jwt.go
 *
 * ВАЖНО: Использует JJWT 0.11.x/0.12.x API
 * Время жизни токенов берется из конфигурации
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JwtService {

    private final KeyService keyService;
    private final TokenBlacklistService tokenBlacklistService;

    // Время жизни токенов из конфигурации (в миллисекундах)
    // Go: const TimeAToken = 10 * time.Minute
    @Value("${app.jwt.app.access.expiration}")
    private long appAccessTokenExpiration;

    // Go: const TimeRToken = 8 * time.Hour
    @Value("${app.jwt.app.refresh.expiration}")
    private long appRefreshTokenExpiration;

    @Value("${app.jwt.base.access.expiration}")
    private long baseAccessTokenExpiration;

    @Value("${app.jwt.base.refresh.expiration}")
    private long baseRefreshTokenExpiration;

    @Value("${app.jwt.reset-password.expiration}")
    private long resetPasswordExpiration;

    /**
     * Создание всех типов токенов (App + Base)
     * Go: используется в RefreshAppTokenByBaseToken
     *
     * @param userId ID пользователя
     * @return Map с 4 токенами
     */
    public TokensResponse createAllTokens(UUID userId) {
        log.debug("Creating all tokens for userId: {}", userId);

        validateUserId(userId);

        Map<String, String> appTokens = createAppTokens(userId);
        Map<String, String> baseTokens = createBaseTokens(userId);

        return new TokensResponse(
                appTokens.get("accessToken"),
                appTokens.get("refreshToken"),
                baseTokens.get("accessToken"),
                baseTokens.get("refreshToken")
        );
    }

    /**
     * Создание App Access и Refresh токенов
     * Go: func createAppAccessAndRefreshTokens(logger go_logger.Logger, userId int,
     *                                          signingAppKeyAccess, signingAppKeyRefresh string)
     *
     * @param userId ID пользователя
     * @return Map с accessToken и refreshToken
     */
    public Map<String, String> createAppTokens(UUID userId) {
        log.debug("Creating App tokens for userId: {}", userId);

        validateUserId(userId);

        String accessToken = createToken(
                userId,
                appAccessTokenExpiration,
                keyService.getSigningAppKeyAccess(),
                TokenType.APP_ACCESS
        );

        String refreshToken = createToken(
                userId,
                appRefreshTokenExpiration,
                keyService.getSigningAppKeyRefresh(),
                TokenType.APP_REFRESH
        );

        Map<String, String> tokens = new HashMap<>();
        tokens.put("accessToken", accessToken);
        tokens.put("refreshToken", refreshToken);

        log.debug("App tokens created successfully for userId: {}", userId);
        return tokens;
    }

    /**
     * Создание Base Access и Refresh токенов
     * Go: func createBaseAccessAndRefreshTokens(logger go_logger.Logger, userId int,
     *                                           signingBaseKeyAccess, signingBaseKeyRefresh string)
     *
     * @param userId ID пользователя
     * @return Map с accessToken и refreshToken
     */
    public Map<String, String> createBaseTokens(UUID userId) {
        log.debug("Creating Base tokens for userId: {}", userId);

        validateUserId(userId);

        String accessToken = createToken(
                userId,
                baseAccessTokenExpiration,
                keyService.getSigningBaseKeyAccess(),
                TokenType.BASE_ACCESS
        );

        String refreshToken = createToken(
                userId,
                baseRefreshTokenExpiration,
                keyService.getSigningBaseKeyRefresh(),
                TokenType.BASE_REFRESH
        );

        Map<String, String> tokens = new HashMap<>();
        tokens.put("accessToken", accessToken);
        tokens.put("refreshToken", refreshToken);

        log.debug("Base tokens created successfully for userId: {}", userId);
        return tokens;
    }

    /**
     * Создание токена для сброса пароля
     * Go: func createTokenForResetPassword(logger go_logger.Logger, userId int,
     *                                      signingKeyResetPassword string)
     *
     * @param userId ID пользователя
     * @return Reset password токен
     */
    public String createResetPasswordToken(UUID userId) {
        log.debug("Creating reset password token for userId: {}", userId);

        validateUserId(userId);

        String token = createToken(
                userId,
                resetPasswordExpiration,
                keyService.getSigningKeyResetPassword(),
                TokenType.RESET_PASSWORD
        );

        log.debug("Reset password token created for userId: {}", userId);
        return token;
    }

    /**
     * Создание API ключа с кастомным временем истечения
     * Go: func createApiKeyToken(logger go_logger.Logger, userId int,
     *                            timeExpiration time.Time)
     *
     * @param userId ID пользователя
     * @param expireAt Дата истечения
     * @return API ключ токен
     */
    public String createApiKeyToken(UUID userId, LocalDateTime expireAt) {
        log.debug("Creating API key token for userId: {} with expiration: {}",
                userId, expireAt);

        validateUserId(userId);

        if (expireAt == null || expireAt.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Invalid expiration date for API key");
        }

        long expirationMillis = expireAt
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli();

        Date issuedAt = new Date();
        Date expiration = new Date(expirationMillis);

        SecretKey key = getSecretKey(keyService.getSigningKeyApiKey());

        String token = Jwts.builder()
                .claim("userId", userId)
                .claim("type", TokenType.API_KEY.name())
                .setIssuedAt(issuedAt)
                .setExpiration(expiration)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

        log.debug("API key token created for userId: {}", userId);
        return token;
    }

    /**
     * Парсинг токена и получение userId
     * Go: func parseToken(token, key string) (userId int, err error)
     *
     * @param token JWT токен
     * @param signingKey Ключ для валидации
     * @return userId из токена
     * @throws JwtException если токен невалидный
     */
    public UUID parseToken(String token, String signingKey) {
        if (token == null || token.isEmpty()) {
            throw new IllegalArgumentException("Token cannot be empty");
        }

        if (signingKey == null || signingKey.isEmpty()) {
            throw new IllegalArgumentException("Signing key cannot be empty");
        }

        try {
            SecretKey key = getSecretKey(signingKey);

            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String userId = claims.get("userId", String.class);

            // Go: if claims.UserId == 0
            if (userId == null) {
                throw new JwtException("UserId is empty in token");
            }

            log.debug("Token parsed successfully. UserId: {}", userId);
            return UUID.fromString(userId);

        } catch (ExpiredJwtException e) {
            log.debug("Token expired: {}", e.getMessage());
            throw new JwtException("Token expired", e);

        } catch (MalformedJwtException e) {
            log.warn("Malformed token: {}", e.getMessage());
            throw new JwtException("Invalid token format", e);

        } catch (io.jsonwebtoken.security.SecurityException e) {
            log.warn("Invalid signature: {}", e.getMessage());
            throw new JwtException("Invalid token signature", e);

        } catch (Exception e) {
            log.error("Unexpected error parsing token", e);
            throw new JwtException("Token parsing error", e);
        }
    }

    /**
     * Проверка токена на нахождение в черном списке
     * Используется для reset password токенов (одноразовые)
     *
     * @param token JWT токен
     * @return true если токен в blacklist
     */
    public boolean isTokenBlacklisted(String token) {
        return tokenBlacklistService.isBlacklisted(token);
    }

    /**
     * Добавление токена в черный список
     * Используется после использования reset password токена
     *
     * @param token JWT токен
     */
    public void addToBlacklist(String token) {
        if (token != null && !token.isEmpty()) {
            tokenBlacklistService.addToken(token);
            log.debug("Token added to blacklist");
        }
    }

    // ==================== PRIVATE METHODS ====================

    /**
     * Базовый метод создания токена
     *
     * @param userId ID пользователя
     * @param validityMillis Время жизни в миллисекундах
     * @param signingKey Ключ для подписи
     * @param tokenType Тип токена (для отладки и claims)
     * @return JWT токен
     */
    private String createToken(UUID userId, long validityMillis,
                               String signingKey, TokenType tokenType) {

        validateUserId(userId);

        if (validityMillis <= 0) {
            throw new IllegalArgumentException(
                    "Token validity must be positive: " + validityMillis
            );
        }

        Date now = new Date();
        Date expiration = new Date(now.getTime() + validityMillis);

        try {
            SecretKey key = getSecretKey(signingKey);

            String token = Jwts.builder()
                    .claim("userId", userId)
                    .claim("type", tokenType.name())
                    .setIssuedAt(now)
                    .setExpiration(expiration)
                    .signWith(key, SignatureAlgorithm.HS256)
                    .compact();

            log.debug("Token created: type={}, userId={}, validity={}ms",
                    tokenType, userId, validityMillis);

            return token;

        } catch (Exception e) {
            log.error("Error creating {} token for userId: {}", tokenType, userId, e);
            throw new RuntimeException("Failed to create token", e);
        }
    }

    /**
     * Создание SecretKey из строки
     *
     * ВАЖНО: JJWT требует минимум 256 бит (32 байта) для HS256
     * Если ключ короче - используем SHA-256 для создания ключа нужной длины
     *
     * @param keyString Ключ из конфигурации
     * @return SecretKey для подписи
     */
    private SecretKey getSecretKey(String keyString) {
        try {
            byte[] keyBytes = keyString.getBytes(StandardCharsets.UTF_8);

            // JJWT требует минимум 256 бит (32 байта) для HS256
            if (keyBytes.length < 32) {
                log.debug("Key is shorter than 32 bytes, using SHA-256 hash");

                // Используем SHA-256 для получения ключа нужной длины
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                keyBytes = digest.digest(keyBytes);
            }

            return Keys.hmacShaKeyFor(keyBytes);

        } catch (Exception e) {
            log.error("Error creating secret key", e);
            throw new RuntimeException("Failed to create secret key", e);
        }
    }

    /**
     * Валидация userId
     */
    private void validateUserId(UUID userId) {
        if (userId == null) {
            throw new IllegalArgumentException("Invalid userId");
        }
    }

    // ==================== INNER CLASSES ====================

    /**
     * Типы токенов для отладки и логирования
     */
    private enum TokenType {
        APP_ACCESS,
        APP_REFRESH,
        BASE_ACCESS,
        BASE_REFRESH,
        RESET_PASSWORD,
        API_KEY
    }

    /**
     * DTO для всех токенов
     */
    public record TokensResponse(
            String accessAppToken,
            String refreshAppToken,
            String accessBaseToken,
            String refreshBaseToken
    ) {}

    /**
     * Кастомное исключение для JWT ошибок
     */
    public static class JwtException extends RuntimeException {
        public JwtException(String message) {
            super(message);
        }

        public JwtException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}