package ru.platik777.backauth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.platik777.backauth.dto.response.*;
import ru.platik777.backauth.entity.*;
import ru.platik777.backauth.entity.embedded.UserSettings;
import ru.platik777.backauth.repository.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Основной сервис аутентификации и авторизации
 * Соответствует AuthService из Go (auth.go)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final ValidationService validationService;
    private final PasswordService passwordService;
    private final JwtService jwtService;
    private final KeyService keyService;
    private final RoleService roleService;

    /**
     * Регистрация пользователя
     * Go: func (a *AuthService) SignUp(ctx context.Context, l go_logger.Logger,
     *                                  u *models.User, s *models.Student, c *models.Company, locale string)
     */
    @Transactional
    public SignUpResponse signUp(User user, Tenant tenant, String locale) {
        log.debug("SignUp started for login: {}", user.getLogin());

        try {
            // 1. Установка дефолтных значений
            setDefaultUserData(user);

            // 2. ВАЖНО: Сначала хешируем пароль
            String hashedPassword = passwordService.generatePasswordHash(user.getPasswordHash());
            user.setPasswordHash(hashedPassword);

            // 3. Теперь валидируем (пароль уже захеширован)
            validationService.validateSignUp(user, tenant);

            // 4. Получение ролей и модулей
            // RoleService.RolesResponse rolesData = roleService.getAvailableRoles();
            // List<Map<String, Object>> roles = rolesData.roles();
            // List<Object> modules = rolesData.useModules();

            log.debug("User: {}", user);

            // 5. Создание пользователя и связанных сущностей
            User savedUser = userRepository.save(user);
            UUID userId = savedUser.getId();

            // 6. Асинхронная установка тарифа
            // if (modules != null && !modules.isEmpty()) {
            //    roleService.setTariffAsync(userId, modules);
            // }

            // 9. Формирование ответа
            SignUpResponse.SignUpResponseBuilder responseBuilder = SignUpResponse.builder()
                    .user(UserResponse.fromUser(savedUser));

            // Добавление данных студента или компании
            // TODO: Определиться с компаниями и их видами
            switch (user.getAccountType()) {
                case ENTREPRENEUR:
                case COMPANY_RESIDENT:
                case COMPANY_NON_RESIDENT:
                case EDUCATIONAL_UNIT:
                    if (tenant != null) {
                        tenant.setCreatedAt(savedUser.getCreatedAt());
                        responseBuilder.company(CompanyResponse.fromCompany(tenant));
                    }
                    break;
                default:
                    break;
            }

            log.info("User registered successfully: userId={}, login={}", userId, user.getLogin());
            return responseBuilder.build();

        } catch (ValidationService.ValidationException e) {
            log.warn("Validation failed during sign up: {}", e.getMessage());
            throw new AuthException("Registration validation failed: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error during sign up for login: {}", user.getLogin(), e);
            throw new AuthException("Registration failed", e);
        }
    }

    /**
     * Вход пользователя
     * Go: func (a *AuthService) SignIn(logger go_logger.Logger, user *models.User, agentHeader string)
     */
    @Transactional
    public SignInResponse signIn(String login, String password, String agentHeader) {
        log.debug("SignIn started for login: {}", login);

        // Валидация входных данных
        if (login == null || login.trim().isEmpty()) {
            throw new AuthException("Login cannot be empty");
        }
        if (password == null || password.isEmpty()) {
            throw new AuthException("Password cannot be empty");
        }

        try {
            // 1. Проверка логина и пароля, получение пользователя
            User user = authenticateUser(login, password);

            // 2. Создание токенов
            JwtService.TokensResponse tokens = jwtService.createAllTokens(user.getId());

            log.info("User signed in successfully: userId={}, login={}", user.getId(), login);

            // 4. Формирование ответа
            return SignInResponse.builder()
                    .accessToken(tokens.accessAppToken())
                    .refreshToken(tokens.refreshAppToken())
                    .accessBaseToken(tokens.accessBaseToken())
                    .refreshBaseToken(tokens.refreshBaseToken())
                    .user(UserResponse.fromUser(user))
                    .build();

        } catch (AuthException e) {
            log.warn("Authentication failed for login: {}", login);
            throw e;
        } catch (Exception e) {
            log.error("Error during sign in for login: {}", login, e);
            throw new AuthException("Sign in failed", e);
        }
    }

    /**
     * Проверка уникальности поля
     * Go: func (a *AuthService) CheckFieldForUniqueness(logger go_logger.Logger, field, value string)
     */
    @Transactional(readOnly = true)
    public UniquenessResponse checkFieldForUniqueness(String field, String value) {
        log.debug("CheckFieldForUniqueness: field={}, value={}", field, value);

        try {
            // Использ ValidationService для проверки
            boolean unique = validationService.checkFieldUniqueness(field, value);

            return UniquenessResponse.builder()
                    .unique(unique)
                    .build();

        } catch (ValidationService.ValidationException e) {
            log.warn("Field uniqueness check failed: field={}, error={}", field, e.getMessage());
            throw new AuthException(e.getMessage(), e);
        }
    }

    /**
     * Обновление app токенов через base refresh token
     * Go: func (a *AuthService) RefreshAppTokenByBaseToken(logger go_logger.Logger, rToken string)
     */
    public TokenResponse refreshAppTokenByBaseToken(UUID userId) {
        log.debug("RefreshAppTokenByBaseToken started");

        try {
            JwtService.TokensResponse tokens = jwtService.createAllTokens(userId);

            return TokenResponse.builder()
                    .accessToken(tokens.accessAppToken())
                    .refreshToken(tokens.refreshAppToken())
                    .accessBaseToken(tokens.accessBaseToken())
                    .refreshBaseToken(tokens.refreshBaseToken())
                    .build();

        } catch (JwtService.JwtException e) {
            log.warn("Invalid refresh token: {}", e.getMessage());
            throw new AuthException("Invalid refresh token", e);
        }
    }

    /**
     * Обновление app токенов
     * Go: func (a *AuthService) RefreshAppToken(logger go_logger.Logger, rToken string)
     */
    public TokenResponse refreshAppToken(UUID userId) {
        log.debug("RefreshAppToken started");

        try {
            Map<String, String> appTokens = jwtService.createAppTokens(userId);
            Map<String, String> baseTokens = jwtService.createBaseTokens(userId);

            return TokenResponse.builder()
                    .accessToken(appTokens.get("accessToken"))
                    .refreshToken(appTokens.get("refreshToken"))
                    .accessBaseToken(baseTokens.get("accessToken"))
                    .refreshBaseToken(baseTokens.get("refreshToken"))
                    .build();

        } catch (JwtService.JwtException e) {
            log.warn("Invalid refresh token: {}", e.getMessage());
            throw new AuthException("Invalid refresh token", e);
        }
    }

    /**
     * Обновление base токенов
     * Go: func (a *AuthService) RefreshBaseToken(logger go_logger.Logger, rToken string)
     */
    public TokenResponse refreshBaseToken(UUID userId) {
        log.debug("RefreshBaseToken started");

        try {
            Map<String, String> tokens = jwtService.createBaseTokens(userId);

            return TokenResponse.builder()
                    .accessBaseToken(tokens.get("accessToken"))
                    .refreshBaseToken(tokens.get("refreshToken"))
                    .build();

        } catch (JwtService.JwtException e) {
            log.warn("Invalid refresh token: {}", e.getMessage());
            throw new AuthException("Invalid refresh token", e);
        }
    }

    /**
     * Проверка app авторизации
     * Go: func (a *AuthService) IsAppAuthorization(logger go_logger.Logger, accessToken string)
     */
    @Transactional(readOnly = true)
    public AuthorizationResponse isAppAuthorization(String accessToken) {
        if (accessToken == null || accessToken.trim().isEmpty()) {
            throw new AuthException("Empty accessToken");
        }

        try {
            UUID userId = jwtService.parseToken(
                    accessToken,
                    keyService.getSigningAppKeyAccess()
            );

            return AuthorizationResponse.builder()
                    .status("OK")
                    .userId(userId)
                    .build();

        } catch (JwtService.JwtException e) {
            log.warn("Invalid access token: {}", e.getMessage());
            throw new AuthException("Invalid access token", e);
        }
    }

    /**
     * Проверка base авторизации
     * Go: func (a *AuthService) IsBaseAuthorization(logger go_logger.Logger, accessToken string)
     */
    @Transactional(readOnly = true)
    public AuthorizationResponse isBaseAuthorization(String accessToken) {
        if (accessToken == null || accessToken.trim().isEmpty()) {
            throw new AuthException("Empty accessToken");
        }

        try {
            UUID userId = jwtService.parseToken(
                    accessToken,
                    keyService.getSigningBaseKeyAccess()
            );

            return AuthorizationResponse.builder()
                    .status("OK")
                    .userId(userId)
                    .build();

        } catch (JwtService.JwtException e) {
            log.warn("Invalid access token: {}", e.getMessage());
            throw new AuthException("Invalid access token", e);
        }
    }

    /**
     * Редактирование пользователя
     * Go: func (a *AuthService) EditUser(logger go_logger.Logger, user *models.User)
     */
    @Transactional
    public UserResponse editUser(UUID userId, String currentPassword, String newPassword,
                                 String email, String phone, String userName, String login) {
        // TODO: реализовать
        return null;
    }

    /**
     * Получение данных пользователя
     * Go: func (a *AuthService) GetUser(logger go_logger.Logger, userId int)
     */
    @Transactional(readOnly = true)
    public UserResponse getUser(UUID userId) {
        log.debug("GetUser started for userId: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException("User not found: " + userId));

        user.setPasswordHash(null);
        return UserResponse.fromUser(user);
    }

    /**
     * Получение базовых данных пользователя
     * Go: func (a *AuthService) GetBaseUser(logger go_logger.Logger, userId int)
     */
    @Transactional(readOnly = true)
    public BaseUserResponse getBaseUser(UUID userId) {
        log.debug("GetBaseUser started for userId: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException("User not found: " + userId));

        return BaseUserResponse.fromUser(user);
    }

    /**
     * Получение компаний пользователя
     * Go: func (a *AuthService) GetCompanies(logger go_logger.Logger, userId int)
     */
    @Transactional(readOnly = true)
    public CompaniesResponse getCompanies(UUID userId) {
        log.debug("GetCompanies started for userId: {}", userId);

        List<Tenant> companies = companyRepository.findByOwnerId(userId);
        List<CompanyResponse> companyResponses = companies.stream()
                .map(CompanyResponse::fromCompany)
                .collect(Collectors.toList());

        return CompaniesResponse.builder()
                .companies(companyResponses)
                .build();
    }

    /**
     * Проверка роли администратора
     * Go: func (a *AuthService) CheckRoleAdmin(ctx context.Context, logger, userId int)
     */
    @Transactional(readOnly = true)
    public boolean checkRoleAdmin(UUID userId) {
        log.debug("CheckRoleAdmin for userId: {}", userId);
        return roleService.checkRoleAdmin(userId);
    }

    // ==================== PRIVATE METHODS ====================

    /**
     * Установка дефолтных данных пользователя
     * Go: func setDefaultUserData(u *models.User)
     */
    private void setDefaultUserData(User user) {
        // Go: if u.UserSettings == nil { u.UserSettings = &models.UserSettings{Locale: "ru"} }
        if (user.getSettings() == null) {
            UserSettings settings = new UserSettings();
            settings.setLocale("ru");
            user.setSettings(settings);
        }
    }


    /**
     * Аутентификация пользователя
     * Go: часть метода generateToken
     */
    private User authenticateUser(String login, String password) {
        // Хеширование введенного пароля
        String passwordHash = passwordService.generatePasswordHash(password);

        // Получение пользователя
        User user = userRepository.findByLogin(login)
                .orElseThrow(() -> new AuthException("Invalid login or password"));

        // Сверка паролей
        // Go: if user.Password != hashPassword
        if (!user.getPasswordHash().equals(passwordHash)) {
            throw new AuthException("Invalid login or password");
        }

        return user;
    }

    // ==================== CUSTOM EXCEPTIONS ====================

    /**
     * Исключение аутентификации/авторизации
     */
    public static class AuthException extends RuntimeException {
        public AuthException(String message) {
            super(message);
        }

        public AuthException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}