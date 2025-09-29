package ru.platik777.backauth.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO для обновления токена доступа
 * Используется для всех refresh endpoints
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RefreshTokenRequestDto {

    /**
     * Refresh токен для получения нового Access токена
     */
    @NotBlank(message = "Refresh token cannot be empty")
    private String refreshToken;
}