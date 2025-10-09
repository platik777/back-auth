package ru.platik777.backauth.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import ru.platik777.backauth.security.JwtAuthenticationFilter;

import java.util.List;

/**
 * Конфигурация Spring Security с правильной настройкой endpoint'ов
 *
 * ИСПРАВЛЕНИЯ:
 * 1. Четкое разделение публичных и защищенных endpoint'ов
 * 2. Согласованность с JwtAuthenticationFilter
 * 3. Правильная настройка refresh endpoint'ов
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // JWT фильтр перед стандартной аутентификацией
                .addFilterBefore(jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class)

                .authorizeHttpRequests(authz -> authz
                        // ========== ПУБЛИЧНЫЕ ENDPOINTS (БЕЗ токена) ==========

                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/api-docs/**",
                                "/swagger-resources/**",
                                "/webjars/**"
                        ).permitAll()

                        // Регистрация и вход
                        .requestMatchers(
                                "/auth/signUp",
                                "/auth/signIn"
                        ).permitAll()

                        // Восстановление пароля (все endpoint'ы)
                        .requestMatchers("/api/v1/resetPassword/**").permitAll()

                        // Проверка уникальности полей (для формы регистрации)
                        .requestMatchers("/api/v1/auth/checkFieldForUniqueness").permitAll()

                        // Образовательные учреждения (публичный справочник)
                        .requestMatchers("/api/v1/educationalInstitutions/**").permitAll()

                        // Internal endpoints (защищены на уровне сети)
                        .requestMatchers("/api/v1/internal/**").permitAll()

                        // Мониторинг
                        .requestMatchers(
                                "/monitor/**",
                                "/actuator/**",
                                "/metrics"
                        ).permitAll()

                        // Проверка API ключа (публичный endpoint)
                        .requestMatchers("/api/v1/key/check").permitAll()

                        // ========== ЗАЩИЩЕННЫЕ ENDPOINTS (С токеном) ==========

                        // Обновление токенов - ТРЕБУЮТ соответствующий Refresh Token
                        // Проверка типа токена происходит в JwtAuthenticationFilter
                        .requestMatchers(
                                "/auth/refreshToken",           // App Refresh Token
                                "/auth/refreshTokenByBaseToken", // Base Refresh Token
                                "/auth/refreshBaseToken"         // Base Refresh Token
                        ).authenticated()

                        // Проверка авторизации (для других микросервисов)
                        // Требуют соответствующий Access Token
                        .requestMatchers(
                                "/auth/isAuthorization",     // App Access Token
                                "/auth/isBaseAuthorization"  // Base Access Token
                        ).authenticated()

                        // Пользовательские операции - App Access Token
                        .requestMatchers(
                                "/auth/editUser",
                                "/auth/checkProjectId",
                                "/auth/checkRoleAdmin"
                        ).authenticated()

                        // API v1/user - App Access Token
                        .requestMatchers("/api/v1/user/**").authenticated()

                        // API v1/key - App Access Token
                        .requestMatchers("/api/v1/key/**").authenticated()

                        // API v1/base - Base Access Token
                        .requestMatchers("/api/v1/base/**").authenticated()

                        // Все остальное требует аутентификации
                        .anyRequest().authenticated()
                );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("*"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}