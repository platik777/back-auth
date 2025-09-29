package ru.platik777.backauth.service;

import ru.platik777.backauth.config.properties.AppProperties;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

/**
 * Сервис для работы с JWT токенами
 * Полностью соответствует jwt.go из Go проекта
 *
 * Включает создание и валидацию:
 * - App Access/Refresh токенов
 * - Base Access/Refresh токенов
 * - ResetPassword токенов
 * - API Key токенов
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JwtService {

    private final AppProperties appProperties;

    // ============================================
    // Создание токенов (аналог create* функций из Go)
    // ============================================

    /**
     * Создание App Access токена
     * Аналог createAppAccessAndRefreshTokens (часть 1)
     */
    public String createAppAccessToken(Integer userId) {
        log.debug("Creating app access token for userId: {}", userId);
        return createToken(
                userId,
                appProperties.getJwt().getApp().getAccess().getSecret(),
                appProperties.getJwt().getApp().getAccess().getExpiration()
        );
    }

    /**
     * Создание App Refresh токена
     * Аналог createAppAccessAndRefreshTokens (часть 2)
     */
    public String createAppRefreshToken(Integer userId) {
        log.debug("Creating app refresh token for userId: {}", userId);
        return createToken(
                userId,
                appProperties.getJwt().getApp().getRefresh().getSecret(),
                appProperties.getJwt().getApp().getRefresh().getExpiration()
        );
    }

    /**
     * Создание Base Access токена
     * Аналог createBaseAccessAndRefreshTokens (часть 1)
     */
    public String createBaseAccessToken(Integer userId) {
        log.debug("Creating base access token for userId: {}", userId);
        return createToken(
                userId,
                appProperties.getJwt().getBase().getAccess().getSecret(),
                appProperties.getJwt().getBase().getAccess().getExpiration()
        );
    }

    /**
     * Создание Base Refresh токена
     * Аналог createBaseAccessAndRefreshTokens (часть 2)
     */
    public String createBaseRefreshToken(Integer userId) {
        log.debug("Creating base refresh token for userId: {}", userId);
        return createToken(
                userId,
                appProperties.getJwt().getBase().getRefresh().getSecret(),
                appProperties.getJwt().getBase().getRefresh().getExpiration()
        );
    }

    /**
     * Создание токена для сброса пароля
     * Аналог createTokenForResetPassword из Go
     */
    public String createResetPasswordToken(Integer userId) {
        log.debug("Creating reset password token for userId: {}", userId);
        return createToken(
                userId,
                appProperties.getJwt().getResetPassword().getSecret(),
                appProperties.getJwt().getResetPassword().getExpiration()
        );
    }

    /**
     * Создание API Key токена
     * Аналог createApiKeyToken из Go
     */
    public String createApiKeyToken(Integer userId, LocalDateTime expireAt) {
        log.debug("Creating API key token for userId: {} with expireAt: {}", userId, expireAt);

        Date expirationDate = Date.from(expireAt.atZone(ZoneId.systemDefault()).toInstant());
        Date issuedAt = new Date();

        SecretKey key = Keys.hmacShaKeyFor(
                appProperties.getJwt().getApiKey().getSecret().getBytes(StandardCharsets.UTF_8)
        );

        return Jwts.builder()
                .setSubject(userId.toString())
                .setIssuedAt(issuedAt)
                .setExpiration(expirationDate)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    // ============================================
    // Парсинг и валидация токенов (аналог parseToken из Go)
    // ============================================

    /**
     * Парсинг App Access токена
     * Аналог parseToken с signingAppKeyAccess
     */
    public Integer parseAppAccessToken(String token) {
        log.debug("Parsing app access token");
        return parseToken(token, appProperties.getJwt().getApp().getAccess().getSecret());
    }

    /**
     * Парсинг App Refresh токена
     * Аналог parseToken с signingAppKeyRefresh
     */
    public Integer parseAppRefreshToken(String token) {
        log.debug("Parsing app refresh token");
        return parseToken(token, appProperties.getJwt().getApp().getRefresh().getSecret());
    }

    /**
     * Парсинг Base Access токена
     * Аналог parseToken с signingBaseKeyAccess
     */
    public Integer parseBaseAccessToken(String token) {
        log.debug("Parsing base access token");
        return parseToken(token, appProperties.getJwt().getBase().getAccess().getSecret());
    }

    /**
     * Парсинг Base Refresh токена
     * Аналог parseToken с signingBaseKeyRefresh
     */
    public Integer parseBaseRefreshToken(String token) {
        log.debug("Parsing base refresh token");
        return parseToken(token, appProperties.getJwt().getBase().getRefresh().getSecret());
    }

    /**
     * Парсинг Reset Password токена
     * Аналог parseToken с signingKeyResetPassword
     */
    public Integer parseResetPasswordToken(String token) {
        log.debug("Parsing reset password token");
        return parseToken(token, appProperties.getJwt().getResetPassword().getSecret());
    }

    /**
     * Парсинг API Key токена
     * Аналог parseToken с SigningKeyApiKey
     */
    public Integer parseApiKeyToken(String token) {
        log.debug("Parsing API key token");
        return parseToken(token, appProperties.getJwt().getApiKey().getSecret());
    }

    // ============================================
    // Валидация токенов с обработкой ошибок (validate*)
    // ============================================

    /**
     * Валидация App Access токена с полной обработкой ошибок
     */
    public Integer validateAppAccessToken(String token) {
        return validateTokenWithErrorHandling(
                token,
                appProperties.getJwt().getApp().getAccess().getSecret(),
                "App Access"
        );
    }

    /**
     * Валидация App Refresh токена с полной обработкой ошибок
     */
    public Integer validateAppRefreshToken(String token) {
        return validateTokenWithErrorHandling(
                token,
                appProperties.getJwt().getApp().getRefresh().getSecret(),
                "App Refresh"
        );
    }

    /**
     * Валидация Base Access токена с полной обработкой ошибок
     */
    public Integer validateBaseAccessToken(String token) {
        return validateTokenWithErrorHandling(
                token,
                appProperties.getJwt().getBase().getAccess().getSecret(),
                "Base Access"
        );
    }

    /**
     * Валидация Base Refresh токена с полной обработкой ошибок
     */
    public Integer validateBaseRefreshToken(String token) {
        return validateTokenWithErrorHandling(
                token,
                appProperties.getJwt().getBase().getRefresh().getSecret(),
                "Base Refresh"
        );
    }

    /**
     * Валидация Reset Password токена с полной обработкой ошибок
     */
    public Integer validateResetPasswordToken(String token) {
        return validateTokenWithErrorHandling(
                token,
                appProperties.getJwt().getResetPassword().getSecret(),
                "Reset Password"
        );
    }

    /**
     * Валидация API Key токена с полной обработкой ошибок
     */
    public Integer validateApiKeyToken(String token) {
        return validateTokenWithErrorHandling(
                token,
                appProperties.getJwt().getApiKey().getSecret(),
                "API Key"
        );
    }

    // ============================================
    // Проверка истечения токена без выброса исключения
    // ============================================

    /**
     * Проверка, истек ли токен
     * Возвращает true если токен истек или невалиден
     */
    public boolean isTokenExpired(String token, String secret) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));

            Claims claims = Jwts.parser()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            return claims.getExpiration().before(new Date());
        } catch (ExpiredJwtException e) {
            return true;
        } catch (Exception e) {
            log.warn("Token validation failed: {}", e.getMessage());
            return true;
        }
    }

    /**
     * Извлечение userId из токена без полной валидации
     * Работает даже для истекших токенов
     */
    public Integer extractUserIdFromToken(String token, String secret) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));

            Claims claims = Jwts.parser()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            return Integer.parseInt(claims.getSubject());
        } catch (ExpiredJwtException e) {
            // Токен истек, но можем извлечь userId из claims
            return Integer.parseInt(e.getClaims().getSubject());
        } catch (Exception e) {
            log.error("Failed to extract userId from token", e);
            throw new RuntimeException("Invalid token format");
        }
    }

    // ============================================
    // Приватные вспомогательные методы
    // ============================================

    /**
     * Универсальный метод создания токена
     * Аналог общей логики из jwt.go
     */
    private String createToken(Integer userId, String secret, Long expirationMs) {
        if (userId == null) {
            throw new IllegalArgumentException("UserId cannot be null");
        }
        if (secret == null || secret.isEmpty()) {
            throw new IllegalArgumentException("Secret cannot be empty");
        }

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMs);

        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));

        return Jwts.builder()
                .setSubject(userId.toString())
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Универсальный метод парсинга токена
     * Аналог parseToken из Go
     * Возвращает userId или выбрасывает исключение
     */
    private Integer parseToken(String token, String secret) {
        if (token == null || token.trim().isEmpty()) {
            throw new IllegalArgumentException("Token cannot be empty");
        }

        try {
            SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));

            Claims claims = Jwts.parser()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            String userIdStr = claims.getSubject();
            if (userIdStr == null || userIdStr.isEmpty()) {
                throw new RuntimeException("UserId is empty in token");
            }

            return Integer.parseInt(userIdStr);

        } catch (ExpiredJwtException e) {
            log.warn("Token expired");
            throw new RuntimeException("Token expired", e);
        } catch (UnsupportedJwtException e) {
            log.error("Unsupported JWT token");
            throw new RuntimeException("Unsupported token", e);
        } catch (MalformedJwtException e) {
            log.error("Invalid JWT token format");
            throw new RuntimeException("Invalid token format", e);
        } catch (SignatureException e) {
            log.error("Invalid JWT signature");
            throw new RuntimeException("Invalid signature", e);
        } catch (IllegalArgumentException e) {
            log.error("JWT claims string is empty");
            throw new RuntimeException("Empty claims", e);
        } catch (Exception e) {
            log.error("JWT parsing failed", e);
            throw new RuntimeException("Token parsing failed", e);
        }
    }

    /**
     * Валидация токена с детальной обработкой ошибок
     * Аналог логики валидации из Go с проверкой различных типов ошибок
     */
    private Integer validateTokenWithErrorHandling(String token, String secret, String tokenType) {
        if (token == null || token.trim().isEmpty()) {
            log.error("{} token is empty", tokenType);
            throw new RuntimeException(tokenType + " token cannot be empty");
        }

        try {
            SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));

            Claims claims = Jwts.parser()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            String userIdStr = claims.getSubject();
            if (userIdStr == null || userIdStr.isEmpty()) {
                log.error("UserId is empty in {} token", tokenType);
                throw new RuntimeException("UserId is empty in token");
            }

            Integer userId = Integer.parseInt(userIdStr);
            log.debug("{} token validated successfully for userId: {}", tokenType, userId);

            return userId;

        } catch (ExpiredJwtException e) {
            log.warn("{} token expired", tokenType);
            throw new RuntimeException(tokenType + " token expired");
        } catch (UnsupportedJwtException e) {
            log.error("Unsupported {} token", tokenType);
            throw new RuntimeException("Unsupported " + tokenType + " token");
        } catch (MalformedJwtException e) {
            log.error("Invalid {} token format", tokenType);
            throw new RuntimeException("Invalid " + tokenType + " token format");
        } catch (SignatureException e) {
            log.error("Invalid {} token signature", tokenType);
            throw new RuntimeException("Invalid " + tokenType + " token signature");
        } catch (IllegalArgumentException e) {
            log.error("{} token claims string is empty", tokenType);
            throw new RuntimeException(tokenType + " token claims are empty");
        } catch (Exception e) {
            log.error("{} token validation failed: {}", tokenType, e.getMessage());
            throw new RuntimeException(tokenType + " token validation failed");
        }
    }
}