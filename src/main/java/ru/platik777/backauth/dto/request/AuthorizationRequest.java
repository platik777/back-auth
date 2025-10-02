package ru.platik777.backauth.dto.request;

import lombok.Data;

/**
 * Запрос на проверку авторизации
 */
@Data
public class AuthorizationRequest {
    private String token;
}