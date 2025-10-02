package ru.platik777.backauth.dto.request;

import lombok.Data;

/**
 * Запрос на проверку API ключа
 */
@Data
public class ApiKeyCheckRequest {
    private String token;
}