package ru.platik777.backauth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Ответ на проверку авторизации
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthorizationResponse {
    private String status;
    private UUID userId;
}
