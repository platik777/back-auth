package ru.platik777.backauth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO для запроса сброса пароля
 * Соответствует ResetPasswordForgot из Go
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResetPasswordForgotRequestDto {

    /**
     * Email пользователя для отправки письма со сбросом пароля
     */
    @NotBlank(message = "Email cannot be empty")
    @Email(message = "Invalid email format")
    private String email;

    /**
     * Локаль для письма (ru, en)
     */
    private String locale;
}