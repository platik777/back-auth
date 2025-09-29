package ru.platik777.backauth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO для ответа проверки принадлежности проекта
 * Соответствует ответу CheckProjectId из Go
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckProjectResponseDto {

    /**
     * Принадлежит ли проект пользователю
     */
    private Boolean belongs;

    /**
     * ID пользователя
     */
    private Integer userId;

    /**
     * ID проекта
     */
    private Integer projectId;

    /**
     * Название проекта (если найден)
     */
    private String projectName;
}