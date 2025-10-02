package ru.platik777.backauth.dto.request;

import lombok.Data;

/**
 * Запрос на проверку уникальности поля
 */
@Data
public class CheckFieldUniquenessRequest {
    private String field;  // phone, email, login
    private String value;
}