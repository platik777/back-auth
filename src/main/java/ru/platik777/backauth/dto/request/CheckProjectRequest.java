package ru.platik777.backauth.dto.request;

import lombok.Data;

import java.util.UUID;

/**
 * Запрос на проверку доступа к проекту
 */
@Data
public class CheckProjectRequest {
    private UUID userId;
    private Integer projectId;
}