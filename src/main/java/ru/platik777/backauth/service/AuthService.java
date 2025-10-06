package ru.platik777.backauth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.platik777.backauth.entity.UserData;
import ru.platik777.backauth.dto.response.*;
import ru.platik777.backauth.entity.*;
import ru.platik777.backauth.repository.*;

import java.util.List;
import java.util.Map;
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
    private final UserDataRepository userDataRepository;
    private final UserSchemaRepository userSchemaRepository;
    private final CompanyRepository companyRepository;
    private final ProjectAccessRepository projectAccessRepository;
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
    public SignUpResponse signUp(User user, Company company, String locale) {
        log.debug("SignUp started for login: {}", user.getLogin());

        try {
            // 1. Установка дефолтных значений
            setDefaultUserData(user);

            // 2. ВАЖНО: Сначала хешируем пароль
            String plainPassword = user.getPasswordHash(); // Временно хранится здесь
            String hashedPassword = passwordService.generatePasswordHash(plainPassword);
            user.setPasswordHash(hashedPassword);

            // 3. Теперь валидируем (пароль уже захеширован)
            validationService.validateSignUp(user, company);

            // 4. Получение ролей и модулей
            RoleService.RolesResponse rolesData = roleService.getAvailableRoles();
            List<Map<String, Object>> roles = rolesData.roles();
            List<Object> modules = rolesData.useModules();

            // 5. Создание пользователя и связанных сущностей
            Integer userId = createUser(user, company, roles);

            // 6. Асинхронная установка тарифа
            if (modules != null && !modules.isEmpty()) {
                roleService.setTariffAsync(userId, modules);
            }

            // 7. Получение созданного пользователя
            User createdUser = userDataRepository.getUserWithData(userId)
                    .orElseThrow(() -> new AuthException("Failed to retrieve created user"));

            // 8. Очистка чувствительных данных
            createdUser.setPasswordHash(null);

            // 9. Формирование ответа
            SignUpResponse.SignUpResponseBuilder responseBuilder = SignUpResponse.builder()
                    .user(UserResponse.fromUser(createdUser));

            // Добавление данных студента или компании
            switch (user.getAccountType()) {
                case ENTREPRENEUR:
                case COMPANY_RESIDENT:
                case COMPANY_NON_RESIDENT:
                case EDUCATIONAL_UNIT:
                    if (company != null) {
                        company.setCreatedAt(createdUser.getCreatedAt());
                        responseBuilder.company(CompanyResponse.fromCompany(company));
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

            // 3. Очистка паролей
            user.setPasswordHash(null);

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
    public TokenResponse refreshAppTokenByBaseToken(String refreshToken) {
        log.debug("RefreshAppTokenByBaseToken started");

        try {
            Integer userId = jwtService.parseToken(
                    refreshToken,
                    keyService.getSigningBaseKeyRefresh()
            );

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
    public TokenResponse refreshAppToken(String refreshToken) {
        log.debug("RefreshAppToken started");

        try {
            Integer userId = jwtService.parseToken(
                    refreshToken,
                    keyService.getSigningAppKeyRefresh()
            );

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
    public TokenResponse refreshBaseToken(String refreshToken) {
        log.debug("RefreshBaseToken started");

        try {
            Integer userId = jwtService.parseToken(
                    refreshToken,
                    keyService.getSigningBaseKeyRefresh()
            );

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
            Integer userId = jwtService.parseToken(
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
            Integer userId = jwtService.parseToken(
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
    public UserResponse editUser(Integer userId, String currentPassword, String newPassword,
                                 String email, String phone, String userName, String login) {
        log.debug("EditUser started for userId: {}", userId);

        try {
            // 1. Получение текущих данных пользователя
            User oldUser = userDataRepository.getUserWithData(userId)
                    .orElseThrow(() -> new AuthException("User not found"));

            UserData oldUserData = userDataRepository.getUserData(userId)
                    .orElseThrow(() -> new AuthException("User data not found"));

            // 2. Валидация и хеширование нового пароля (если передан)
            String newPasswordHash = null;
            if (currentPassword != null && newPassword != null) {
                newPasswordHash = validationService.validateAndHashPasswordChange(
                        oldUser.getPasswordHash(),
                        currentPassword,
                        newPassword
                );
            }

            // 3. Подготовка новых данных для валидации
            UserData newUserData = new UserData();
            newUserData.setLogin(login);
            newUserData.setEmail(email);
            newUserData.setPhone(phone);
            newUserData.setUserName(userName);

            // 4. Валидация изменений
            validationService.validateUserUpdate(newUserData, oldUserData);

            // 5. Обновление данных
            updateUserData(userId, login, newPasswordHash, email, phone, userName);

            // 6. Получение обновленных данных
            User updatedUser = userDataRepository.getUserWithData(userId)
                    .orElseThrow(() -> new AuthException("Failed to retrieve updated user"));

            updatedUser.setPasswordHash(null);

            log.info("User updated successfully: userId={}", userId);
            return UserResponse.fromUser(updatedUser);

        } catch (ValidationService.ValidationException e) {
            log.warn("Validation failed during user update: {}", e.getMessage());
            throw new AuthException("Update validation failed: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error updating user: userId={}", userId, e);
            throw new AuthException("User update failed", e);
        }
    }

    /**
     * Получение данных пользователя
     * Go: func (a *AuthService) GetUser(logger go_logger.Logger, userId int)
     */
    @Transactional(readOnly = true)
    public UserResponse getUser(Integer userId) {
        log.debug("GetUser started for userId: {}", userId);

        User user = userDataRepository.getUserWithData(userId)
                .orElseThrow(() -> new AuthException("User not found: " + userId));

        user.setPasswordHash(null);
        return UserResponse.fromUser(user);
    }

    /**
     * Получение базовых данных пользователя
     * Go: func (a *AuthService) GetBaseUser(logger go_logger.Logger, userId int)
     */
    @Transactional(readOnly = true)
    public BaseUserResponse getBaseUser(Integer userId) {
        log.debug("GetBaseUser started for userId: {}", userId);

        User user = userDataRepository.getUserWithData(userId)
                .orElseThrow(() -> new AuthException("User not found: " + userId));

        return BaseUserResponse.fromUser(user);
    }

    /**
     * Получение компаний пользователя
     * Go: func (a *AuthService) GetCompanies(logger go_logger.Logger, userId int)
     */
    @Transactional(readOnly = true)
    public CompaniesResponse getCompanies(Integer userId) {
        log.debug("GetCompanies started for userId: {}", userId);

        List<Company> companies = companyRepository.findByOwnerId(userId);
        List<CompanyResponse> companyResponses = companies.stream()
                .map(CompanyResponse::fromCompany)
                .collect(Collectors.toList());

        return CompaniesResponse.builder()
                .companies(companyResponses)
                .build();
    }

    /**
     * Проверка принадлежности проекта пользователю
     * Go: func (a *AuthService) CheckProjectId(logger go_logger.Logger, userId, projectId int)
     */
    @Transactional(readOnly = true)
    public boolean checkProjectId(Integer userId, Integer projectId) {
        log.debug("CheckProjectId: userId={}, projectId={}", userId, projectId);
        return projectAccessRepository.checkProjectOwnership(userId, projectId);
    }

    /**
     * Проверка роли администратора
     * Go: func (a *AuthService) CheckRoleAdmin(ctx context.Context, logger, userId int)
     */
    @Transactional(readOnly = true)
    public boolean checkRoleAdmin(Integer userId) {
        log.debug("CheckRoleAdmin for userId: {}", userId);
        return roleService.checkRoleAdmin(userId);
    }

    // ==================== PRIVATE METHODS ====================

    /**
     * Установка дефолтных данных пользователя
     * Go: func setDefaultUserData(u *models.User)
     */
    private void setDefaultUserData(User user) {
        // Go: u.BillingId = 0
        if (user.getBillingId() == null) {
            user.setBillingId(0);
        }

        // Go: if u.UserSettings == nil { u.UserSettings = &models.UserSettings{Locale: "ru"} }
        if (user.getSettings() == null) {
            User.UserSettings settings = new User.UserSettings();
            settings.setLocale("ru");
            user.setSettings(settings);
        }
    }

    /**
     * Создание пользователя и его сущностей
     * Go: часть метода SignUp - создание пользователя и связанных entity
     */
    private Integer createUser(User user, Company company,
                               List<Map<String, Object>> roles) {

        // Пароль уже захеширован на этом этапе
        String passwordHash = user.getPasswordHash();

        // 1. Создание записи в public.users
        user.setPasswordHash(null); // Не храним в public.users
        User savedUser = userRepository.save(user);
        Integer userId = savedUser.getId();

        log.debug("User created in public.users: userId={}", userId);

        // 2. Создание схемы userN и всех таблиц
        userSchemaRepository.createUserSchema(
                userId,
                savedUser.getLogin(),
                passwordHash,
                user.getEmail(),
                user.getUserName(),
                user.getPhone()
        );

        log.debug("User schema created: user{}", userId);

        // 3. Создание связанных сущностей
        switch (user.getAccountType()) {
            case ENTREPRENEUR:
            case COMPANY_RESIDENT:
            case COMPANY_NON_RESIDENT:
            case EDUCATIONAL_UNIT:
                if (company != null) {
                    company.setOwner(savedUser);
                    companyRepository.save(company);
                    log.debug("Company entity created for userId={}", userId);
                }
                break;
            default:
                break;
        }

        return userId;
    }

    /**
     * Аутентификация пользователя
     * Go: часть метода generateToken
     */
    private User authenticateUser(String login, String password) {
        // Хеширование введенного пароля
        String passwordHash = passwordService.generatePasswordHash(password);

        // Получение пользователя
        User user = userDataRepository.getUserByLogin(login)
                .orElseThrow(() -> new AuthException("Invalid login or password"));

        // Сверка паролей
        // Go: if user.Password != hashPassword
        if (!user.getPasswordHash().equals(passwordHash)) {
            throw new AuthException("Invalid login or password");
        }

        return user;
    }

    /**
     * Обновление данных пользователя
     */
    private void updateUserData(Integer userId, String login, String passwordHash,
                                String email, String phone, String userName) {

        // 1. Обновление public.users (если нужно)
        if (login != null && !login.isEmpty()) {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new AuthException("User not found"));
            user.setLogin(login);
            userRepository.save(user);
        }

        // 2. Обновление userN.user_data
        UserData userData = new UserData();
        if (passwordHash != null) {
            userData.setPasswordHash(passwordHash);
        }
        if (email != null) {
            userData.setEmail(email);
        }
        if (userName != null) {
            userData.setUserName(userName);
        }
        if (phone != null) {
            userData.setPhone(phone);
        }

        userDataRepository.updateUserData(userId, userData);
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