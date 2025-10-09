package ru.platik777.backauth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.platik777.backauth.entity.User;

import java.util.UUID;

/**
 * Базовый ответ пользователя (только основные поля)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BaseUserResponse {
    private UUID id;
    private String login;
    private String email;
    private String userName;

    public static BaseUserResponse fromUser(User user) {
        return BaseUserResponse.builder()
                .id(user.getId())
                .login(user.getLogin())
                .email(user.getEmail())
                .build();
    }
}
