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
 * Конфигурация Spring Security с JWT аутентификацией
 *
 * Архитектура:
 * 1. Публичные endpoints (signIn, signUp, reset password) - без токена
 * 2. App-защищенные endpoints (/api/v1/user, /api/v1/key) - App Access Token
 * 3. Base-защищенные endpoints (/api/v1/base) - Base Access Token
 * 4. Internal endpoints (/api/v1/internal) - без токена (доступны внутри сети)
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

                // Добавляем JWT фильтр ПЕРЕД стандартной аутентификацией
                .addFilterBefore(jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class)

                .authorizeHttpRequests(authz -> authz
                        // ===== ПУБЛИЧНЫЕ ENDPOINTS (без токена) =====

                        // Регистрация и вход
                        .requestMatchers("/auth/signUp").permitAll()
                        .requestMatchers("/auth/signIn").permitAll()

                        // Восстановление пароля
                        .requestMatchers("/api/v1/resetPassword/**").permitAll()

                        // Проверка уникальности полей (для формы регистрации)
                        .requestMatchers("/api/v1/auth/checkFieldForUniqueness").permitAll()

                        // Образовательные учреждения (публичный справочник)
                        .requestMatchers("/api/v1/educationalInstitutions/**").permitAll()

                        // Internal endpoints (доступны без токена, но защищены на уровне сети)
                        .requestMatchers("/api/v1/internal/**").permitAll()

                        // Мониторинг и health checks
                        .requestMatchers("/monitor/**").permitAll()
                        .requestMatchers("/actuator/**").permitAll()
                        .requestMatchers("/metrics").permitAll()

                        // ===== ЗАЩИЩЕННЫЕ APP ENDPOINTS (App Access Token) =====

                        // Обновление токенов (требует Refresh Token, но проверяется в контроллере)
                        .requestMatchers("/auth/refreshToken").permitAll()
                        .requestMatchers("/auth/refreshTokenByBaseToken").permitAll()
                        .requestMatchers("/auth/refreshBaseToken").permitAll()

                        // Проверка авторизации (внутренний endpoint для других сервисов)
                        .requestMatchers("/auth/isAuthorization").permitAll()
                        .requestMatchers("/auth/isBaseAuthorization").permitAll()

                        // Пользовательские операции
                        .requestMatchers("/auth/editUser").authenticated()
                        .requestMatchers("/auth/checkProjectId").authenticated()
                        .requestMatchers("/auth/checkRoleAdmin").authenticated()

                        // API v1 - пользователь
                        .requestMatchers("/api/v1/user/**").authenticated()

                        // API v1 - API ключи
                        .requestMatchers("/api/v1/key/**").authenticated()

                        // ===== ЗАЩИЩЕННЫЕ BASE ENDPOINTS (Base Access Token) =====

                        // Базовая информация о пользователе
                        .requestMatchers("/api/v1/base/**").authenticated()

                        // ===== ВСЕ ОСТАЛЬНОЕ =====
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