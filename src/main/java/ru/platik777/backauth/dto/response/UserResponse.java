package ru.platik777.backauth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.platik777.backauth.entity.User;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO пользователя для ответа
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private Integer id;
    private String login;
    private String email;
    private String userName;
    private String phone;
    private Integer billingId;
    private String accountType;
    private LocalDateTime createdAt;
    private User.UserSettings settings;
    private List<String> roles;
    private List<String> permissions;

    public static UserResponse fromUser(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .login(user.getLogin())
                .email(user.getEmail())
                .userName(user.getUserName())
                .phone(user.getPhone())
                .billingId(user.getBillingId())
                .accountType(user.getAccountType() != null ? user.getAccountType().getValue() : null)
                .createdAt(user.getCreatedAt())
                .settings(user.getSettings())
                .roles(user.getRoles())
                .permissions(user.getPermissions())
                .build();
    }
}
