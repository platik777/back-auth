package ru.platik777.backauth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO для обновления пароля после сброса
 * Соответствует ResetPasswordUpdate из Go
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResetPasswordUpdateRequestDto {

    /**
     * Токен для сброса пароля (из email)
     */
    @NotBlank(message = "Token cannot be empty")
    private String token;

    /**
     * Новый пароль
     */
    @NotBlank(message = "Password cannot be empty")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;
}