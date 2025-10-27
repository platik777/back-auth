package ru.platik777.backauth.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.platik777.backauth.dto.AuthenticatedUser;
import ru.platik777.backauth.dto.request.*;
import ru.platik777.backauth.dto.response.*;
import ru.platik777.backauth.entity.Tenant;
import ru.platik777.backauth.entity.User;
import ru.platik777.backauth.mapper.CompanyMapper;
import ru.platik777.backauth.mapper.UserMapper;
import ru.platik777.backauth.security.CurrentUser;
import ru.platik777.backauth.service.AuthService;

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

    /**
     * POST /auth/signUp
     * Регистрация нового пользователя
     */
    @PostMapping("/auth/signUp")
    public ResponseEntity<SignUpResponse> signUp(@RequestBody SignUpRequest request) {
        log.info("Sign-up request for login: {}",
                request.getUser() != null ? request.getUser().getLogin() : "null");

        log.debug("User in request: {}", request.getUser());
        User user = userMapper.toEntity(request.getUser());
        log.debug("User in controller: {}", user);
        Tenant tenant = companyMapper.toEntity(request.getCompany());

        SignUpResponse response = authService.signUp(user, tenant, request.getLocale());

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
    public ResponseEntity<TokenResponse> refreshToken(@CurrentUser AuthenticatedUser user) {
        log.debug("Refreshing app token for userId: {}, tenantId: {}",
                user.getUserId(), user.getTenantId());

        TokenResponse response = authService.refreshAppToken(user.getUserId(), user.getTenantId());
        return ResponseEntity.ok(response);
    }

    /**
     * POST /auth/refreshTokenByBaseToken
     * Обновление app токенов по base refresh token
     */
    @PostMapping("/auth/refreshTokenByBaseToken")
    public ResponseEntity<TokenResponse> refreshAppTokenByBaseToken(@CurrentUser AuthenticatedUser user) {

        log.debug("Refreshing app token by base token");

        TokenResponse response = authService.refreshAppTokenByBaseToken(user.getUserId(), user.getTenantId());

        return ResponseEntity.ok(response);
    }

    // ========== ЗАЩИЩЕННЫЕ ENDPOINTS (требуют токен) ==========

    /**
     * POST /auth/editUser
     * Редактирование пользователя
     *
     * ВАЖНО: userId извлекается из JWT токена через @CurrentUser
     */
    @PostMapping("/auth/user/edit")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<UserResponse> editUser(
            @CurrentUser AuthenticatedUser  user,
            @RequestBody UpdateUserRequest request) {

        log.info("Edit user request from userId: {}", user.getUserId());

        // Проверка что пользователь редактирует себя
        if (request.getId() != null && !request.getId().equals(user.getUserId())) {
            log.warn("User {} attempted to edit user {}", user.getUserId(), request.getId());
            throw new SecurityException("Cannot edit other users");
        }

        UserResponse response = authService.editUser(
                user.getUserId(),
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
            @CurrentUser AuthenticatedUser user,
            @RequestBody CheckProjectRequest request) {
        // TODO: Реализовать
        return null;
    }

    /**
     * POST /auth/checkRoleAdmin
     * Проверка роли администратора
     */
    @PostMapping("/auth/checkRoleAdmin")
    public ResponseEntity<StatusResponse> checkRoleAdmin(@CurrentUser AuthenticatedUser user) {

        log.debug("Checking admin role for userId: {}", user.getUserId());

        boolean isAdmin = authService.checkRoleAdmin(user.getUserId());

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
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<UserResponse> getUser(@CurrentUser AuthenticatedUser  user) {

        log.debug("Get user request for userId: {}", user.getUserId());

        UserResponse response = authService.getUser(user.getUserId());

        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/v1/base/user
     * Получение базовых данных пользователя (через Base токен)
     */
    @GetMapping("/api/v1/base/user")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<BaseUserResponse> getBaseUser(@CurrentUser AuthenticatedUser  user) {

        log.debug("Get base user request for userId: {}", user.getUserId());

        BaseUserResponse response = authService.getBaseUser(user.getUserId());

        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/v1/user/company
     * Получение компаний пользователя
     */
    @GetMapping("/api/v1/user/company")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<CompaniesResponse> getCompanies(@CurrentUser AuthenticatedUser  user) {

        log.debug("Get companies request for userId: {}", user.getUserId());

        CompaniesResponse response = authService.getCompanies(user.getUserId());

        return ResponseEntity.ok(response);
    }
}