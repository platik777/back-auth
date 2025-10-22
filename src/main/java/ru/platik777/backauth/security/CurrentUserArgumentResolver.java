package ru.platik777.backauth.security;

import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import ru.platik777.backauth.dto.AuthenticatedUser;
import ru.platik777.backauth.exception.UnauthorizedException;

import java.util.UUID;

/**
 * Резолвер для автоматического извлечения userId из SecurityContext
 * <p>
 * Работает в связке с JwtAuthenticationFilter:
 * 1. Фильтр парсит JWT и устанавливает userId в Authentication.principal
 * 2. Резолвер извлекает userId из Authentication и передает в контроллер
 */
@Slf4j
@Component
public class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUser.class) &&
                (UUID.class.isAssignableFrom(parameter.getParameterType()) ||
                        AuthenticatedUser.class.isAssignableFrom(parameter.getParameterType()));
    }

    @Override
    public Object resolveArgument(@Nullable MethodParameter parameter,
                                  ModelAndViewContainer mavContainer,
                                  @Nullable NativeWebRequest webRequest,
                                  WebDataBinderFactory binderFactory) throws Exception {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            log.warn("No authenticated user found for @CurrentUser parameter");
            throw new UnauthorizedException("User not authenticated");
        }

        Object principal = authentication.getPrincipal();

        if (!(principal instanceof AuthenticatedUser authenticatedUser)) {
            log.error("Invalid principal type: expected AuthenticatedUser, got {}",
                    principal.getClass().getSimpleName());
            throw new UnauthorizedException("Invalid authentication principal");
        }

        log.debug("Resolved @CurrentUser: userId={}, tenantId={}",
                authenticatedUser.getUserId(), authenticatedUser.getTenantId());
        return authenticatedUser;
    }
}