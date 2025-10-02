package ru.platik777.backauth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Ответ на сброс пароля (forgot)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResetPasswordForgotResponse {
    private Boolean status;
    private String message;
}
