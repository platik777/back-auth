package ru.platik777.backauth.service;

import ru.platik777.backauth.entity.User;
import ru.platik777.backauth.entity.UserData;
import ru.platik777.backauth.repository.UserDataRepository;
import ru.platik777.backauth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Сервис для проверки уникальности полей пользователя
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UniquenessService {

    private final UserRepository userRepository;
    private final UserDataRepository userDataRepository;

    /**
     * Проверка уникальности поля
     * @param field Поле для проверки (login, email, phone)
     * @param value Значение поля
     * @return true если поле уникально, false если уже существует
     */
    public boolean isFieldUnique(String field, String value) {
        if (value == null || value.trim().isEmpty()) {
            return false;
        }

        String normalizedValue = value.toLowerCase().trim();

        return switch (field.toLowerCase()) {
            case "login" -> isLoginUnique(normalizedValue);
            case "email" -> isEmailUnique(normalizedValue);
            case "phone" -> isPhoneUnique(normalizedValue);
            default -> throw new IllegalArgumentException("Unknown field type: " + field);
        };
    }

    /**
     * Проверка уникальности логина
     */
    private boolean isLoginUnique(String login) {
        return !userRepository.existsByLoginIgnoreCase(login);
    }

    /**
     * Проверка уникальности email
     */
    private boolean isEmailUnique(String email) {
        return !userDataRepository.existsByEmailIgnoreCase(email);
    }

    /**
     * Проверка уникальности телефона
     */
    private boolean isPhoneUnique(String phone) {
        return !userDataRepository.existsByPhoneIgnoreCase(phone);
    }

    /**
     * Поиск пользователя по email
     */
    public Integer findUserIdByEmail(String email) {
        return userDataRepository.findByEmailIgnoreCase(email.toLowerCase())
                .map(UserData::getUserId)
                .orElse(null);
    }

    /**
     * Поиск пользователя по телефону
     */
    public Integer findUserIdByPhone(String phone) {
        return userDataRepository.findByPhoneIgnoreCase(phone.toLowerCase())
                .map(UserData::getUserId)
                .orElse(null);
    }

    /**
     * Поиск пользователя по логину
     */
    public Integer findUserIdByLogin(String login) {
        return userRepository.findByLoginIgnoreCase(login.toLowerCase())
                .map(User::getId)
                .orElse(null);
    }

    /**
     * Получение всех ID пользователей (для совместимости с Go версией)
     */
    public List<Integer> getAllUserIds() {
        return userRepository.findAllUserIds();
    }
}