package ru.platik777.backauth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Список API ключей
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiKeysResponse {
    private List<ApiKeyResponse> apiKeys;
}
