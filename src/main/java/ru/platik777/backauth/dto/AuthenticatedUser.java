package ru.platik777.backauth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.UUID;

/**
 * Класс для хранения данных аутентифицированного пользователя
 */
@Data
@AllArgsConstructor
public class AuthenticatedUser {
    private String userId;
    private String tenantId;
}
