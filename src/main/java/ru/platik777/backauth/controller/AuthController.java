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
import ru.platik777.backauth.mapper.StudentMapper;
import ru.platik777.backauth.mapper.UserMapper;
import ru.platik777.backauth.service.AuthService;
import ru.platik777.backauth.util.TokenExtractor;

/**
 * Основной контроллер аутентификации и авторизации
 * Соответствует auth.go endpoints
 *
 * ВАЖНО: Пути точно соответствуют Go роутингу
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserMapper userMapper;
    private final StudentMapper studentMapper;
    private final CompanyMapper companyMapper;
    private final TokenExtractor tokenExtractor;

    /**
     * POST /auth/signUp
     * Регистрация нового пользователя
     * Go: authR.HandleFunc("/signUp", h.signUp).Methods(http.MethodPost)
     */
    @PostMapping("/auth/signUp")
    public ResponseEntity<SignUpResponse> signUp(@RequestBody SignUpRequest request) {
        log.info("Sign-up request received for login: {}",
                request.getUser() != null ? request.getUser().getLogin() : "null");

        User user = userMapper.toEntity(request.getUser());
        Company company = companyMapper.toEntity(request.getCompany());

        SignUpResponse response = authService.signUp(user, company, request.getLocale());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * POST /auth/signIn
     * Вход пользователя
     * Go: authR.HandleFunc("/signIn", h.singIn).Methods(http.MethodPost)
     */
    @PostMapping("/auth/signIn")
    public ResponseEntity<SignInResponse> signIn(
            @RequestBody SignInRequest request,
            @RequestHeader(value = "User-Agent", required = false) String userAgent) {

        log.info("Sign-in request received for login: {}", request.getLogin());

        SignInResponse response = authService.signIn(
                request.getLogin(),
                request.getPassword(),
                userAgent
        );

        return ResponseEntity.ok(response);
    }

    /**
     * POST /auth/editUser
     * Редактирование пользователя
     * Go: authR.HandleFunc("/editUser", h.editUser).Methods(http.MethodPost)
     */
    @PostMapping("/auth/editUser")
    public ResponseEntity<UserResponse> editUser(@RequestBody UpdateUserRequest request) {
        log.info("Edit user request for userId: {}", request.getId());

        UserResponse response = authService.editUser(
                request.getId(),
                request.getPassword(),      // current password
                request.getNewPassword(),   // new password
                request.getEmail(),
                request.getPhone(),
                request.getUserName(),
                request.getLogin()
        );

        return ResponseEntity.ok(response);
    }

    /**
     * POST /auth/refreshToken
     * Обновление app токенов
     * Go: authR.HandleFunc("/refreshToken", h.refreshAppToken).Methods(http.MethodPost)
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
     * Обновление app токенов через base refresh token
     * Go: authR.HandleFunc("/refreshTokenByBaseToken", h.refreshAppTokenByBaseToken).Methods(http.MethodPost)
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
     * Go: authR.HandleFunc("/refreshBaseToken", h.refreshBaseToken).Methods(http.MethodPost)
     */
    @PostMapping("/auth/refreshBaseToken")
    public ResponseEntity<TokenResponse> refreshBaseToken(
            @RequestBody TokenRefreshRequest request) {

        log.debug("Refreshing base token");

        TokenResponse response = authService.refreshBaseToken(request.getRefreshToken());

        return ResponseEntity.ok(response);
    }

    /**
     * POST /auth/isAuthorization
     * Проверка app авторизации
     * Go: authR.HandleFunc("/isAuthorization", h.isAppAuthorization).Methods(http.MethodPost)
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
     * Проверка base авторизации
     * Go: authR.HandleFunc("/isBaseAuthorization", h.isBaseAuthorization).Methods(http.MethodPost)
     */
    @PostMapping("/auth/isBaseAuthorization")
    public ResponseEntity<AuthorizationResponse> isBaseAuthorization(
            @RequestHeader("Authorization") String authHeader) {

        log.debug("Checking base authorization");

        String accessToken = tokenExtractor.extractToken(authHeader);
        AuthorizationResponse response = authService.isBaseAuthorization(accessToken);

        return ResponseEntity.ok(response);
    }

    /**
     * POST /auth/checkProjectId
     * Проверка принадлежности проекта пользователю
     * Go: authR.HandleFunc("/checkProjectId", h.checkProjectId).Methods(http.MethodPost)
     */
    @PostMapping("/auth/checkProjectId")
    public ResponseEntity<ProjectAccessResponse> checkProjectId(
            @RequestBody CheckProjectRequest request) {

        log.debug("Checking project access: userId={}, projectId={}",
                request.getUserId(), request.getProjectId());

        boolean hasAccess = authService.checkProjectId(
                request.getUserId(),
                request.getProjectId()
        );

        return ResponseEntity.ok(
                ProjectAccessResponse.builder()
                        .hasAccess(hasAccess)
                        .build()
        );
    }

    /**
     * POST /auth/checkRoleAdmin
     * Проверка роли администратора
     * Go: authR.HandleFunc("/checkRoleAdmin", h.checkRoleAdmin).Methods(http.MethodPost)
     */
    @PostMapping("/auth/checkRoleAdmin")
    public ResponseEntity<StatusResponse> checkRoleAdmin(
            @RequestHeader("userId") Integer userId) {

        log.debug("Checking admin role for userId: {}", userId);

        boolean isAdmin = authService.checkRoleAdmin(userId);

        return ResponseEntity.ok(
                StatusResponse.builder()
                        .status(isAdmin)
                        .message(isAdmin ? "User is admin" : "User is not admin")
                        .build()
        );
    }

    /**
     * GET /api/v1/auth/checkFieldForUniqueness
     * Проверка уникальности поля (login, email, phone)
     * Go: apiv1RAuthR.HandleFunc("/checkFieldForUniqueness", h.checkFieldForUniqueness).Methods(http.MethodGet)
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
     * Получение пользователя
     * Go: userR.HandleFunc("", h.getUser).Methods(http.MethodGet)
     * ВАЖНО: userId передается через header
     */
    @GetMapping("/api/v1/user")
    public ResponseEntity<UserResponse> getUser(@RequestHeader("userId") Integer userId) {
        log.debug("Get user request for userId: {}", userId);

        UserResponse response = authService.getUser(userId);

        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/v1/base/user
     * Получение базовых данных пользователя
     * Go: apiv1R.HandleFunc("/base/user", h.getUserByBaseToken).Methods(http.MethodGet)
     * ВАЖНО: userId передается через header
     */
    @GetMapping("/api/v1/base/user")
    public ResponseEntity<BaseUserResponse> getBaseUser(@RequestHeader("userId") Integer userId) {
        log.debug("Get base user request for userId: {}", userId);

        BaseUserResponse response = authService.getBaseUser(userId);

        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/v1/user/company
     * Получение компаний пользователя
     * Go: userR.HandleFunc("/company", h.getCompaniesOfUser).Methods(http.MethodGet)
     * ВАЖНО: userId передается через header
     */
    @GetMapping("/api/v1/user/company")
    public ResponseEntity<CompaniesResponse> getCompanies(@RequestHeader("userId") Integer userId) {
        log.debug("Get companies request for userId: {}", userId);

        CompaniesResponse response = authService.getCompanies(userId);

        return ResponseEntity.ok(response);
    }
}