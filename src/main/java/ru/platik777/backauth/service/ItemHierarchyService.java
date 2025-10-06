package ru.platik777.backauth.service;

import ru.platik777.backauth.entity.types.ItemType;
import java.util.List;
import java.util.UUID;

public interface ItemHierarchyService {

    /**
     * Получить всех потомков элемента (рекурсивно)
     */
    List<UUID> getAllDescendants(UUID itemId, ItemType itemType, UUID tenantId);

    /**
     * Получить потомков определенного типа
     */
    List<UUID> getDescendantsOfType(UUID itemId, ItemType itemType,
                                    ItemType targetType, UUID tenantId);

    /**
     * Получить всех родителей элемента до корня
     */
    List<ParentInfo> getAllParents(UUID itemId, ItemType itemType);

    /**
     * Информация о родителе
     */
    record ParentInfo(UUID id, ItemType type) {}
}