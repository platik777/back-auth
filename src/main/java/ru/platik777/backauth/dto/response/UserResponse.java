package ru.platik777.backauth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.platik777.backauth.entity.User;
import ru.platik777.backauth.entity.embedded.UserSettings;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DTO пользователя для ответа
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private UUID id;
    private String login;
    private String email;
    private String userName;
    private String phone;
    private Integer billingId;
    private String accountType;
    private LocalDateTime createdAt;
    private UserSettings settings;
    private List<String> roles;
    private List<String> permissions;

    public static UserResponse fromUser(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .login(user.getLogin())
                .email(user.getEmail())
                .phone(user.getPhone())
                .accountType(user.getAccountType() != null ? user.getAccountType().getValue() : null)
                .createdAt(user.getCreatedAt() != null ? LocalDateTime.from(user.getCreatedAt()) : null)
                .settings(user.getSettings())
                .build();
    }
}
