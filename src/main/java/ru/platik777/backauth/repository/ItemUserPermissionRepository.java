package ru.platik777.backauth.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.platik777.backauth.entity.ItemUserPermission;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ItemUserPermissionRepository extends JpaRepository<ItemUserPermission, UUID> {

    /**
     * Найти все права доступа для пользователя
     */
    List<ItemUserPermission> findByUserId(UUID userId);

    /**
     * Найти права доступа пользователя к проекту
     */
    Optional<ItemUserPermission> findByUserIdAndProjectId(UUID userId, UUID projectId);

    /**
     * Найти права доступа пользователя к папке
     */
    Optional<ItemUserPermission> findByUserIdAndFolderId(UUID userId, UUID folderId);

    /**
     * Найти права доступа пользователя к файлу
     */
    Optional<ItemUserPermission> findByUserIdAndFileId(UUID userId, UUID fileId);

    /**
     * Найти права доступа пользователя к блоку
     */
    Optional<ItemUserPermission> findByUserIdAndBlockId(UUID userId, UUID blockId);

    /**
     * Найти все права доступа к проекту
     */
    List<ItemUserPermission> findByProjectId(UUID projectId);

    /**
     * Найти все права доступа к папке
     */
    List<ItemUserPermission> findByFolderId(UUID folderId);

    /**
     * Найти все права доступа к файлу
     */
    List<ItemUserPermission> findByFileId(UUID fileId);

    /**
     * Найти все права доступа к блоку
     */
    List<ItemUserPermission> findByBlockId(UUID blockId);

    /**
     * Проверить существование прав доступа пользователя к проекту
     */
    boolean existsByUserIdAndProjectId(UUID userId, UUID projectId);

    /**
     * Проверить существование прав доступа пользователя к папке
     */
    boolean existsByUserIdAndFolderId(UUID userId, UUID folderId);

    /**
     * Проверить существование прав доступа пользователя к файлу
     */
    boolean existsByUserIdAndFileId(UUID userId, UUID fileId);

    /**
     * Проверить существование прав доступа пользователя к блоку
     */
    boolean existsByUserIdAndBlockId(UUID userId, UUID blockId);

    /**
     * Удалить все права доступа пользователя
     */
    void deleteByUserId(UUID userId);

    /**
     * Удалить все права доступа к проекту
     */
    void deleteByProjectId(UUID projectId);

    /**
     * Удалить все права доступа к папке
     */
    void deleteByFolderId(UUID folderId);

    /**
     * Удалить все права доступа к файлу
     */
    void deleteByFileId(UUID fileId);

    /**
     * Удалить все права доступа к блоку
     */
    void deleteByBlockId(UUID blockId);

    /**
     * Найти все элементы (любого типа) пользователя в пределах тенанта
     */
    @Query("SELECT p FROM ItemUserPermission p WHERE p.user.id = :userId AND p.tenantId = :tenantId")
    List<ItemUserPermission> findByUserIdAndTenantId(@Param("userId") UUID userId, @Param("tenantId") String tenantId);

    /**
     * Подсчитать количество пользователей с доступом к проекту
     */
    long countByProjectId(UUID projectId);

    /**
     * Подсчитать количество проектов, к которым у пользователя есть доступ
     */
    @Query("SELECT COUNT(p) FROM ItemUserPermission p WHERE p.user.id = :userId AND p.project IS NOT NULL")
    long countUserProjects(@Param("userId") UUID userId);
}