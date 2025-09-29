package ru.platik777.backauth.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO для проверки авторизации пользователя
 * Используется в isAuthorization и isBaseAuthorization
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CheckAuthorizationRequestDto {

    /**
     * Access токен для проверки
     */
    @NotBlank(message = "Access token cannot be empty")
    private String accessToken;
}