package ru.platik777.backauth.mapper;

import org.springframework.stereotype.Component;
import ru.platik777.backauth.dto.request.SignUpRequest;
import ru.platik777.backauth.entity.User;
import ru.platik777.backauth.entity.types.AccountType;

/**
 * Маппер для преобразования DTO в User Entity и обратно
 */
@Component
public class UserMapper {

    /**
     * Маппинг UserDto в User Entity
     */
    public User toEntity(SignUpRequest.UserDto dto) {
        if (dto == null) {
            throw new IllegalArgumentException("User data cannot be null");
        }

        User user = new User();
        user.setLogin(dto.getLogin());
        user.setName(dto.getName());
        user.setPasswordHash(dto.getPassword());
        user.setEmail(dto.getEmail());
        user.setPhone(dto.getPhone());

        if (dto.getAccountType() != null) {
            user.setAccountType(AccountType.valueOf(dto.getAccountType()));
        }

        return user;
    }
}