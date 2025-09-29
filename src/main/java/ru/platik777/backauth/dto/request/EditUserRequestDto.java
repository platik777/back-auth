package ru.platik777.backauth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO для редактирования данных пользователя
 * Соответствует структуре User из Go models.go
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EditUserRequestDto {

    /**
     * ID пользователя (из заголовка или токена)
     */
    private Integer userId;

    /**
     * Имя пользователя
     */
    @Size(max = 255, message = "User name must not exceed 255 characters")
    private String userName;

    /**
     * Email пользователя
     */
    @Email(message = "Invalid email format")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    private String email;

    /**
     * Номер телефона
     */
    @Size(max = 50, message = "Phone must not exceed 50 characters")
    private String phone;

    /**
     * Старый пароль (для проверки при смене пароля)
     */
    private String oldPassword;

    /**
     * Новый пароль (если меняется)
     */
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String newPassword;

    /**
     * Локаль пользователя (ru, en)
     */
    private String locale;
}