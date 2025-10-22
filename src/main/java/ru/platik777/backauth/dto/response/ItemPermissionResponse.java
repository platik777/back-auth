package ru.platik777.backauth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.platik777.backauth.entity.ItemUserPermission;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
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
    private UUID itemId;

    /**
     * Тип элемента
     */
    private String itemType;

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

    /**
     * Конвертация из сущности ItemUserPermission
     */
    public static ItemPermissionResponse fromEntity(ItemUserPermission permission) {
        if (permission == null) {
            return null;
        }

        return ItemPermissionResponse.builder()
                .itemId(permission.getItemId())
                .itemType(permission.getItemType())
                .permissions(permission.getPermissions())
                .permissionsList(permission.getPermissionsList())
                .createdAt(permission.getCreatedAt() != null ?
                        LocalDateTime.from(permission.getCreatedAt()) : null)
                .tenantId(permission.getTenantId())
                .build();
    }

    /**
     * Конвертация списка сущностей
     */
    public static List<ItemPermissionResponse> fromEntities(List<ItemUserPermission> permissions) {
        if (permissions == null) {
            return List.of();
        }

        return permissions.stream()
                .map(ItemPermissionResponse::fromEntity)
                .collect(Collectors.toList());
    }
}