package ru.platik777.backauth.dto.request;

import lombok.Data;

/**
 * Запрос на проверку доступа к проекту
 */
@Data
public class CheckProjectRequest {
    private Integer userId;
    private Integer projectId;
}