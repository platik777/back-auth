package ru.platik777.backauth.service;

import ru.platik777.backauth.dto.UserDto;
import ru.platik777.backauth.dto.request.SignInRequestDto;
import ru.platik777.backauth.dto.request.SignUpRequestDto;
import ru.platik777.backauth.dto.response.ApiResponseDto;
import ru.platik777.backauth.dto.response.AuthorizationResponseDto;
import ru.platik777.backauth.dto.response.TokenResponseDto;
import ru.platik777.backauth.dto.response.UniquenessResponseDto;
import ru.platik777.backauth.entity.*;
import ru.platik777.backauth.mapper.CompanyMapper;
import ru.platik777.backauth.mapper.StudentMapper;
import ru.platik777.backauth.mapper.UserMapper;
import ru.platik777.backauth.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Основной сервис аутентификации
 * Реализует логику из Go версии AuthService
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final UserDataRepository userDataRepository;
    private final CompanyRepository companyRepository;
    private final StudentRepository studentRepository;
    private final UserSessionRepository userSessionRepository;
    private final UserSchemaRepository userSchemaRepository;
    private final ProjectAccessRepository projectAccessRepository;

    private final JwtService jwtService;
    private final PasswordService passwordService;
    private final UniquenessService uniquenessService;
    private final UserMapper userMapper;
    private final CompanyMapper companyMapper;
    private final StudentMapper studentMapper;

    /**
     * Регистрация пользователя (аналог SignUp из Go)
     */
    @Transactional
    public ApiResponseDto<?> signUp(SignUpRequestDto requestDto, String locale) {
        log.debug("Starting user registration for login: {}", requestDto.getLogin());

        // Валидация уникальности полей
        validateUniqueFields(requestDto);

        // Установка значений по умолчанию
        setDefaultUserData(requestDto, locale);

        // Создание пользователя
        User user = createUser(requestDto);

        // Создание пользовательской схемы БД
        createUserSchema(user, requestDto);

        // Создание дополнительных сущностей
        createAdditionalEntities(user, requestDto);

        // Получение данных пользователя для ответа
        UserDto userDto = getUserDto(user);

        log.info("User registered successfully with ID: {}", user.getId());
        return ApiResponseDto.success(userDto);
    }

    /**
     * Вход пользователя (аналог SignIn из Go)
     */
    @Transactional
    public TokenResponseDto signIn(SignInRequestDto requestDto, String userAgent) {
        log.debug("Starting user sign in for login: {}", requestDto.getLogin());

        // Валидация входных данных
        validateSignInData(requestDto);

        // Поиск пользователя и проверка пароля
        User user = findAndValidateUser(requestDto);

        // Создание токенов
        TokenResponseDto tokens = generateTokens(user.getId());

        // Создание сессии
        createSession(user.getId(), userAgent);

        // Получение данных пользователя
        UserDto userDto = getUserDto(user);
        tokens.setUser(userDto);

        log.info("User signed in successfully with ID: {}", user.getId());
        return tokens;
    }

    /**
     * Проверка уникальности поля
     */
    public UniquenessResponseDto checkFieldUniqueness(String field, String value) {
        boolean isUnique = uniquenessService.isFieldUnique(field, value);
        return isUnique ? UniquenessResponseDto.unique() : UniquenessResponseDto.notUnique();
    }

    /**
     * Обновление App токенов по Base Refresh токену
     */
    public TokenResponseDto refreshAppTokenByBaseToken(String refreshBaseToken) {
        Integer userId = jwtService.parseBaseRefreshToken(refreshBaseToken);
        return generateTokens(userId);
    }

    /**
     * Обновление App токенов
     */
    public TokenResponseDto refreshAppToken(String refreshToken) {
        Integer userId = jwtService.parseAppRefreshToken(refreshToken);
        return generateTokens(userId);
    }

    /**
     * Обновление Base токенов
     */
    public TokenResponseDto refreshBaseToken(String refreshToken) {
        Integer userId = jwtService.parseBaseRefreshToken(refreshToken);

        String accessBaseToken = jwtService.createBaseAccessToken(userId);
        String refreshBaseToken = jwtService.createBaseRefreshToken(userId);

        return new TokenResponseDto(null, null, accessBaseToken, refreshBaseToken, null);
    }

    /**
     * Проверка App авторизации
     */
    public AuthorizationResponseDto isAppAuthorization(String accessToken) {
        try {
            Integer userId = jwtService.parseAppAccessToken(accessToken);
            return AuthorizationResponseDto.success(userId);
        } catch (Exception e) {
            return AuthorizationResponseDto.invalid();
        }
    }

    /**
     * Проверка Base авторизации
     */
    public AuthorizationResponseDto isBaseAuthorization(String accessToken) {
        try {
            Integer userId = jwtService.parseBaseAccessToken(accessToken);
            return AuthorizationResponseDto.success(userId);
        } catch (Exception e) {
            return AuthorizationResponseDto.invalid();
        }
    }

    /**
     * Проверка принадлежности проекта пользователю
     */
    public boolean checkProjectId(Integer userId, Integer projectId) {
        return projectAccessRepository.checkProjectOwnership(userId, projectId);
    }

    /**
     * Получение данных пользователя
     */
    public UserDto getUser(Integer userId) {
        User user = userRepository.findByIdWithUserData(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return getUserDto(user);
    }

    /**
     * Получение базовых данных пользователя
     */
    public UserDto getBaseUser(Integer userId) {
        User user = userRepository.findByIdWithUserData(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        UserDto userDto = new UserDto();
        userDto.setId(user.getId());
        userDto.setLogin(user.getLogin());
        if (user.getUserData() != null) {
            userDto.setEmail(user.getUserData().getEmail());
            userDto.setUserName(user.getUserData().getUserName());
        }

        return userDto;
    }

    // Приватные методы

    private void validateUniqueFields(SignUpRequestDto requestDto) {
        if (!uniquenessService.isFieldUnique("login", requestDto.getLogin())) {
            throw new RuntimeException("Login is already in use");
        }
        if (!uniquenessService.isFieldUnique("email", requestDto.getEmail())) {
            throw new RuntimeException("Email is already in use");
        }
        if (!uniquenessService.isFieldUnique("phone", requestDto.getPhone())) {
            throw new RuntimeException("Phone is already in use");
        }
    }

    private void setDefaultUserData(SignUpRequestDto requestDto, String locale) {
        if (requestDto.getAccountType() == null || requestDto.getAccountType().isEmpty()) {
            requestDto.setAccountType("individual");
        }
    }

    private User createUser(SignUpRequestDto requestDto) {
        User user = userMapper.toEntity(requestDto);

        // Установка настроек пользователя
        User.UserSettings settings = new User.UserSettings();
        settings.setLocale("ru"); // По умолчанию
        user.setSettings(settings);

        return userRepository.save(user);
    }

    private void createUserSchema(User user, SignUpRequestDto requestDto) {
        String passwordHash = passwordService.generatePasswordHash(requestDto.getPassword());

        userSchemaRepository.createUserSchema(
                user.getId(),
                requestDto.getLogin(),
                passwordHash,
                requestDto.getEmail(),
                requestDto.getUserName(),
                requestDto.getPhone()
        );
    }

    private void createAdditionalEntities(User user, SignUpRequestDto requestDto) {
        switch (user.getAccountType()) {
            case STUDENT -> {
                Student student = studentMapper.toEntity(requestDto, user);
                studentRepository.save(student);
            }
            case ENTREPRENEUR, COMPANY_RESIDENT, COMPANY_NON_RESIDENT, EDUCATIONAL_UNIT -> {
                Company company = companyMapper.toEntity(requestDto, user);
                companyRepository.save(company);
            }
            default -> {
                // INDIVIDUAL - не требует дополнительных сущностей
            }
        }
    }

    private void validateSignInData(SignInRequestDto requestDto) {
        if (requestDto.getLogin() == null || requestDto.getLogin().trim().isEmpty()) {
            throw new RuntimeException("Login cannot be empty");
        }
        if (requestDto.getPassword() == null || requestDto.getPassword().trim().isEmpty()) {
            throw new RuntimeException("Password cannot be empty");
        }
    }

    private User findAndValidateUser(SignInRequestDto requestDto) {
        Optional<User> userOpt = userRepository.findByLoginWithUserData(requestDto.getLogin());
        if (userOpt.isEmpty()) {
            throw new RuntimeException("Invalid login or password");
        }

        User user = userOpt.get();
        UserData userData = user.getUserData();

        if (userData == null) {
            throw new RuntimeException("User data not found");
        }

        String passwordHash = passwordService.generatePasswordHash(requestDto.getPassword());
        if (!passwordHash.equals(userData.getPasswordHash())) {
            throw new RuntimeException("Invalid login or password");
        }

        return user;
    }

    private TokenResponseDto generateTokens(Integer userId) {
        String accessAppToken = jwtService.createAppAccessToken(userId);
        String refreshAppToken = jwtService.createAppRefreshToken(userId);
        String accessBaseToken = jwtService.createBaseAccessToken(userId);
        String refreshBaseToken = jwtService.createBaseRefreshToken(userId);

        return new TokenResponseDto(accessAppToken, refreshAppToken, accessBaseToken, refreshBaseToken, null);
    }

    private void createSession(Integer userId, String userAgent) {
        User user = userRepository.getReferenceById(userId);
        UserSession session = new UserSession();
        session.setUser(user);
        session.setAgent(userAgent != null ? userAgent : "Unknown");
        userSessionRepository.save(session);
    }

    private UserDto getUserDto(User user) {
        UserDto userDto = userMapper.toDto(user);
        // TODO: Добавить роли и права пользователя
        return userDto;
    }
}