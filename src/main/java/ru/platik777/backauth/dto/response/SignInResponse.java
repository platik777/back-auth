package ru.platik777.backauth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Ответ на вход
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SignInResponse {
    private String accessToken;
    private String refreshToken;
    private String accessBaseToken;
    private String refreshBaseToken;
    private UserResponse user;
}
