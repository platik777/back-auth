package ru.platik777.backauth.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.platik777.backauth.entity.types.ItemType;

import java.util.UUID;

/**
 * Запрос на проверку прав доступа к элементу
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckItemPermissionRequest {
    /**
     * ID элемента для проверки
     */
    private UUID itemId;

    /**
     * Тип элемента (PROJECT, FOLDER, FILE, BLOCK)
     */
    private ItemType itemType;

    /**
     * Требуемые права доступа (битовая маска 0-7)
     *
     * Значения:
     * - 0: нет доступа
     * - 1: READ (001)
     * - 3: READ + WRITE (011)
     * - 5: READ + EXECUTE (101)
     * - 7: READ + WRITE + EXECUTE (111)
     */
    private Integer requiredPermissions;
}