package ru.platik777.backauth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.platik777.backauth.entity.ApiKey;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO API ключа для ответа
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiKeyResponse {
    private UUID id;
    private String apiKey;
    private String name;
    private LocalDateTime expireAt;
    private LocalDateTime createdAt;
    private Boolean isDeleted;

    public static ApiKeyResponse fromApiKey(ApiKey apiKey) {
        return ApiKeyResponse.builder()
                .id(apiKey.getId())
                .apiKey(apiKey.getApiKey())
                .name(apiKey.getName())
                .expireAt(LocalDateTime.from(apiKey.getExpireAt()))
                .createdAt(LocalDateTime.from(apiKey.getCreatedAt()))
                .isDeleted(apiKey.getIsDeleted())
                .build();
    }
}
