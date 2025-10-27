package ru.platik777.backauth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.platik777.backauth.dto.AuthenticatedUser;
import ru.platik777.backauth.dto.response.*;
import ru.platik777.backauth.entity.*;
import ru.platik777.backauth.entity.embedded.UserSettings;
import ru.platik777.backauth.exception.AuthException;
import ru.platik777.backauth.exception.CustomJwtException;
import ru.platik777.backauth.exception.ValidationException;
import ru.platik777.backauth.repository.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Основной сервис аутентификации и авторизации
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
     */
    @Transactional
    public SignUpResponse signUp(User user, Tenant tenant, String locale) {
        log.debug("SignUp started for login: {}", user.getLogin());

        try {
            setDefaultUserData(user);
            String hashedPassword = passwordService.generatePasswordHash(user.getPasswordHash());
            user.setPasswordHash(hashedPassword);
            validationService.validateSignUp(user, tenant);

            // 4. Получение ролей и модулей
            // RoleService.RolesResponse rolesData = roleService.getAvailableRoles();
            // List<Map<String, Object>> roles = rolesData.roles();
            // List<Object> modules = rolesData.useModules();

            log.debug("User: {}", user);

            User savedUser = userRepository.save(user);
            String userId = savedUser.getId();

            // 6. Асинхронная установка тарифа
            // if (modules != null && !modules.isEmpty()) {
            //    roleService.setTariffAsync(userId, modules);
            // }

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

        } catch (ValidationException e) {
            log.warn("Validation failed during sign up: {}", e.getMessage());
            throw new AuthException("Registration validation failed: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error during sign up for login: {}", user.getLogin(), e);
            throw new AuthException("Registration failed", e);
        }
    }

    /**
     * Вход пользователя
     */
    @Transactional
    public SignInResponse signIn(String login, String password, String agentHeader) {
        log.debug("SignIn started for login: {}", login);

        if (login == null || login.trim().isEmpty()) {
            throw new AuthException("Login cannot be empty");
        }
        if (password == null || password.isEmpty()) {
            throw new AuthException("Password cannot be empty");
        }

        try {
            String passwordHash = passwordService.generatePasswordHash(password);
            User user = userRepository.findByLogin(login)
                    .orElseThrow(() -> new AuthException("Invalid login or password"));

            if (!user.getPasswordHash().equals(passwordHash)) {
                throw new AuthException("Invalid login or password");
            }
            TokenResponse tokens = jwtService.createAllTokens(user.getId(), user.getTenantId());

            log.info("User signed in successfully: userId={}, login={}", user.getId(), login);

            return SignInResponse.builder()
                    .accessToken(tokens.getAccessToken())
                    .refreshToken(tokens.getRefreshToken())
                    .accessBaseToken(tokens.getAccessBaseToken())
                    .refreshBaseToken(tokens.getRefreshBaseToken())
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
     */
    @Transactional(readOnly = true)
    public UniquenessResponse checkFieldForUniqueness(String field, String value) {
        log.debug("CheckFieldForUniqueness: field={}, value={}", field, value);

        try {
            boolean unique = validationService.checkFieldUniqueness(field, value);

            return UniquenessResponse.builder()
                    .unique(unique)
                    .build();

        } catch (ValidationException e) {
            log.warn("Field uniqueness check failed: field={}, error={}", field, e.getMessage());
            throw new AuthException(e.getMessage(), e);
        }
    }

    /**
     * Обновление app токенов через base refresh token
     */
    public TokenResponse refreshAppTokenByBaseToken(String userId, String tenantId) {
        log.debug("RefreshAppTokenByBaseToken started");

        try {
            TokenResponse tokens = jwtService.createAllTokens(userId, tenantId);

            return TokenResponse.builder()
                    .accessToken(tokens.getAccessToken())
                    .refreshToken(tokens.getRefreshToken())
                    .accessBaseToken(tokens.getAccessBaseToken())
                    .refreshBaseToken(tokens.getRefreshBaseToken())
                    .build();

        } catch (CustomJwtException e) {
            log.warn("Invalid refresh token: {}", e.getMessage());
            throw new AuthException("Invalid refresh token", e);
        }
    }

    /**
     * Обновление app токенов
     */
    public TokenResponse refreshAppToken(String userId, String tenantId) {
        log.debug("RefreshAppToken started");

        try {
            Map<String, String> appTokens = jwtService.createAppTokens(userId, tenantId);
            Map<String, String> baseTokens = jwtService.createBaseTokens(userId, tenantId);

            return TokenResponse.builder()
                    .accessToken(appTokens.get("accessToken"))
                    .refreshToken(appTokens.get("refreshToken"))
                    .accessBaseToken(baseTokens.get("accessToken"))
                    .refreshBaseToken(baseTokens.get("refreshToken"))
                    .build();

        } catch (CustomJwtException e) {
            log.warn("Invalid refresh token: {}", e.getMessage());
            throw new AuthException("Invalid refresh token", e);
        }
    }

    /**
     * Обновление base токенов
     */
    public TokenResponse refreshBaseToken(String userId, String tenantId) {
        log.debug("RefreshBaseToken started");

        try {
            Map<String, String> tokens = jwtService.createBaseTokens(userId, tenantId);

            return TokenResponse.builder()
                    .accessBaseToken(tokens.get("accessToken"))
                    .refreshBaseToken(tokens.get("refreshToken"))
                    .build();

        } catch (CustomJwtException e) {
            log.warn("Invalid refresh token: {}", e.getMessage());
            throw new AuthException("Invalid refresh token", e);
        }
    }

    /**
     * Проверка app авторизации
     */
    @Transactional(readOnly = true)
    public AuthorizationResponse isAppAuthorization(String accessToken) {
        if (accessToken == null || accessToken.trim().isEmpty()) {
            throw new AuthException("Empty accessToken");
        }

        try {
            AuthenticatedUser user = jwtService.parseToken(
                    accessToken,
                    keyService.getSigningAppKeyAccess()
            );

            return AuthorizationResponse.builder()
                    .status("OK")
                    .userId(user.getUserId())
                    .build();

        } catch (CustomJwtException e) {
            log.warn("Invalid access token: {}", e.getMessage());
            throw new AuthException("Invalid access token", e);
        }
    }

    /**
     * Проверка base авторизации
     */
    @Transactional(readOnly = true)
    public AuthorizationResponse isBaseAuthorization(String accessToken) {
        if (accessToken == null || accessToken.trim().isEmpty()) {
            throw new AuthException("Empty accessToken");
        }

        try {
            AuthenticatedUser user = jwtService.parseToken(
                    accessToken,
                    keyService.getSigningBaseKeyAccess()
            );

            return AuthorizationResponse.builder()
                    .status("OK")
                    .userId(user.getUserId())
                    .build();

        } catch (CustomJwtException e) {
            log.warn("Invalid access token: {}", e.getMessage());
            throw new AuthException("Invalid access token", e);
        }
    }

    /**
     * Редактирование пользователя
     */
    @Transactional
    public UserResponse editUser(String userId, String currentPassword, String newPassword,
                                 String email, String phone, String userName, String login) {
        // TODO: реализовать
        return null;
    }

    /**
     * Получение данных пользователя
     */
    @Transactional(readOnly = true)
    public UserResponse getUser(String userId) {
        log.debug("GetUser started for userId: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException("User not found: " + userId));

        user.setPasswordHash(null);
        return UserResponse.fromUser(user);
    }

    /**
     * Получение базовых данных пользователя
     */
    @Transactional(readOnly = true)
    public BaseUserResponse getBaseUser(String userId) {
        log.debug("GetBaseUser started for userId: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException("User not found: " + userId));

        return BaseUserResponse.fromUser(user);
    }

    /**
     * Получение компаний пользователя
     */
    @Transactional(readOnly = true)
    public CompaniesResponse getCompanies(String userId) {
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
     */
    @Transactional(readOnly = true)
    public boolean checkRoleAdmin(String userId) {
        log.debug("CheckRoleAdmin for userId: {}", userId);
        return roleService.checkRoleAdmin(userId);
    }

    /**
     * Установка дефолтных данных пользователя
     */
    private void setDefaultUserData(User user) {
        if (user.getSettings() == null) {
            UserSettings settings = new UserSettings();
            settings.setLocale("ru");
            user.setSettings(settings);
        }
    }
}