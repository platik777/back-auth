package ru.platik777.backauth.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.platik777.backauth.entity.types.ItemType;

import java.util.List;

/**
 * Запрос на обновление прав доступа пользователя
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePermissionRequest {
    /**
     * ID пользователя, чьи права обновляются
     */
    private String targetUserId;

    /**
     * ID элемента
     */
    private String itemId;

    /**
     * Тип элемента
     */
    private ItemType itemType;

    /**
     * Новые права (можно указать один из форматов)
     */

    /**
     * Вариант 1: Битовая маска (0-7)
     */
    private Short permissions;

    /**
     * Вариант 2: Список прав
     */
    private List<String> permissionsList;
}