package ru.platik777.backauth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import ru.platik777.backauth.entity.UserData;
import ru.platik777.backauth.entity.Company;
import ru.platik777.backauth.entity.User;
import ru.platik777.backauth.repository.UserDataRepository;
import ru.platik777.backauth.repository.UserRepository;

import java.util.regex.Pattern;

/**
 * Сервис валидации данных
 * Соответствует validations.go
 *
 * ВАЖНО: Этот сервис только проверяет данные, но НЕ изменяет их (кроме нормализации)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ValidationService {

    private final UserRepository userRepository;
    private final UserDataRepository userDataRepository;
    private final PasswordService passwordService;

    // Регулярные выражения для валидации
    // Go: validator := regexp.MustCompile(...)
    private static final Pattern LOGIN_PATTERN = Pattern.compile("^[a-zA-Z][a-zA-Z0-9_]{2,}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("([a-zA-Z0-9._-]+@[a-zA-Z0-9._-]+\\.[a-zA-Z0-9_-]+)");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+\\d{1,4} \\d{1,4} \\d{1,12}$");
    private static final Pattern UUID_PATTERN = Pattern.compile("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$");

    /**
     * Валидация данных при регистрации
     * Go: func signUpValidation(logger go_logger.Logger, db repository.AuthInterfaceDB,
     *                           u *models.User, s *models.Student, c *models.Company)
     *
     * ВАЖНО: Этот метод изменяет объекты (нормализует данные)
     *
     * @param user Пользователь (будет изменен)
     * @param company Компания (может быть null)
     * @throws ValidationException если валидация не прошла
     */
    public void validateSignUp(User user, Company company) {
        log.debug("Starting sign-up validation for login: {}", user.getLogin());

        // Нормализация данных (приведение к нижнему регистру)
        // Go: u.Login = strings.ToLower(u.Login)
        normalizeUserData(user);

        // Установка дефолтного типа аккаунта
        // Go: if u.AccountType == "" { u.AccountType = "individual" }
        if (user.getAccountType() == null) {
            user.setAccountType(User.AccountType.INDIVIDUAL);
            log.debug("Set default account type: INDIVIDUAL");
        }

        // Валидация общих данных пользователя
        validateUserCommonData(user);

        // Валидация по типам аккаунтов
        // Go: switch u.AccountType
        validateByAccountType(user, company);

        log.debug("Sign-up validation completed successfully");
    }

    /**
     * Валидация при обновлении пользователя
     * Go: часть метода EditUser в auth.go
     *
     * @param newUserData Новые данные (транзиентные поля из UserData)
     * @param oldUserData Старые данные из БД
     */
    public void validateUserUpdate(UserData newUserData, UserData oldUserData) {
        log.debug("Validating user update");

        // Проверка и валидация телефона
        if (isFieldChanged(newUserData.getPhone(), oldUserData.getPhone())) {
            validatePhone(newUserData.getPhone());

            if (!userDataRepository.isPhoneUnique(newUserData.getPhone())) {
                throw new ValidationException("Phone is already in use", "phone");
            }
        }

        // Проверка и валидация email
        if (isFieldChanged(newUserData.getEmail(), oldUserData.getEmail())) {
            validateEmail(newUserData.getEmail());

            if (!userDataRepository.isEmailUnique(newUserData.getEmail())) {
                throw new ValidationException("Email is already in use", "email");
            }
        }

        // Проверка и валидация логина
        if (isFieldChanged(newUserData.getLogin(), oldUserData.getLogin())) {
            validateLogin(newUserData.getLogin());

            if (userRepository.existsByLoginIgnoreCase(newUserData.getLogin())) {
                throw new ValidationException("Login is already in use", "login");
            }
        }

        log.debug("User update validation completed successfully");
    }

    /**
     * Проверка и хеширование паролей при смене
     * Go: func comparingPasswordHashes(logger go_logger.Logger,
     *                                  oldUserData, newUserData *models.User)
     *
     * ВАЖНО: В Go newUserData содержит:
     * - Password (string) - текущий пароль пользователя (открытый)
     * - NewPassword (string) - новый пароль (открытый)
     * Оба хешируются и сравниваются
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
        // Go: if newUserData.Password != oldUserData.Password
        if (!currentPasswordHash.equals(oldPasswordHashFromDb)) {
            throw new ValidationException(
                    "Wrong current password entered",
                    "password"
            );
        }

        // Хешируем новый пароль
        // Go: newUserData.NewPassword, err = generatePasswordHash(logger, newUserData.NewPassword)
        String newPasswordHash = passwordService.generatePasswordHash(newPasswordPlain);

        log.debug("Password change validation completed, new password hashed");
        return newPasswordHash;
    }

    /**
     * Проверка поля на уникальность
     * Go: func (a *AuthService) CheckFieldForUniqueness(...)
     *
     * @param field Имя поля (phone/email/login)
     * @param value Значение для проверки
     * @return true если уникальное
     */
    public boolean checkFieldUniqueness(String field, String value) {
        log.debug("Checking uniqueness for field: {}", field);

        // Валидация значения
        switch (field.toLowerCase()) {
            case "phone":
                validatePhone(value);
                return userDataRepository.isPhoneUnique(value);

            case "email":
                validateEmail(value);
                return userDataRepository.isEmailUnique(value);

            case "login":
                validateLogin(value);
                return !userRepository.existsByLoginIgnoreCase(value);

            default:
                throw new ValidationException(
                        "Unknown field type for uniqueness check: " + field,
                        field
                );
        }
    }

    // ==================== FIELD VALIDATORS ====================

    /**
     * Валидация логина
     * Go: func validationLogin(logger go_logger.Logger, login string)
     *
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
     * Go: func validationEmail(logger go_logger.Logger, email string)
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
     * Go: func validationPhone(logger go_logger.Logger, phone string)
     *
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
     * Go: func validationPassword(logger go_logger.Logger, password string)
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
     * Go: func validationUserName(logger go_logger.Logger, userName string)
     */
    public void validateUserName(String userName) {
        if (!StringUtils.hasText(userName)) {
            throw new ValidationException("UserName cannot be empty", "userName");
        }
    }

    /**
     * Валидация сообщения (для техподдержки)
     * Go: func validationMessage(logger go_logger.Logger, message string)
     */
    public void validateMessage(String message) {
        if (!StringUtils.hasText(message)) {
            throw new ValidationException("Message cannot be empty", "message");
        }

        // Go: if len(message) > 4096
        if (message.length() > 4096) {
            throw new ValidationException(
                    "Message is too long (max 4096 characters)",
                    "message"
            );
        }
    }

    // ==================== PRIVATE METHODS ====================

    /**
     * Нормализация данных пользователя
     * Go: приведение к нижнему регистру в начале signUpValidation
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
     * Go: func validationUser(logger, db, u)
     */
    private void validateUserCommonData(User user) {
        if (user == null) {
            throw new ValidationException("User cannot be null", "user");
        }

        // Валидация форматов полей
        validateLogin(user.getLogin());
        validateEmail(user.getEmail());
        validatePhone(user.getPhone());
        validateUserName(user.getUserName());

        // ВАЖНО: Пароль НЕ валидируется здесь!
        // Он должен быть УЖЕ захеширован до вызова валидации
        // В Go: u.Password, err = generatePasswordHash(logger, u.Password)
        // происходит ПЕРЕД вызовом signUpValidation

        if (!StringUtils.hasText(user.getPasswordHash())) {
            throw new ValidationException("Password hash cannot be empty", "password");
        }

        // Проверка уникальности
        // Go: uniqueLogin, _ := a.db.FieldValueUniqueness(...)
        if (userRepository.existsByLoginIgnoreCase(user.getLogin())) {
            throw new ValidationException("Login is already in use", "login");
        }

        if (!userDataRepository.isEmailUnique(user.getEmail())) {
            throw new ValidationException("Email is already in use", "email");
        }

        if (!userDataRepository.isPhoneUnique(user.getPhone())) {
            throw new ValidationException("Phone is already in use", "phone");
        }

        // Проверка типа аккаунта
        if (user.getAccountType() == null) {
            throw new ValidationException("Account type cannot be empty", "accountType");
        }
    }

    /**
     * Валидация по типам аккаунтов
     * Go: switch u.AccountType в signUpValidation
     */
    private void validateByAccountType(User user, Company company) {
        switch (user.getAccountType()) {
            case INDIVIDUAL:
                // Не требует дополнительных проверок
                log.debug("Account type: INDIVIDUAL - no additional validation needed");
                break;

            case ENTREPRENEUR:
                if (company == null) {
                    throw new ValidationException(
                            "Company data cannot be null for ENTREPRENEUR account type",
                            "company"
                    );
                }
                validateGeneralCompanyFields(company);
                validateNotEmpty(company.getTaxNumber(), "taxNumber", "INN cannot be empty");
                validateNotEmpty(company.getOgrnip(), "ogrnip", "OGRNIP cannot be empty");
                break;

            case COMPANY_RESIDENT:
                if (company == null) {
                    throw new ValidationException(
                            "Company data cannot be null for COMPANY_RESIDENT account type",
                            "company"
                    );
                }
                validateGeneralCompanyFields(company);
                validateNotEmpty(company.getTaxNumber(), "taxNumber", "INN cannot be empty");
                validateNotEmpty(company.getOgrn(), "ogrn", "OGRN cannot be empty");
                validateNotEmpty(company.getKpp(), "kpp", "KPP cannot be empty");
                break;

            case COMPANY_NON_RESIDENT:
                if (company == null) {
                    throw new ValidationException(
                            "Company data cannot be null for COMPANY_NON_RESIDENT account type",
                            "company"
                    );
                }
                validateGeneralCompanyFields(company);
                validateNotEmpty(company.getKio(), "kio", "KIO cannot be empty");
                validateNotEmpty(company.getOgrn(), "ogrn", "OGRN cannot be empty");
                validateNotEmpty(company.getKpp(), "kpp", "KPP cannot be empty");
                break;

            case EDUCATIONAL_UNIT:
                if (company == null) {
                    throw new ValidationException(
                            "Company data cannot be null for EDUCATIONAL_UNIT account type",
                            "company"
                    );
                }
                validateGeneralCompanyFields(company);
                validateNotEmpty(company.getTaxNumber(), "taxNumber", "INN cannot be empty");
                validateNotEmpty(company.getOgrn(), "ogrn", "OGRN cannot be empty");
                validateNotEmpty(company.getKpp(), "kpp", "KPP cannot be empty");
                validateNotEmpty(company.getLicenseNumber(), "licenseNumber",
                        "License number cannot be empty");
                validateNotEmpty(company.getLicenseIssueDate(), "licenseIssueDate",
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
     * Go: func validationGeneralFileldCompany(logger go_logger.Logger, c *models.Company)
     */
    private void validateGeneralCompanyFields(Company company) {
        validateNotEmpty(company.getFullTitle(), "fullTitle",
                "Full company name cannot be empty");

        if (!StringUtils.hasText(company.getCountry())) {
            company.setCountry("russia");
        }

        validateNotEmpty(company.getLegalAddress(), "legalAddress",
                "Legal address cannot be empty");
        validateNotEmpty(company.getPostAddress(), "postAddress",
                "Postal address cannot be empty");
        validateNotEmpty(company.getAccountNumber(), "accountNumber",
                "Current account cannot be empty");
        validateNotEmpty(company.getBankName(), "bankName",
                "Bank name cannot be empty");
        validateNotEmpty(company.getBic(), "bic",
                "BIC cannot be empty");
        validateNotEmpty(company.getTaxBank(), "taxBank",
                "Bank INN cannot be empty");
        validateNotEmpty(company.getKppBank(), "kppBank",
                "Bank KPP cannot be empty");
        validateNotEmpty(company.getCorrespondentAccount(), "correspondentAccount",
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

    // ==================== CUSTOM EXCEPTION ====================

    /**
     * Исключение валидации
     */
    public static class ValidationException extends RuntimeException {
        private final String fieldName;

        public ValidationException(String message, String fieldName) {
            super(message);
            this.fieldName = fieldName;
        }

        public String getFieldName() {
            return fieldName;
        }
    }
}