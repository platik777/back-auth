package ru.platik777.backauth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Ответ на проверку API ключа
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiKeyAuthResponse {
    private String userId;
}
