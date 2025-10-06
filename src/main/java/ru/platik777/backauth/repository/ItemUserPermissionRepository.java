package ru.platik777.backauth.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.platik777.backauth.entity.ItemUserPermission;
import ru.platik777.backauth.entity.keys.ItemUserPermissionId;
import ru.platik777.backauth.entity.types.ItemType;

import java.util.List;
import java.util.UUID;

@Repository
public interface ItemUserPermissionRepository extends JpaRepository<ItemUserPermission, ItemUserPermissionId> {

    // Найти все права пользователя в тенанте
    List<ItemUserPermission> findByUserIdAndTenantId(UUID userId, UUID tenantId);

    // Найти конкретное право
    List<ItemUserPermission> findByIdItemIdAndIdItemTypeAndUserId(
            UUID itemId, ItemType itemType, UUID userId
    );

    // Проверить, есть ли право на конкретный элемент
    boolean existsByIdItemIdAndIdItemTypeAndUserIdAndTenantId(
            UUID itemId, ItemType itemType, UUID userId, UUID tenantId
    );

    // Удалить права на конкретный элемент для пользователя
    @Modifying
    @Query("DELETE FROM ItemUserPermission p WHERE p.id.itemId = :itemId " +
            "AND p.id.itemType = :itemType AND p.userId = :userId AND p.tenantId = :tenantId")
    void deleteByItemAndUser(
            @Param("itemId") UUID itemId,
            @Param("itemType") ItemType itemType,
            @Param("userId") UUID userId,
            @Param("tenantId") UUID tenantId
    );

    // Удалить все дочерние права (когда выдается право на родителя)
    @Modifying
    @Query("DELETE FROM ItemUserPermission p WHERE p.id.itemId IN :childIds " +
            "AND p.userId = :userId AND p.tenantId = :tenantId")
    void deleteChildPermissions(
            @Param("childIds") List<UUID> childIds,
            @Param("userId") UUID userId,
            @Param("tenantId") UUID tenantId
    );

    // Найти все права пользователя определенного типа
    List<ItemUserPermission> findByIdItemTypeAndUserIdAndTenantId(
            ItemType itemType, UUID userId, UUID tenantId
    );
}