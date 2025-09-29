package ru.platik777.backauth.service;

import ru.platik777.backauth.config.properties.AppProperties;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

/**
 * Сервис для работы с JWT токенами
 * Реализует логику из Go версии с поддержкой App и Base токенов
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JwtService {

    private final AppProperties appProperties;

    /**
     * Создание App Access токена
     */
    public String createAppAccessToken(Integer userId) {
        return createToken(
                userId,
                appProperties.getJwt().getApp().getAccess().getSecret(),
                appProperties.getJwt().getApp().getAccess().getExpiration()
        );
    }

    /**
     * Создание App Refresh токена
     */
    public String createAppRefreshToken(Integer userId) {
        return createToken(
                userId,
                appProperties.getJwt().getApp().getRefresh().getSecret(),
                appProperties.getJwt().getApp().getRefresh().getExpiration()
        );
    }

    /**
     * Создание Base Access токена
     */
    public String createBaseAccessToken(Integer userId) {
        return createToken(
                userId,
                appProperties.getJwt().getBase().getAccess().getSecret(),
                appProperties.getJwt().getBase().getAccess().getExpiration()
        );
    }

    /**
     * Создание Base Refresh токена
     */
    public String createBaseRefreshToken(Integer userId) {
        return createToken(
                userId,
                appProperties.getJwt().getBase().getRefresh().getSecret(),
                appProperties.getJwt().getBase().getRefresh().getExpiration()
        );
    }

    /**
     * Создание токена для сброса пароля
     */
    public String createResetPasswordToken(Integer userId) {
        return createToken(
                userId,
                appProperties.getJwt().getResetPassword().getSecret(),
                appProperties.getJwt().getResetPassword().getExpiration()
        );
    }

    /**
     * Создание API ключа токена
     */
    public String createApiKeyToken(Integer userId, LocalDateTime expireAt) {
        Date expiration = Date.from(expireAt.atZone(ZoneId.systemDefault()).toInstant());

        return Jwts.builder()
                .setSubject(userId.toString())
                .claim("userId", userId)
                .setIssuedAt(new Date())
                .setExpiration(expiration)
                .signWith(getSigningKey(appProperties.getJwt().getApiKey().getSecret()), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Парсинг App Access токена
     */
    public Integer parseAppAccessToken(String token) {
        return parseToken(token, appProperties.getJwt().getApp().getAccess().getSecret());
    }

    /**
     * Парсинг App Refresh токена
     */
    public Integer parseAppRefreshToken(String token) {
        return parseToken(token, appProperties.getJwt().getApp().getRefresh().getSecret());
    }

    /**
     * Парсинг Base Access токена
     */
    public Integer parseBaseAccessToken(String token) {
        return parseToken(token, appProperties.getJwt().getBase().getAccess().getSecret());
    }

    /**
     * Парсинг Base Refresh токена
     */
    public Integer parseBaseRefreshToken(String token) {
        return parseToken(token, appProperties.getJwt().getBase().getRefresh().getSecret());
    }

    /**
     * Парсинг токена сброса пароля
     */
    public Integer parseResetPasswordToken(String token) {
        return parseToken(token, appProperties.getJwt().getResetPassword().getSecret());
    }

    /**
     * Парсинг API ключа
     */
    public Integer parseApiKeyToken(String token) {
        return parseToken(token, appProperties.getJwt().getApiKey().getSecret());
    }

    /**
     * Проверка валидности токена
     */
    public boolean isTokenValid(String token, String secret) {
        try {
            parseToken(token, secret);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Проверка истечения токена
     */
    public boolean isTokenExpired(String token, String secret) {
        try {
            Claims claims = Jwts.parser()
                    .setSigningKey(getSigningKey(secret))
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            return claims.getExpiration().before(new Date());
        } catch (Exception e) {
            return true; // Если не можем распарсить, считаем истекшим
        }
    }

    /**
     * Базовый метод создания токена
     */
    private String createToken(Integer userId, String secret, long expirationMs) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .setSubject(userId.toString())
                .claim("userId", userId)
                .setIssuedAt(now)
                .setExpiration(expiration)
                .signWith(getSigningKey(secret), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Базовый метод парсинга токена
     */
    private Integer parseToken(String token, String secret) {
        try {
            Claims claims = Jwts.parser()
                    .setSigningKey(getSigningKey(secret))
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            Integer userId = claims.get("userId", Integer.class);
            if (userId == null || userId == 0) {
                throw new IllegalArgumentException("UserId is empty in token");
            }

            return userId;
        } catch (SecurityException e) {
            log.error("Invalid JWT signature: {}", e.getMessage());
            throw new IllegalArgumentException("Invalid JWT signature", e);
        } catch (MalformedJwtException e) {
            log.error("Invalid JWT token: {}", e.getMessage());
            throw new IllegalArgumentException("Invalid JWT token", e);
        } catch (ExpiredJwtException e) {
            log.error("JWT token is expired: {}", e.getMessage());
            throw new IllegalArgumentException("JWT token is expired", e);
        } catch (UnsupportedJwtException e) {
            log.error("JWT token is unsupported: {}", e.getMessage());
            throw new IllegalArgumentException("JWT token is unsupported", e);
        } catch (IllegalArgumentException e) {
            log.error("JWT claims string is empty: {}", e.getMessage());
            throw new IllegalArgumentException("JWT claims string is empty", e);
        }
    }

    /**
     * Получение ключа подписи
     */
    private SecretKey getSigningKey(String secret) {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }
}