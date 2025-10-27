package ru.platik777.backauth.service;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.platik777.backauth.dto.AuthenticatedUser;
import ru.platik777.backauth.dto.response.TokenResponse;
import ru.platik777.backauth.entity.types.TokenType;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Сервис работы с JWT токенами
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JwtService {

    private final KeyService keyService;
    private final TokenBlacklistService tokenBlacklistService;

    @Value("${app.jwt.app.access.expiration}")
    private long appAccessTokenExpiration;

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
     *
     * @param userId ID пользователя
     * @return Map с 4 токенами
     */
    public TokenResponse createAllTokens(String userId, String tenantId) {
        log.debug("Creating all tokens for userId: {}", userId);

        validateUserId(userId);

        Map<String, String> appTokens = createAppTokens(userId, tenantId);
        Map<String, String> baseTokens = createBaseTokens(userId, tenantId);

        return new TokenResponse(
                appTokens.get("accessToken"),
                appTokens.get("refreshToken"),
                baseTokens.get("accessToken"),
                baseTokens.get("refreshToken")
        );
    }

    /**
     * Создание App Access и Refresh токенов
     *
     * @param userId ID пользователя
     * @return Map с accessToken и refreshToken
     */
    public Map<String, String> createAppTokens(String userId, String tenantId) {
        log.debug("Creating App tokens for userId: {}", userId);

        validateUserId(userId);

        String accessToken = createToken(
                userId,
                tenantId,
                appAccessTokenExpiration,
                keyService.getSigningAppKeyAccess(),
                TokenType.APP_ACCESS
        );

        String refreshToken = createToken(
                userId,
                tenantId,
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
     *
     * @param userId ID пользователя
     * @return Map с accessToken и refreshToken
     */
    public Map<String, String> createBaseTokens(String userId, String tenantId) {
        log.debug("Creating Base tokens for userId: {}", userId);

        validateUserId(userId);

        String accessToken = createToken(
                userId,
                tenantId,
                baseAccessTokenExpiration,
                keyService.getSigningBaseKeyAccess(),
                TokenType.BASE_ACCESS
        );

        String refreshToken = createToken(
                userId,
                tenantId,
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
     *
     * @param userId ID пользователя
     * @return Reset password токен
     */
    public String createResetPasswordToken(String userId, String tenantId) {
        log.debug("Creating reset password token for userId: {}", userId);

        validateUserId(userId);

        String token = createToken(
                userId,
                tenantId,
                resetPasswordExpiration,
                keyService.getSigningKeyResetPassword(),
                TokenType.RESET_PASSWORD
        );

        log.debug("Reset password token created for userId: {}", userId);
        return token;
    }

    /**
     * Создание API ключа с кастомным временем истечения
     *
     * @param userId ID пользователя
     * @param expireAt Дата истечения
     * @return API ключ токен
     */
    public String createApiKeyToken(String userId, LocalDateTime expireAt) {
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
     *
     * @param token JWT токен
     * @param signingKey Ключ для валидации
     * @return userId из токена
     * @throws JwtException если токен невалидный
     */
    public AuthenticatedUser parseToken(String token, String signingKey) {
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
            String tenantId = claims.get("tenant_id", String.class);

            if (userId == null) {
                throw new JwtException("UserId is empty in token");
            }

            log.debug("Token parsed successfully. UserId: {}, TenantId: {}", userId, tenantId);
            return new AuthenticatedUser(userId, tenantId);

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

    /**
     * Базовый метод создания токена
     *
     * @param userId ID пользователя
     * @param validityMillis Время жизни в миллисекундах
     * @param signingKey Ключ для подписи
     * @param tokenType Тип токена (для отладки и claims)
     * @return JWT токен
     */
    private String createToken(String userId, String tenantId, long validityMillis,
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
                    .claim("tenant_id", tenantId)
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
    private void validateUserId(String userId) {
        if (userId == null) {
            throw new IllegalArgumentException("Invalid userId");
        }
    }
}