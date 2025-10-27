package ru.platik777.backauth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Ответ на проверку токена сброса пароля
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResetPasswordCheckResponse {
    private String userId;
}
