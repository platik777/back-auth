package ru.platik777.backauth.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO для проверки принадлежности проекта пользователю
 * Соответствует логике CheckProjectId из Go
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CheckProjectIdRequestDto {

    /**
     * ID пользователя
     */
    @NotNull(message = "User ID cannot be null")
    private Integer userId;

    /**
     * ID проекта для проверки
     */
    @NotNull(message = "Project ID cannot be null")
    private Integer projectId;
}