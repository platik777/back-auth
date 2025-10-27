package ru.platik777.backauth.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.platik777.backauth.entity.types.ItemType;

import java.util.List;

/**
 * Запрос на выдачу прав доступа пользователю
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GrantPermissionRequest {
    /**
     * ID пользователя, которому выдаются права
     */
    private String targetUserId;

    /**
     * ID элемента (project/folder/file/block)
     */
    private String itemId;

    /**
     * Тип элемента
     */
    private ItemType itemType;

    /**
     * Права для выдачи (можно указать один из форматов)
     */

    /**
     * Вариант 1: Битовая маска (0-7)
     * Например: 3 для READ+WRITE
     */
    private Short permissions;

    /**
     * Вариант 2: Список прав (альтернатива битовой маске)
     * Например: ["READ", "WRITE"]
     */
    private List<String> permissionsList;
}