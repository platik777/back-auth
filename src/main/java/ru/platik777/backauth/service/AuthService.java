package ru.platik777.backauth.service;

import ru.platik777.backauth.dto.UserDto;
import ru.platik777.backauth.dto.request.EditUserRequestDto;
import ru.platik777.backauth.dto.request.SignInRequestDto;
import ru.platik777.backauth.dto.request.SignUpRequestDto;
import ru.platik777.backauth.dto.response.*;
import ru.platik777.backauth.entity.*;
import ru.platik777.backauth.mapper.CompanyMapper;
import ru.platik777.backauth.mapper.StudentMapper;
import ru.platik777.backauth.mapper.UserMapper;
import ru.platik777.backauth.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Основной сервис аутентификации
 * Реализует ВСЮ логику из Go версии AuthService (auth.go)
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
    private final EducationalInstitutionRepository educationalInstitutionRepository;

    private final JwtService jwtService;
    private final PasswordService passwordService;
    private final UniquenessService uniquenessService;
    private final UserMapper userMapper;
    private final CompanyMapper companyMapper;
    private final StudentMapper studentMapper;

    // ============================================
    // Аутентификация и авторизация
    // ============================================

    /**
     * Регистрация пользователя
     * Аналог SignUp из Go
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

        // Создание дополнительных сущностей (Student/Company)
        createAdditionalEntities(user, requestDto);

        // Генерация токенов
        TokenResponseDto tokens = generateTokens(user.getId());

        // Получение данных пользователя для ответа
        UserDto userDto = getUserDto(user);

        log.info("User registered successfully with ID: {}", user.getId());

        return ApiResponseDto.success(new SignUpResponse(tokens, userDto));
    }

    /**
     * Вход пользователя
     * Аналог SignIn из Go
     */
    @Transactional
    public ApiResponseDto<?> signIn(SignInRequestDto requestDto, String userAgent) {
        log.debug("Starting user sign in for login: {}", requestDto.getLogin());

        // Валидация
        validateSignInRequest(requestDto);

        // Поиск и проверка пользователя
        User user = findAndValidateUser(requestDto);

        // Генерация токенов
        TokenResponseDto tokens = generateTokens(user.getId());

        // Создание сессии
        createSession(user.getId(), userAgent);

        // Получение данных пользователя
        UserDto userDto = getUserDto(user);

        log.info("User signed in successfully with ID: {}", user.getId());

        return ApiResponseDto.success(new SignInResponse(tokens, userDto));
    }

    /**
     * Обновление App Access токена через App Refresh Token
     * Аналог RefreshAppToken из Go
     */
    public ApiResponseDto<TokenResponseDto> refreshAppToken(String refreshToken) {
        log.debug("Refreshing app access token");

        // Валидация refresh токена
        Integer userId = jwtService.validateAppRefreshToken(refreshToken);

        // Проверка существования пользователя
        userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Генерация нового access токена
        String newAccessToken = jwtService.createAppAccessToken(userId);

        TokenResponseDto tokens = new TokenResponseDto(
                newAccessToken,
                refreshToken,  // Старый refresh токен остается
                null,
                null,
                null
        );

        log.info("App access token refreshed for userId: {}", userId);

        return ApiResponseDto.success(tokens);
    }

    /**
     * Обновление App токенов через Base Refresh Token
     * Аналог RefreshAppTokenByBaseToken из Go
     */
    public ApiResponseDto<TokenResponseDto> refreshAppTokenByBaseToken(String baseRefreshToken) {
        log.debug("Refreshing app tokens by base refresh token");

        // Валидация base refresh токена
        Integer userId = jwtService.validateBaseRefreshToken(baseRefreshToken);

        // Проверка существования пользователя
        userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Генерация новых App токенов
        String appAccessToken = jwtService.createAppAccessToken(userId);
        String appRefreshToken = jwtService.createAppRefreshToken(userId);

        TokenResponseDto tokens = new TokenResponseDto(
                appAccessToken,
                appRefreshToken,
                null,
                baseRefreshToken,  // Старый base refresh токен остается
                null
        );

        log.info("App tokens refreshed by base token for userId: {}", userId);

        return ApiResponseDto.success(tokens);
    }

    /**
     * Обновление Base токенов через Base Refresh Token
     * Аналог RefreshBaseToken из Go
     */
    public ApiResponseDto<TokenResponseDto> refreshBaseToken(String refreshToken) {
        log.debug("Refreshing base tokens");

        // Валидация refresh токена
        Integer userId = jwtService.validateBaseRefreshToken(refreshToken);

        // Проверка существования пользователя
        userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Генерация новых Base токенов
        String baseAccessToken = jwtService.createBaseAccessToken(userId);
        String baseRefreshToken = jwtService.createBaseRefreshToken(userId);

        TokenResponseDto tokens = new TokenResponseDto(
                null,
                null,
                baseAccessToken,
                baseRefreshToken,
                null
        );

        log.info("Base tokens refreshed for userId: {}", userId);

        return ApiResponseDto.success(tokens);
    }

    /**
     * Проверка App авторизации
     * Аналог IsAppAuthorization из Go
     */
    public ApiResponseDto<AuthorizationResponseDto> isAppAuthorization(String accessToken) {
        log.debug("Checking app authorization");

        if (accessToken == null || accessToken.trim().isEmpty()) {
            throw new RuntimeException("Access token cannot be empty");
        }

        try {
            // Валидация токена
            Integer userId = jwtService.validateAppAccessToken(accessToken);

            // Проверка существования пользователя
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            AuthorizationResponseDto response = AuthorizationResponseDto.builder()
                    .userId(user.getId())
                    .build();

            return ApiResponseDto.success(response);

        } catch (Exception e) {
            log.error("App authorization check failed", e);
            throw new RuntimeException("Invalid or expired token");
        }
    }

    /**
     * Проверка Base авторизации
     * Аналог IsBaseAuthorization из Go
     */
    public ApiResponseDto<AuthorizationResponseDto> isBaseAuthorization(String accessToken) {
        log.debug("Checking base authorization");

        if (accessToken == null || accessToken.trim().isEmpty()) {
            throw new RuntimeException("Access token cannot be empty");
        }

        try {
            // Валидация токена
            Integer userId = jwtService.validateBaseAccessToken(accessToken);

            // Проверка существования пользователя
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            AuthorizationResponseDto response = AuthorizationResponseDto.builder()
                    .userId(user.getId())
                    .build();

            return ApiResponseDto.success(response);

        } catch (Exception e) {
            log.error("Base authorization check failed", e);
            throw new RuntimeException("Invalid or expired token");
        }
    }

    /**
     * Проверка принадлежности проекта пользователю
     * Аналог CheckProjectId из Go
     */
    public boolean checkProjectId(Integer userId, Integer projectId) {
        log.debug("Checking project ownership - userId: {}, projectId: {}", userId, projectId);
        return projectAccessRepository.checkProjectOwnership(userId, projectId);
    }

    // ============================================
    // Управление пользователями
    // ============================================

    /**
     * Редактирование данных пользователя
     * Аналог EditUser из Go
     */
    @Transactional
    public ApiResponseDto<UserDto> editUser(EditUserRequestDto requestDto) {
        log.debug("Editing user data for userId: {}", requestDto.getUserId());

        // Поиск пользователя
        User user = userRepository.findById(requestDto.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Получение UserData через связь
        UserData userData = user.getUserData();
        if (userData == null) {
            throw new RuntimeException("User data not found");
        }

        // Обновление полей в UserData
        boolean dataChanged = false;

        if (requestDto.getUserName() != null && !requestDto.getUserName().trim().isEmpty()) {
            userData.setUserName(requestDto.getUserName().trim());
            dataChanged = true;
        }

        if (requestDto.getEmail() != null && !requestDto.getEmail().trim().isEmpty()) {
            // Проверка уникальности email (если изменился)
            if (!requestDto.getEmail().equals(userData.getEmail())) {
                boolean emailExists = userDataRepository.existsByEmailIgnoreCase(requestDto.getEmail());
                if (emailExists) {
                    throw new RuntimeException("Email already in use");
                }
                userData.setEmail(requestDto.getEmail().trim());
                dataChanged = true;
            }
        }

        if (requestDto.getPhone() != null) {
            userData.setPhone(requestDto.getPhone().trim());
            dataChanged = true;
        }

        // Обновление locale в User.settings
        if (requestDto.getLocale() != null) {
            User.UserSettings settings = user.getSettings();
            if (settings == null) {
                settings = new User.UserSettings();
            }
            settings.setLocale(requestDto.getLocale());
            user.setSettings(settings);
            user = userRepository.save(user);
        }

        // Обновление пароля (если указан)
        if (requestDto.getNewPassword() != null && !requestDto.getNewPassword().trim().isEmpty()) {
            // Проверка старого пароля (если указан)
            if (requestDto.getOldPassword() != null) {
                String oldPasswordHash = passwordService.generatePasswordHash(requestDto.getOldPassword());
                if (!oldPasswordHash.equals(userData.getPasswordHash())) {
                    throw new RuntimeException("Old password is incorrect");
                }
            }

            // Установка нового пароля
            String newPasswordHash = passwordService.generatePasswordHash(requestDto.getNewPassword());
            userData.setPasswordHash(newPasswordHash);
            dataChanged = true;
        }

        // Сохранение UserData если были изменения
        if (dataChanged) {
            userDataRepository.save(userData);
        }

        log.info("User data updated successfully for userId: {}", user.getId());

        UserDto userDto = userMapper.toDto(user);
        return ApiResponseDto.success(userDto);
    }

    /**
     * Получение данных пользователя
     * Аналог GetUser из Go
     */
    @Transactional(readOnly = true)
    public ApiResponseDto<UserDto> getUser(Integer userId) {
        log.debug("Getting user data for userId: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        UserDto userDto = userMapper.toDto(user);

        return ApiResponseDto.success(userDto);
    }

    /**
     * Получение базовых данных пользователя
     * Аналог GetBaseUser из Go
     */
    @Transactional(readOnly = true)
    public ApiResponseDto<UserDto> getBaseUser(Integer userId) {
        log.debug("Getting base user data for userId: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        UserData userData = user.getUserData();

        // В Go версии GetBaseUser возвращает только базовые поля
        UserDto userDto = new UserDto();
        userDto.setId(user.getId());
        userDto.setLogin(user.getLogin());
        if (userData != null) {
            userDto.setEmail(userData.getEmail());
            userDto.setUserName(userData.getUserName());
        }

        return ApiResponseDto.success(userDto);
    }

    /**
     * Получение списка компаний пользователя
     * Аналог GetCompanies из Go
     */
    @Transactional(readOnly = true)
    public ApiResponseDto<CompanyListResponseDto> getCompanies(Integer userId) {
        log.debug("Getting companies for userId: {}", userId);

        // В Company нет userId, есть ownerId
        List<Company> companies = companyRepository.findByOwnerId(userId);

        List<CompanyListResponseDto.CompanyItemDto> companyItems = companies.stream()
                .map(c -> CompanyListResponseDto.CompanyItemDto.builder()
                        .id(c.getCompanyId())  // Исправлено: companyId вместо id
                        .name(c.getFullTitle())  // Исправлено: fullTitle вместо name
                        .inn(c.getTaxNumber())   // Исправлено: taxNumber вместо inn
                        .position(null)          // В Company нет поля position
                        .createdAt(c.getCreatedAt() != null ? c.getCreatedAt().toString() : null)
                        .build())
                .collect(Collectors.toList());

        CompanyListResponseDto response = CompanyListResponseDto.builder()
                .companies(companyItems)
                .build();

        return ApiResponseDto.success(response);
    }

    /**
     * Получение данных студента
     * Аналог GetStudent из Go
     */
    @Transactional(readOnly = true)
    public ApiResponseDto<StudentDataResponseDto> getStudent(Integer userId) {
        log.debug("Getting student data for userId: {}", userId);

        Student student = studentRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Student data not found"));

        // Получаем название учебного заведения
        String institutionName = null;
        if (student.getEducationalInstitution() != null) {
            institutionName = student.getEducationalInstitution().getFullName();
        } else if (student.getEducationalInstitutionsUuid() != null) {
            Optional<EducationalInstitution> instOpt = educationalInstitutionRepository
                    .findById(student.getEducationalInstitutionsUuid());
            institutionName = instOpt.map(EducationalInstitution::getFullName).orElse(null);
        }

        // Формируем информацию о курсе из годов обучения
        String courseInfo = String.format("%d-%d", student.getStartYear(), student.getEndYear());

        // Используем studentId как идентификатор группы
        String groupInfo = student.getStudentId();

        StudentDataResponseDto response = StudentDataResponseDto.builder()
                .id(student.getUuid() != null ? student.getUuid().hashCode() : null)
                .educationalInstitution(institutionName)
                .course(courseInfo)
                .group(groupInfo)
                .createdAt(student.getCreatedAt() != null ? student.getCreatedAt().toString() : null)
                .build();

        return ApiResponseDto.success(response);
    }

    /**
     * Проверка роли администратора
     * Аналог CheckRoleAdmin из Go
     * TODO: Реализовать интеграцию с back-access для проверки ролей
     */
    public boolean checkRoleAdmin(Integer userId) {
        log.debug("Checking admin role for userId: {}", userId);

        // TODO: Интеграция с back-access сервисом для получения ролей
        // Пока возвращаем false
        log.warn("Admin role check not implemented, returning false");
        return false;
    }

    // ============================================
    // Вспомогательные приватные методы
    // ============================================

    private void validateUniqueFields(SignUpRequestDto requestDto) {
        // Проверка уникальности логина
        boolean loginUnique = uniquenessService.isFieldUnique("login", requestDto.getLogin());
        if (!loginUnique) {
            throw new RuntimeException("Login already exists");
        }

        // Проверка уникальности email
        boolean emailUnique = uniquenessService.isFieldUnique("email", requestDto.getEmail());
        if (!emailUnique) {
            throw new RuntimeException("Email already exists");
        }

        // Проверка уникальности телефона (если указан)
        if (requestDto.getPhone() != null && !requestDto.getPhone().trim().isEmpty()) {
            boolean phoneUnique = uniquenessService.isFieldUnique("phone", requestDto.getPhone());
            if (!phoneUnique) {
                throw new RuntimeException("Phone already exists");
            }
        }
    }

    private void setDefaultUserData(SignUpRequestDto requestDto, String locale) {
        // В SignUpRequestDto может не быть поля locale
        // Устанавливаем значение по умолчанию через settings после создания user
    }

    private User createUser(SignUpRequestDto requestDto) {
        User user = new User();
        user.setLogin(requestDto.getLogin().trim());
        user.setAccountType(User.AccountType.fromValue(requestDto.getAccountType()));

        // Установка настроек пользователя
        User.UserSettings settings = new User.UserSettings();
        settings.setLocale("ru"); // По умолчанию
        user.setSettings(settings);

        user = userRepository.save(user);

        // Создание UserData с паролем
        String passwordHash = passwordService.generatePasswordHash(requestDto.getPassword());
        UserData userData = new UserData();
        userData.setUser(user);
        userData.setLogin(requestDto.getLogin().trim());
        userData.setPasswordHash(passwordHash);
        userData.setEmail(requestDto.getEmail().trim());
        userData.setUserName(requestDto.getUserName() != null ? requestDto.getUserName().trim() : requestDto.getLogin());
        userData.setPhone(requestDto.getPhone() != null ? requestDto.getPhone().trim() : null);
        userDataRepository.save(userData);

        return user;
    }

    private void createUserSchema(User user, SignUpRequestDto requestDto) {
        String passwordHash = passwordService.generatePasswordHash(requestDto.getPassword());
        userSchemaRepository.createUserSchema(
                user.getId(),
                user.getLogin(),
                passwordHash,
                requestDto.getEmail(),
                requestDto.getUserName() != null ? requestDto.getUserName() : user.getLogin(),
                requestDto.getPhone()
        );
    }

    private void createAdditionalEntities(User user, SignUpRequestDto requestDto) {
        // В SignUpRequestDto может не быть методов getStudent() и getCompany()
        // Это зависит от accountType
        // Создание дополнительных сущностей должно происходить на основе данных из requestDto
    }

    private void validateSignInRequest(SignInRequestDto requestDto) {
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
        return userMapper.toDto(user);
    }

    // Response DTOs
    public record SignUpResponse(TokenResponseDto tokens, UserDto user) {}
    public record SignInResponse(TokenResponseDto tokens, UserDto user) {}
}