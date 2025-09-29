package ru.platik777.backauth.mapper;

import ru.platik777.backauth.dto.UserDto;
import ru.platik777.backauth.dto.request.SignUpRequestDto;
import ru.platik777.backauth.entity.User;
import ru.platik777.backauth.entity.UserData;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

@Component
public class UserMapper {

    public UserDto toDto(User user) {
        UserDto dto = new UserDto(user);

        // Добавляем роли и разрешения (будут заполнены в сервисе)
        dto.setRoles(new ArrayList<>());
        dto.setPermissions(new ArrayList<>());

        return dto;
    }

    public User toEntity(SignUpRequestDto requestDto) {
        User user = new User();
        user.setLogin(requestDto.getLogin().toLowerCase());
        user.setAccountType(User.AccountType.fromValue(requestDto.getAccountType()));
        user.setBillingId(0); // По умолчанию 0

        // Настройки пользователя
        User.UserSettings settings = new User.UserSettings();
        settings.setLocale("ru"); // По умолчанию русский
        user.setSettings(settings);

        return user;
    }

    public UserData toUserData(SignUpRequestDto requestDto, User user, String passwordHash) {
        UserData userData = new UserData();
        userData.setUser(user);
        userData.setLogin(requestDto.getLogin().toLowerCase());
        userData.setPasswordHash(passwordHash);
        userData.setEmail(requestDto.getEmail().toLowerCase());
        userData.setUserName(requestDto.getUserName());
        userData.setPhone(requestDto.getPhone().toLowerCase());

        return userData;
    }
}