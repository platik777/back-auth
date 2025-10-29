package ru.platik777.backauth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.platik777.backauth.entity.ItemUserPermission;
import ru.platik777.backauth.entity.types.ItemType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * DTO для ответа с правами доступа к элементу
 *
 * ГИБРИДНЫЙ ПОДХОД:
 * - permissions: битовая маска (0-7) для совместимости
 * - permissionsList: массив строк ["READ", "WRITE"] для читаемости
 * - permissionsDescription: описание "READ + WRITE + EXECUTE"
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemPermissionResponse {
    /**
     * ID элемента
     */
    private String itemId;

    /**
     * Тип элемента
     */
    private ItemType itemType;

    /**
     * Битовая маска прав доступа (0-7)
     * Для совместимости и производительности
     */
    private Short permissions;

    /**
     * Список прав в читаемом виде
     * ["READ", "WRITE", "EXECUTE"]
     */
    private List<String> permissionsList;

    /**
     * Дата создания доступа
     */
    private LocalDateTime createdAt;

    /**
     * Tenant ID
     */
    private String tenantId;
}