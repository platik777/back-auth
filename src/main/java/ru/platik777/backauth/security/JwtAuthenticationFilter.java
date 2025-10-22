package ru.platik777.backauth.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import ru.platik777.backauth.dto.AuthenticatedUser;
import ru.platik777.backauth.entity.types.TokenType;
import ru.platik777.backauth.service.JwtService;
import ru.platik777.backauth.service.KeyService;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * JWT фильтр с правильной обработкой ошибок
 *
 * ИСПРАВЛЕНИЯ:
 * 1. Явная отправка 401 при невалидном токене
 * 2. Детальные сообщения об ошибках
 * 3. Пропуск ТОЛЬКО для публичных endpoint'ов
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final KeyService keyService;
    private final ObjectMapper objectMapper;

    // Публичные endpoint'ы, не требующие токена
    private static final String[] PUBLIC_ENDPOINTS = {
            "/auth/signIn",
            "/auth/signUp",
            "/api/v1/resetPassword",
            "/api/v1/auth/checkFieldForUniqueness",
            "/api/v1/educationalInstitutions",
            "/api/v1/internal",
            "/monitor",
            "/actuator",
            "/metrics",
            "/api/v1/key/check",  // Проверка API ключа

            // Swagger UI и документация
            "/swagger-ui",
            "/v3/api-docs",
            "/swagger-resources",
            "/webjars"
    };

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String requestPath = request.getRequestURI();

        // 1. Пропускаем публичные endpoint'ы БЕЗ проверки токена
        if (isPublicEndpoint(requestPath)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 2. Извлекаем токен из header
        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            // Нет токена для защищенного endpoint'а - 401
            sendUnauthorizedError(response, "Missing or invalid Authorization header");
            return;
        }

        String token = authHeader.substring(7).trim();

        if (token.isEmpty()) {
            sendUnauthorizedError(response, "Token is empty");
            return;
        }

        try {
            // 3. Определяем тип токена по пути
            TokenType tokenType = determineTokenType(requestPath);

            // 4. Валидируем токен соответствующим ключом
            AuthenticatedUser authenticatedUser = validateToken(token, tokenType);

            if (authenticatedUser == null || authenticatedUser.getUserId() == null) {
                sendUnauthorizedError(response, "Invalid or expired token");
                return;
            }

            // 5. Устанавливаем Authentication в контекст
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            authenticatedUser,
                            null,
                            Collections.emptyList()
                    );

            authentication.setDetails(
                    new WebAuthenticationDetailsSource().buildDetails(request)
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            request.setAttribute("userId", authenticatedUser.getUserId());
            request.setAttribute("tenantId", authenticatedUser.getTenantId());
            request.setAttribute("jwtToken", token);

            log.debug("JWT authenticated: userId={}, tenantId={}, path={}, tokenType={}",
                    authenticatedUser.getUserId(), authenticatedUser.getTenantId(), requestPath, tokenType);

            // 6. Продолжаем цепочку
            filterChain.doFilter(request, response);

        } catch (JwtException e) {
            // Специфичные ошибки JWT
            log.warn("JWT validation failed: {}", e.getMessage());
            sendUnauthorizedError(response, e.getMessage());
        } catch (Exception e) {
            // Неожиданные ошибки
            log.error("Unexpected error in JWT filter", e);
            sendUnauthorizedError(response, "Token validation failed");
        }
    }

    /**
     * Проверка, является ли endpoint публичным
     */
    private boolean isPublicEndpoint(String path) {
        for (String publicPath : PUBLIC_ENDPOINTS) {
            if (path.startsWith(publicPath)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Определение типа токена по пути запроса
     */
    private TokenType determineTokenType(String path) {
        // Base токены
        if (path.startsWith("/api/v1/base/")) {
            return TokenType.BASE_ACCESS;
        }
        return switch (path) {
            case "/auth/isBaseAuthorization" -> TokenType.BASE_ACCESS;
            case "/auth/refreshToken" -> TokenType.APP_REFRESH;
            case "/auth/refreshTokenByBaseToken", "/auth/refreshBaseToken" -> TokenType.BASE_REFRESH;
            default ->
                // По умолчанию - App Access
                    TokenType.APP_ACCESS;
        };

    }

    /**
     * Валидация токена соответствующим ключом
     */
    private AuthenticatedUser validateToken(String token, TokenType tokenType) {
        try {
            String signingKey = switch (tokenType) {
                case APP_ACCESS -> keyService.getSigningAppKeyAccess();
                case APP_REFRESH -> keyService.getSigningAppKeyRefresh();
                case BASE_ACCESS -> keyService.getSigningBaseKeyAccess();
                case BASE_REFRESH -> keyService.getSigningBaseKeyRefresh();
                case RESET_PASSWORD, API_KEY -> null;
            };

            if (signingKey == null) {
                throw new JwtException("Null signingKey");
            }


            return jwtService.parseToken(token, signingKey);

        } catch (JwtException e) {
            log.debug("Token validation failed: tokenType={}, error={}", tokenType, e.getMessage());
            throw e;
        }
    }

    /**
     * Отправка 401 Unauthorized с JSON ошибкой
     */
    private void sendUnauthorizedError(HttpServletResponse response, String message)
            throws IOException {

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("timestamp", LocalDateTime.now().toString());
        errorDetails.put("status", HttpStatus.UNAUTHORIZED.value());
        errorDetails.put("error", "Unauthorized");
        errorDetails.put("message", message);

        objectMapper.writeValue(response.getWriter(), errorDetails);
    }
}