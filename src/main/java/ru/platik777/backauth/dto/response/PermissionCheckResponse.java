package ru.platik777.backauth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO для ответа на проверку прав доступа
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PermissionCheckResponse {
    /**
     * ID пользователя
     */
    private String userId;

    /**
     * ID элемента
     */
    private String itemId;

    /**
     * Тип элемента
     */
    private String itemType;

    /**
     * Запрошенные права (битовая маска)
     */
    private Integer requiredPermissions;

    /**
     * Результат проверки
     */
    private Boolean hasPermission;

    /**
     * Описание причины отказа (если нет доступа)
     */
    private String message;
}