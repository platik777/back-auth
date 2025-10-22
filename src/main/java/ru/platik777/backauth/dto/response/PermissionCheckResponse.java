package ru.platik777.backauth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

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
    private UUID userId;

    /**
     * ID элемента
     */
    private UUID itemId;

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