package ru.platik777.backauth.service;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import ru.platik777.backauth.entity.Tenant;
import ru.platik777.backauth.entity.User;
import ru.platik777.backauth.entity.types.AccountType;
import ru.platik777.backauth.exception.ValidationException;
import ru.platik777.backauth.repository.UserRepository;

import java.util.regex.Pattern;

/**
 * Сервис валидации данных
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ValidationService {

    private final UserRepository userRepository;
    private final PasswordService passwordService;

    // Регулярные выражения для валидации
    private static final Pattern LOGIN_PATTERN = Pattern.compile("^[a-zA-Z][a-zA-Z0-9_]{2,}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("([a-zA-Z0-9._-]+@[a-zA-Z0-9._-]+\\.[a-zA-Z0-9_-]+)");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+\\d{1,4} \\d{1,4} \\d{1,12}$");

    /**
     * Валидация данных при регистрации
     *
     * @param user Пользователь (будет изменен)
     * @param tenant Компания (может быть null)
     * @throws ValidationException если валидация не прошла
     */
    public void validateSignUp(User user, Tenant tenant) {
        log.debug("Starting sign-up validation for login: {}", user.getLogin());

        // Нормализация данных (приведение к нижнему регистру)
        normalizeUserData(user);

        // Установка дефолтного типа аккаунта
        if (user.getAccountType() == null) {
            user.setAccountType(AccountType.INDIVIDUAL);
            log.debug("Set default account type: INDIVIDUAL");
        }

        // Валидация общих данных пользователя
        validateUserCommonData(user);

        // Валидация по типам аккаунтов
        validateByAccountType(user, tenant);

        log.debug("Sign-up validation completed successfully");
    }

    /**
     * Проверка и хеширование паролей при смене
     *
     * @param oldPasswordHashFromDb Хеш текущего пароля из БД
     * @param currentPasswordPlain Текущий пароль от пользователя (открытый)
     * @param newPasswordPlain Новый пароль от пользователя (открытый)
     * @return Хеш нового пароля для сохранения
     */
    public String validateAndHashPasswordChange(String oldPasswordHashFromDb,
                                                String currentPasswordPlain,
                                                String newPasswordPlain) {
        log.debug("Validating password change");

        // Проверка что текущий пароль не пустой
        if (!StringUtils.hasText(currentPasswordPlain)) {
            throw new ValidationException("Current password cannot be empty", "password");
        }

        // Проверка что новый пароль не пустой
        if (!StringUtils.hasText(newPasswordPlain)) {
            throw new ValidationException("New password cannot be empty", "newPassword");
        }

        // Хешируем введенный текущий пароль
        // Go: newUserData.Password, err = generatePasswordHash(logger, newUserData.Password)
        String currentPasswordHash = passwordService.generatePasswordHash(currentPasswordPlain);

        // Сравниваем с хешем из БД
        if (!currentPasswordHash.equals(oldPasswordHashFromDb)) {
            throw new ValidationException(
                    "Wrong current password entered",
                    "password"
            );
        }

        // Хешируем новый пароль
        String newPasswordHash = passwordService.generatePasswordHash(newPasswordPlain);

        log.debug("Password change validation completed, new password hashed");
        return newPasswordHash;
    }

    /**
     * Проверка поля на уникальность
     *
     * @param field Имя поля (phone/email/login)
     * @param value Значение для проверки
     * @return true если уникальное
     */
    public boolean checkFieldUniqueness(String field, String value) {
        log.debug("Checking uniqueness for field: {}", field);

        return switch (field.toLowerCase()) {
            case "phone" -> {
                validatePhone(value);
                yield !userRepository.existsByPhone(value);
            }
            case "email" -> {
                validateEmail(value);
                yield !userRepository.existsByEmail(value);
            }
            case "login" -> {
                validateLogin(value);
                yield !userRepository.existsByLoginIgnoreCase(value);
            }
            default -> throw new ValidationException(
                    "Unknown field type for uniqueness check: " + field,
                    field
            );
        };
    }

    /**
     * Валидация логина
     * <p/>
     * Формат: начинается с буквы, минимум 3 символа, буквы/цифры/underscore
     */
    public void validateLogin(String login) {
        if (!StringUtils.hasText(login)) {
            throw new ValidationException("Login cannot be empty", "login");
        }

        String lowerLogin = login.toLowerCase();
        if (!LOGIN_PATTERN.matcher(lowerLogin).matches()) {
            throw new ValidationException(
                    "Invalid login format. Must start with letter, min 3 chars, alphanumeric + underscore",
                    "login"
            );
        }
    }

    /**
     * Валидация email
     */
    public void validateEmail(String email) {
        if (!StringUtils.hasText(email)) {
            throw new ValidationException("Email cannot be empty", "email");
        }

        String lowerEmail = email.toLowerCase();
        if (!EMAIL_PATTERN.matcher(lowerEmail).matches()) {
            throw new ValidationException("Invalid email format", "email");
        }
    }

    /**
     * Валидация телефона
     * <p/>
     * Формат: +<код страны 1-4 цифры> <код оператора 1-4 цифры> <номер 1-12 цифр>
     * Пример: +7 916 1234567890
     */
    public void validatePhone(String phone) {
        if (!StringUtils.hasText(phone)) {
            throw new ValidationException("Phone cannot be empty", "phone");
        }

        String lowerPhone = phone.toLowerCase();
        if (!PHONE_PATTERN.matcher(lowerPhone).matches()) {
            throw new ValidationException(
                    "Invalid phone format. Expected: +<country> <operator> <number>",
                    "phone"
            );
        }
    }

    /**
     * Валидация пароля
     */
    public void validatePassword(String password) {
        if (!StringUtils.hasText(password)) {
            throw new ValidationException("Password cannot be empty", "password");
        }

        // Можно добавить дополнительные проверки:
        // - минимальная длина
        // - наличие спецсимволов
        // - наличие цифр
        // Но в Go версии их нет, так что оставляем только проверку на пустоту
    }

    /**
     * Валидация имени пользователя
     */
    public void validateUserName(String userName) {
        if (!StringUtils.hasText(userName)) {
            throw new ValidationException("UserName cannot be empty", "userName");
        }
    }

    /**
     * Валидация сообщения (для техподдержки)
     */
    public void validateMessage(String message) {
        if (!StringUtils.hasText(message)) {
            throw new ValidationException("Message cannot be empty", "message");
        }

        if (message.length() > 4096) {
            throw new ValidationException("Message is too long (max 4096 characters)", "message");
        }
    }

    /**
     * Нормализация данных пользователя
     */
    private void normalizeUserData(User user) {
        if (user.getLogin() != null) {
            user.setLogin(user.getLogin().toLowerCase());
        }
        if (user.getEmail() != null) {
            user.setEmail(user.getEmail().toLowerCase());
        }
        if (user.getPhone() != null) {
            user.setPhone(user.getPhone().toLowerCase());
        }
    }

    /**
     * Валидация общих данных пользователя
     */
    private void validateUserCommonData(User user) {
        if (user == null) {
            throw new ValidationException("User cannot be null", "user");
        }

        // Валидация форматов полей
        validateLogin(user.getLogin());
        validateEmail(user.getEmail());
        validatePhone(user.getPhone());

        if (!StringUtils.hasText(user.getPasswordHash())) {
            throw new ValidationException("Password hash cannot be empty", "password");
        }

        if (userRepository.existsByLoginIgnoreCase(user.getLogin())) {
            throw new ValidationException("Login is already in use", "login");
        }

        if (userRepository.existsByEmail(user.getEmail())) {
            throw new ValidationException("Email is already in use", "email");
        }

        if (userRepository.existsByPhone(user.getPhone())) {
            throw new ValidationException("Phone is already in use", "phone");
        }

        // Проверка типа аккаунта
        if (user.getAccountType() == null) {
            throw new ValidationException("Account type cannot be empty", "accountType");
        }
    }

    /**
     * Валидация по типам аккаунтов
     */
    private void validateByAccountType(User user, Tenant tenant) {
        switch (user.getAccountType()) {
            case INDIVIDUAL:
                // Не требует дополнительных проверок
                log.debug("Account type: INDIVIDUAL - no additional validation needed");
                break;

            case ENTREPRENEUR:
                if (tenant == null) {
                    throw new ValidationException(
                            "Company data cannot be null for ENTREPRENEUR account type",
                            "company"
                    );
                }
                validateGeneralCompanyFields(tenant);
                validateNotEmpty(tenant.getTaxNumber(), "taxNumber", "INN cannot be empty");
                validateNotEmpty(tenant.getOgrnip(), "ogrnip", "OGRNIP cannot be empty");
                break;

            case COMPANY_RESIDENT:
                if (tenant == null) {
                    throw new ValidationException(
                            "Company data cannot be null for COMPANY_RESIDENT account type",
                            "company"
                    );
                }
                validateGeneralCompanyFields(tenant);
                validateNotEmpty(tenant.getTaxNumber(), "taxNumber", "INN cannot be empty");
                validateNotEmpty(tenant.getOgrn(), "ogrn", "OGRN cannot be empty");
                validateNotEmpty(tenant.getKpp(), "kpp", "KPP cannot be empty");
                break;

            case COMPANY_NON_RESIDENT:
                if (tenant == null) {
                    throw new ValidationException(
                            "Company data cannot be null for COMPANY_NON_RESIDENT account type",
                            "company"
                    );
                }
                validateGeneralCompanyFields(tenant);
                validateNotEmpty(tenant.getKio(), "kio", "KIO cannot be empty");
                validateNotEmpty(tenant.getOgrn(), "ogrn", "OGRN cannot be empty");
                validateNotEmpty(tenant.getKpp(), "kpp", "KPP cannot be empty");
                break;

            case EDUCATIONAL_UNIT:
                if (tenant == null) {
                    throw new ValidationException(
                            "Company data cannot be null for EDUCATIONAL_UNIT account type",
                            "company"
                    );
                }
                validateGeneralCompanyFields(tenant);
                validateNotEmpty(tenant.getTaxNumber(), "taxNumber", "INN cannot be empty");
                validateNotEmpty(tenant.getOgrn(), "ogrn", "OGRN cannot be empty");
                validateNotEmpty(tenant.getKpp(), "kpp", "KPP cannot be empty");
                validateNotEmpty(tenant.getLicenseNumber(), "licenseNumber",
                        "License number cannot be empty");
                validateNotEmpty(String.valueOf(tenant.getLicenseIssueDate()), "licenseIssueDate",
                        "License issue date cannot be empty");
                break;

            default:
                throw new ValidationException(
                        "Account type is not supported: " + user.getAccountType(),
                        "accountType"
                );
        }
    }

    /**
     * Валидация общих полей компании
     */
    private void validateGeneralCompanyFields(Tenant tenant) {
        validateNotEmpty(tenant.getFullTitle(), "fullTitle",
                "Full company name cannot be empty");

        if (!StringUtils.hasText(tenant.getCountry())) {
            tenant.setCountry("russia");
        }

        validateNotEmpty(tenant.getLegalAddress(), "legalAddress",
                "Legal address cannot be empty");
        validateNotEmpty(tenant.getPostAddress(), "postAddress",
                "Postal address cannot be empty");
        validateNotEmpty(tenant.getAccountNumber(), "accountNumber",
                "Current account cannot be empty");
        validateNotEmpty(tenant.getBankName(), "bankName",
                "Bank name cannot be empty");
        validateNotEmpty(tenant.getBic(), "bic",
                "BIC cannot be empty");
        validateNotEmpty(tenant.getTaxBank(), "taxBank",
                "Bank INN cannot be empty");
        validateNotEmpty(tenant.getKppBank(), "kppBank",
                "Bank KPP cannot be empty");
        validateNotEmpty(tenant.getCorrespondentAccount(), "correspondentAccount",
                "Correspondent account cannot be empty");
    }

    /**
     * Проверка что поле не пустое
     */
    private void validateNotEmpty(String value, String fieldName, String errorMessage) {
        if (!StringUtils.hasText(value)) {
            throw new ValidationException(errorMessage, fieldName);
        }
    }

    /**
     * Проверка изменилось ли поле
     */
    private boolean isFieldChanged(String newValue, String oldValue) {
        if (!StringUtils.hasText(newValue)) {
            return false;
        }

        return !newValue.equals(oldValue);
    }
}