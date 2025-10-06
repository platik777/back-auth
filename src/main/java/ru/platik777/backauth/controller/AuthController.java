package ru.platik777.backauth.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.platik777.backauth.dto.request.*;
import ru.platik777.backauth.dto.response.*;
import ru.platik777.backauth.entity.Company;
import ru.platik777.backauth.entity.User;
import ru.platik777.backauth.mapper.CompanyMapper;
import ru.platik777.backauth.mapper.UserMapper;
import ru.platik777.backauth.security.CurrentUser;
import ru.platik777.backauth.service.AuthService;
import ru.platik777.backauth.util.TokenExtractor;

/**
 * Основной контроллер аутентификации и авторизации
 *
 * ВАЖНО: Использует @CurrentUser вместо @RequestHeader("userId")
 * для безопасного извлечения userId из JWT токена
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserMapper userMapper;
    private final CompanyMapper companyMapper;
    private final TokenExtractor tokenExtractor;

    // ========== ПУБЛИЧНЫЕ ENDPOINTS (без токена) ==========

    /**
     * POST /auth/signUp
     * Регистрация нового пользователя
     */
    @PostMapping("/auth/signUp")
    public ResponseEntity<SignUpResponse> signUp(@RequestBody SignUpRequest request) {
        log.info("Sign-up request for login: {}",
                request.getUser() != null ? request.getUser().getLogin() : "null");

        User user = userMapper.toEntity(request.getUser());
        Company company = companyMapper.toEntity(request.getCompany());

        SignUpResponse response = authService.signUp(user, company, request.getLocale());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * POST /auth/signIn
     * Вход пользователя
     */
    @PostMapping("/auth/signIn")
    public ResponseEntity<SignInResponse> signIn(
            @RequestBody SignInRequest request,
            @RequestHeader(value = "User-Agent", required = false) String userAgent) {

        log.info("Sign-in request for login: {}", request.getLogin());

        SignInResponse response = authService.signIn(
                request.getLogin(),
                request.getPassword(),
                userAgent
        );

        return ResponseEntity.ok(response);
    }

    // ========== ОБНОВЛЕНИЕ ТОКЕНОВ ==========

    /**
     * POST /auth/refreshToken
     * Обновление app токенов по app refresh token
     */
    @PostMapping("/auth/refreshToken")
    public ResponseEntity<TokenResponse> refreshAppToken(
            @RequestBody TokenRefreshRequest request) {

        log.debug("Refreshing app token");

        TokenResponse response = authService.refreshAppToken(request.getRefreshToken());

        return ResponseEntity.ok(response);
    }

    /**
     * POST /auth/refreshTokenByBaseToken
     * Обновление app токенов по base refresh token
     */
    @PostMapping("/auth/refreshTokenByBaseToken")
    public ResponseEntity<TokenResponse> refreshAppTokenByBaseToken(
            @RequestBody TokenRefreshRequest request) {

        log.debug("Refreshing app token by base token");

        TokenResponse response = authService.refreshAppTokenByBaseToken(request.getRefreshToken());

        return ResponseEntity.ok(response);
    }

    /**
     * POST /auth/refreshBaseToken
     * Обновление base токенов
     */
    @PostMapping("/auth/refreshBaseToken")
    public ResponseEntity<TokenResponse> refreshBaseToken(
            @RequestBody TokenRefreshRequest request) {

        log.debug("Refreshing base token");

        TokenResponse response = authService.refreshBaseToken(request.getRefreshToken());

        return ResponseEntity.ok(response);
    }

    // ========== ПРОВЕРКА АВТОРИЗАЦИИ (для микросервисов) ==========

    /**
     * POST /auth/isAuthorization
     * Проверка app авторизации (для других микросервисов)
     */
    @PostMapping("/auth/isAuthorization")
    public ResponseEntity<AuthorizationResponse> isAppAuthorization(
            @RequestHeader("Authorization") String authHeader) {

        log.debug("Checking app authorization");

        String accessToken = tokenExtractor.extractToken(authHeader);
        AuthorizationResponse response = authService.isAppAuthorization(accessToken);

        return ResponseEntity.ok(response);
    }

    /**
     * POST /auth/isBaseAuthorization
     * Проверка base авторизации (для других микросервисов)
     */
    @PostMapping("/auth/isBaseAuthorization")
    public ResponseEntity<AuthorizationResponse> isBaseAuthorization(
            @RequestHeader("Authorization") String authHeader) {

        log.debug("Checking base authorization");

        String accessToken = tokenExtractor.extractToken(authHeader);
        AuthorizationResponse response = authService.isBaseAuthorization(accessToken);

        return ResponseEntity.ok(response);
    }

    // ========== ЗАЩИЩЕННЫЕ ENDPOINTS (требуют токен) ==========

    /**
     * POST /auth/editUser
     * Редактирование пользователя
     *
     * ВАЖНО: userId извлекается из JWT токена через @CurrentUser
     */
    @PostMapping("/auth/editUser")
    public ResponseEntity<UserResponse> editUser(
            @CurrentUser Integer userId,
            @RequestBody UpdateUserRequest request) {

        log.info("Edit user request from userId: {}", userId);

        // Проверка что пользователь редактирует себя
        if (request.getId() != null && !request.getId().equals(userId)) {
            log.warn("User {} attempted to edit user {}", userId, request.getId());
            throw new SecurityException("Cannot edit other users");
        }

        UserResponse response = authService.editUser(
                userId,
                request.getPassword(),
                request.getNewPassword(),
                request.getEmail(),
                request.getPhone(),
                request.getUserName(),
                request.getLogin()
        );

        return ResponseEntity.ok(response);
    }

    /**
     * POST /auth/checkProjectId
     * Проверка принадлежности проекта пользователю
     */
    @PostMapping("/auth/checkProjectId")
    public ResponseEntity<ProjectAccessResponse> checkProjectId(
            @CurrentUser Integer userId,
            @RequestBody CheckProjectRequest request) {

        log.debug("Checking project access: userId={}, projectId={}",
                userId, request.getProjectId());

        // Используем userId из токена, игнорируем из request
        boolean hasAccess = authService.checkProjectId(userId, request.getProjectId());

        return ResponseEntity.ok(
                ProjectAccessResponse.builder()
                        .hasAccess(hasAccess)
                        .build()
        );
    }

    /**
     * POST /auth/checkRoleAdmin
     * Проверка роли администратора
     */
    @PostMapping("/auth/checkRoleAdmin")
    public ResponseEntity<StatusResponse> checkRoleAdmin(@CurrentUser Integer userId) {

        log.debug("Checking admin role for userId: {}", userId);

        boolean isAdmin = authService.checkRoleAdmin(userId);

        return ResponseEntity.ok(
                StatusResponse.builder()
                        .status(isAdmin)
                        .message(isAdmin ? "User is admin" : "User is not admin")
                        .build()
        );
    }

    // ========== API v1 ENDPOINTS ==========

    /**
     * GET /api/v1/auth/checkFieldForUniqueness
     * Проверка уникальности поля (публичный endpoint для формы регистрации)
     */
    @GetMapping("/api/v1/auth/checkFieldForUniqueness")
    public ResponseEntity<UniquenessResponse> checkFieldForUniqueness(
            @RequestParam String field,
            @RequestParam String value) {

        log.debug("Checking uniqueness for field: {}", field);

        UniquenessResponse response = authService.checkFieldForUniqueness(field, value);

        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/v1/user
     * Получение данных текущего пользователя
     */
    @GetMapping("/api/v1/user")
    public ResponseEntity<UserResponse> getUser(@CurrentUser Integer userId) {

        log.debug("Get user request for userId: {}", userId);

        UserResponse response = authService.getUser(userId);

        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/v1/base/user
     * Получение базовых данных пользователя (через Base токен)
     */
    @GetMapping("/api/v1/base/user")
    public ResponseEntity<BaseUserResponse> getBaseUser(@CurrentUser Integer userId) {

        log.debug("Get base user request for userId: {}", userId);

        BaseUserResponse response = authService.getBaseUser(userId);

        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/v1/user/company
     * Получение компаний пользователя
     */
    @GetMapping("/api/v1/user/company")
    public ResponseEntity<CompaniesResponse> getCompanies(@CurrentUser Integer userId) {

        log.debug("Get companies request for userId: {}", userId);

        CompaniesResponse response = authService.getCompanies(userId);

        return ResponseEntity.ok(response);
    }
}