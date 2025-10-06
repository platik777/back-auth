package ru.platik777.backauth.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import ru.platik777.backauth.service.JwtService;
import ru.platik777.backauth.service.KeyService;

import java.io.IOException;
import java.util.Collections;

/**
 * JWT фильтр для проверки токенов
 *
 * Логика работы:
 * 1. Извлекает токен из Authorization header
 * 2. Определяет тип токена по endpoint
 * 3. Валидирует токен соответствующим ключом
 * 4. Устанавливает Authentication в SecurityContext
 * 5. Добавляет userId в request attributes для контроллеров
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final KeyService keyService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        try {
            // 1. Извлекаем токен из header
            String authHeader = request.getHeader("Authorization");

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                // Нет токена - пропускаем дальше (публичные endpoints)
                filterChain.doFilter(request, response);
                return;
            }

            String token = authHeader.substring(7).trim();

            if (token.isEmpty()) {
                filterChain.doFilter(request, response);
                return;
            }

            // 2. Определяем тип токена по пути запроса
            String requestPath = request.getRequestURI();
            TokenType tokenType = determineTokenType(requestPath);

            // 3. Валидируем токен соответствующим ключом
            Integer userId = validateToken(token, tokenType);

            if (userId != null) {
                // 4. Создаем Authentication и устанавливаем в контекст
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userId,
                                null,
                                Collections.emptyList()
                        );

                authentication.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );

                SecurityContextHolder.getContext().setAuthentication(authentication);

                // 5. Добавляем userId в request attributes для удобного доступа в контроллерах
                request.setAttribute("userId", userId);

                log.debug("JWT authenticated: userId={}, path={}, tokenType={}",
                        userId, requestPath, tokenType);
            }

        } catch (Exception e) {
            // Логируем ошибку, но не прерываем цепочку
            // Spring Security обработает отсутствие Authentication
            log.warn("JWT validation failed: {}", e.getMessage());
        }

        // 6. Продолжаем цепочку фильтров
        filterChain.doFilter(request, response);
    }

    /**
     * Определение типа токена по пути запроса
     *
     * Base токены используются для:
     * - /api/v1/base/** - базовая информация
     * - /auth/refreshTokenByBaseToken - обновление через base token
     *
     * App токены используются для всего остального
     */
    private TokenType determineTokenType(String path) {
        if (path.startsWith("/api/v1/base/") ||
                path.equals("/auth/refreshTokenByBaseToken") ||
                path.equals("/auth/isBaseAuthorization")) {
            return TokenType.BASE_ACCESS;
        }

        // Refresh токены определяются по endpoint'ам обновления
        if (path.equals("/auth/refreshToken")) {
            return TokenType.APP_REFRESH;
        }

        if (path.equals("/auth/refreshBaseToken")) {
            return TokenType.BASE_REFRESH;
        }

        // По умолчанию - App Access токен
        return TokenType.APP_ACCESS;
    }

    /**
     * Валидация токена соответствующим ключом
     */
    private Integer validateToken(String token, TokenType tokenType) {
        try {
            String signingKey = switch (tokenType) {
                case APP_ACCESS -> keyService.getSigningAppKeyAccess();
                case APP_REFRESH -> keyService.getSigningAppKeyRefresh();
                case BASE_ACCESS -> keyService.getSigningBaseKeyAccess();
                case BASE_REFRESH -> keyService.getSigningBaseKeyRefresh();
            };

            return jwtService.parseToken(token, signingKey);

        } catch (JwtService.JwtException e) {
            log.debug("Token validation failed: tokenType={}, error={}",
                    tokenType, e.getMessage());
            return null;
        }
    }

    /**
     * Типы токенов в системе
     */
    private enum TokenType {
        APP_ACCESS,     // Для основных операций с пользователем
        APP_REFRESH,    // Для обновления App токенов
        BASE_ACCESS,    // Для базовой информации
        BASE_REFRESH    // Для обновления Base токенов
    }
}