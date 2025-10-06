package ru.platik777.backauth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.platik777.backauth.entity.ItemUserPermission;
import ru.platik777.backauth.entity.keys.ItemUserPermissionId;
import ru.platik777.backauth.entity.types.ItemType;
import ru.platik777.backauth.entity.types.Permission;
import ru.platik777.backauth.repository.ItemUserPermissionRepository;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ItemUserPermissionService {

    private final ItemUserPermissionRepository permissionRepository;
    private final ItemHierarchyService hierarchyService;

    /**
     * Выдать права пользователю с учетом мерджинга
     */
    @Transactional
    public void grantPermission(UUID itemId, ItemType itemType, UUID userId,
                                UUID tenantId, int permissions) {

        validatePermissions(permissions);

        log.info("Granting permissions {} to user {} for item {} of type {}",
                permissionsToString(permissions), userId, itemId, itemType);

        // 1. Получить существующие права пользователя
        List<ItemUserPermission> existingPermissions =
                permissionRepository.findByUserIdAndTenantId(userId, tenantId);

        // 2. Проверить, есть ли право на родительский элемент с такими же или большими правами
        Optional<ItemUserPermission> parentPermission =
                findParentPermissionWithSameOrGreaterRights(itemId, itemType,
                        existingPermissions, permissions);

        if (parentPermission.isPresent()) {
            log.info("User already has equal or greater permission through parent item {}, skipping",
                    parentPermission.get().getId().getItemId());
            return;
        }

        // 3. Найти всех потомков этого элемента
        List<UUID> descendantIds = hierarchyService.getAllDescendants(itemId, itemType, tenantId);

        // 4. Удалить права на всех потомков (мерджинг вверх)
        if (!descendantIds.isEmpty()) {
            log.info("Removing {} child permissions due to parent permission grant",
                    descendantIds.size());
            permissionRepository.deleteChildPermissions(descendantIds, userId, tenantId);
        }

        // 5. Проверить, существует ли уже право на этот элемент
        Optional<ItemUserPermission> existing = existingPermissions.stream()
                .filter(p -> p.getId().getItemId().equals(itemId)
                        && p.getId().getItemType() == itemType)
                .findFirst();

        if (existing.isPresent()) {
            // Обновить существующие права (объединить с новыми)
            ItemUserPermission existingPerm = existing.get();
            int newPermissions = existingPerm.getPermissions() | permissions;
            existingPerm.setPermissions(newPermissions);
            permissionRepository.save(existingPerm);
            log.info("Updated existing permission to {}", permissionsToString(newPermissions));
        } else {
            // Создать новое право
            ItemUserPermission permission = new ItemUserPermission();
            permission.setId(new ItemUserPermissionId(itemId, itemType));
            permission.setUserId(userId);
            permission.setTenantId(tenantId);
            permission.setPermissions(permissions);

            permissionRepository.save(permission);
            log.info("Permission granted successfully");
        }
    }

    /**
     * Проверить, имеет ли пользователь определенные права к элементу
     */
    public boolean hasPermission(UUID itemId, ItemType itemType, UUID userId,
                                 UUID tenantId, Permission... requiredPermissions) {

        int requiredMask = Permission.combine(requiredPermissions);
        return hasPermissionMask(itemId, itemType, userId, tenantId, requiredMask);
    }

    /**
     * Проверить права по битовой маске
     */
    public boolean hasPermissionMask(UUID itemId, ItemType itemType, UUID userId,
                                     UUID tenantId, int requiredMask) {

        List<ItemUserPermission> userPermissions =
                permissionRepository.findByUserIdAndTenantId(userId, tenantId);

        // Проверить прямой доступ
        Optional<ItemUserPermission> directPermission = userPermissions.stream()
                .filter(p -> p.getId().getItemId().equals(itemId)
                        && p.getId().getItemType() == itemType)
                .findFirst();

        if (directPermission.isPresent()) {
            return Permission.hasAllPermissions(directPermission.get().getPermissions(), requiredMask);
        }

        // Проверить доступ через родителей
        return hasPermissionThroughParent(itemId, itemType, userPermissions, requiredMask);
    }

    /**
     * Получить права пользователя на элемент (прямые или через родителя)
     */
    public Optional<Integer> getUserPermissions(UUID itemId, ItemType itemType,
                                                UUID userId, UUID tenantId) {

        List<ItemUserPermission> userPermissions =
                permissionRepository.findByUserIdAndTenantId(userId, tenantId);

        // Проверить прямые права
        Optional<ItemUserPermission> directPermission = userPermissions.stream()
                .filter(p -> p.getId().getItemId().equals(itemId)
                        && p.getId().getItemType() == itemType)
                .findFirst();

        if (directPermission.isPresent()) {
            return Optional.of(directPermission.get().getPermissions());
        }

        // Проверить права через родителя
        List<ItemHierarchyService.ParentInfo> parents =
                hierarchyService.getAllParents(itemId, itemType);

        return userPermissions.stream()
                .filter(p -> parents.stream()
                        .anyMatch(parent -> parent.id().equals(p.getId().getItemId())
                                && parent.type() == p.getId().getItemType()))
                .map(ItemUserPermission::getPermissions)
                .max(Integer::compareTo); // Вернуть максимальные права из всех родителей
    }

    /**
     * Добавить конкретные права к существующим
     */
    @Transactional
    public void addPermissions(UUID itemId, ItemType itemType, UUID userId,
                               UUID tenantId, Permission... permissions) {

        int permissionsToAdd = Permission.combine(permissions);
        grantPermission(itemId, itemType, userId, tenantId, permissionsToAdd);
    }

    /**
     * Удалить конкретные права (оставить остальные)
     */
    @Transactional
    public void removePermissions(UUID itemId, ItemType itemType, UUID userId,
                                  UUID tenantId, Permission... permissions) {

        Optional<ItemUserPermission> existing = permissionRepository
                .findByIdItemIdAndIdItemTypeAndUserId(itemId, itemType, userId)
                .stream()
                .filter(p -> p.getTenantId().equals(tenantId))
                .findFirst();

        if (existing.isEmpty()) {
            log.warn("No permission found to remove for user {} on item {}", userId, itemId);
            return;
        }

        ItemUserPermission permission = existing.get();
        int permissionsToRemove = Permission.combine(permissions);
        int newPermissions = permission.getPermissions() & ~permissionsToRemove;

        if (newPermissions == 0) {
            // Если не осталось прав, удалить запись
            permissionRepository.delete(permission);
            log.info("All permissions removed, deleting permission record");
        } else {
            // Обновить права
            permission.setPermissions(newPermissions);
            permissionRepository.save(permission);
            log.info("Permissions updated from {} to {}",
                    permissionsToString(permission.getPermissions()),
                    permissionsToString(newPermissions));
        }
    }

    /**
     * Отозвать все права
     */
    @Transactional
    public void revokeAllPermissions(UUID itemId, ItemType itemType,
                                     UUID userId, UUID tenantId) {

        log.info("Revoking all permissions from user {} for item {} of type {}",
                userId, itemId, itemType);

        permissionRepository.deleteByItemAndUser(itemId, itemType, userId, tenantId);
    }

    /**
     * Получить все элементы определенного типа, к которым есть доступ
     */
    public Set<UUID> getAccessibleItemIds(UUID userId, UUID tenantId,
                                          ItemType itemType, Permission... requiredPermissions) {

        int requiredMask = Permission.combine(requiredPermissions);
        List<ItemUserPermission> userPermissions =
                permissionRepository.findByUserIdAndTenantId(userId, tenantId);

        Set<UUID> accessibleIds = new HashSet<>();

        // Добавить элементы с прямым доступом нужного типа
        userPermissions.stream()
                .filter(p -> p.getId().getItemType() == itemType)
                .filter(p -> Permission.hasAllPermissions(p.getPermissions(), requiredMask))
                .forEach(p -> accessibleIds.add(p.getId().getItemId()));

        // Добавить потомков элементов с доступом
        for (ItemUserPermission permission : userPermissions) {
            if (Permission.hasAllPermissions(permission.getPermissions(), requiredMask)) {
                List<UUID> descendants = hierarchyService.getDescendantsOfType(
                        permission.getId().getItemId(),
                        permission.getId().getItemType(),
                        itemType,
                        tenantId
                );
                accessibleIds.addAll(descendants);
            }
        }

        return accessibleIds;
    }

    // === Вспомогательные методы ===

    private Optional<ItemUserPermission> findParentPermissionWithSameOrGreaterRights(
            UUID itemId, ItemType itemType,
            List<ItemUserPermission> existingPermissions,
            int requiredPermissions) {

        List<ItemHierarchyService.ParentInfo> parents =
                hierarchyService.getAllParents(itemId, itemType);

        return existingPermissions.stream()
                .filter(p -> parents.stream()
                        .anyMatch(parent -> parent.id().equals(p.getId().getItemId())
                                && parent.type() == p.getId().getItemType()))
                .filter(p -> (p.getPermissions() & requiredPermissions) == requiredPermissions)
                .findFirst();
    }

    private boolean hasPermissionThroughParent(UUID itemId, ItemType itemType,
                                               List<ItemUserPermission> userPermissions,
                                               int requiredMask) {

        List<ItemHierarchyService.ParentInfo> parents =
                hierarchyService.getAllParents(itemId, itemType);

        return userPermissions.stream()
                .filter(p -> parents.stream()
                        .anyMatch(parent -> parent.id().equals(p.getId().getItemId())
                                && parent.type() == p.getId().getItemType()))
                .anyMatch(p -> Permission.hasAllPermissions(p.getPermissions(), requiredMask));
    }

    private void validatePermissions(int permissions) {
        if (permissions < 0 || permissions > 7) {
            throw new IllegalArgumentException(
                    "Permissions must be between 0 and 7, got: " + permissions);
        }
    }

    private String permissionsToString(int permissions) {
        StringBuilder sb = new StringBuilder();
        if (Permission.hasPermission(permissions, Permission.READ)) sb.append("R");
        if (Permission.hasPermission(permissions, Permission.WRITE)) sb.append("W");
        if (Permission.hasPermission(permissions, Permission.EXECUTE)) sb.append("X");
        return sb.toString() + " (" + permissions + ")";
    }
}