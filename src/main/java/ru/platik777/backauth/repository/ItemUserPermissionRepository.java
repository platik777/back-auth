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
     * Получить все проекты, доступные пользователю на чтение (как минимум READ)
     * Используется нативный SQL для битовых операций
     */
    @Query(value = "SELECT * FROM item_user_permission p " +
            "WHERE p.user_id = :userId " +
            "AND p.project_id IS NOT NULL " +
            "AND (p.permissions & 1) = 1",
            nativeQuery = true)
    List<ItemUserPermission> findReadableProjectsByUserId(@Param("userId") UUID userId);

    /**
     * Получить все папки, доступные пользователю на чтение
     */
    @Query(value = "SELECT * FROM item_user_permission p " +
            "WHERE p.user_id = :userId " +
            "AND p.folder_id IS NOT NULL " +
            "AND (p.permissions & 1) = 1",
            nativeQuery = true)
    List<ItemUserPermission> findReadableFoldersByUserId(@Param("userId") UUID userId);

    /**
     * Получить все файлы, доступные пользователю на чтение
     */
    @Query(value = "SELECT * FROM item_user_permission p " +
            "WHERE p.user_id = :userId " +
            "AND p.file_id IS NOT NULL " +
            "AND (p.permissions & 1) = 1",
            nativeQuery = true)
    List<ItemUserPermission> findReadableFilesByUserId(@Param("userId") UUID userId);

    /**
     * Получить все блоки, доступные пользователю на чтение
     */
    @Query(value = "SELECT * FROM item_user_permission p " +
            "WHERE p.user_id = :userId " +
            "AND p.block_id IS NOT NULL " +
            "AND (p.permissions & 1) = 1",
            nativeQuery = true)
    List<ItemUserPermission> findReadableBlocksByUserId(@Param("userId") UUID userId);

    /**
     * Получить все элементы всех типов, доступные пользователю на чтение
     */
    @Query(value = "SELECT * FROM item_user_permission p " +
            "WHERE p.user_id = :userId " +
            "AND (p.permissions & 1) = 1",
            nativeQuery = true)
    List<ItemUserPermission> findAllReadableItemsByUserId(@Param("userId") UUID userId);

    /**
     * Найти права доступа пользователя к конкретному проекту
     */
    Optional<ItemUserPermission> findByUserIdAndProjectId(UUID userId, UUID projectId);

    /**
     * Найти права доступа пользователя к конкретной папке
     */
    Optional<ItemUserPermission> findByUserIdAndFolderId(UUID userId, UUID folderId);

    /**
     * Найти права доступа пользователя к конкретному файлу
     */
    Optional<ItemUserPermission> findByUserIdAndFileId(UUID userId, UUID fileId);

    /**
     * Найти права доступа пользователя к конкретному блоку
     */
    Optional<ItemUserPermission> findByUserIdAndBlockId(UUID userId, UUID blockId);

    /**
     * Проверить наличие конкретных прав доступа к проекту
     * @param requiredPermissions битовая маска требуемых прав (например, 3 для READ+WRITE)
     */
    @Query(value = "SELECT CASE WHEN COUNT(*) > 0 THEN true ELSE false END " +
            "FROM item_user_permission p " +
            "WHERE p.user_id = :userId " +
            "AND p.project_id = :projectId " +
            "AND (p.permissions & :requiredPermissions) = :requiredPermissions",
            nativeQuery = true)
    boolean hasProjectPermissions(@Param("userId") UUID userId,
                                  @Param("projectId") UUID projectId,
                                  @Param("requiredPermissions") short requiredPermissions);

    /**
     * Проверить наличие конкретных прав доступа к папке
     */
    @Query(value = "SELECT CASE WHEN COUNT(*) > 0 THEN true ELSE false END " +
            "FROM item_user_permission p " +
            "WHERE p.user_id = :userId " +
            "AND p.folder_id = :folderId " +
            "AND (p.permissions & :requiredPermissions) = :requiredPermissions",
            nativeQuery = true)
    boolean hasFolderPermissions(@Param("userId") UUID userId,
                                 @Param("folderId") UUID folderId,
                                 @Param("requiredPermissions") short requiredPermissions);

    /**
     * Проверить наличие конкретных прав доступа к файлу
     */
    @Query(value = "SELECT CASE WHEN COUNT(*) > 0 THEN true ELSE false END " +
            "FROM item_user_permission p " +
            "WHERE p.user_id = :userId " +
            "AND p.file_id = :fileId " +
            "AND (p.permissions & :requiredPermissions) = :requiredPermissions",
            nativeQuery = true)
    boolean hasFilePermissions(@Param("userId") UUID userId,
                               @Param("fileId") UUID fileId,
                               @Param("requiredPermissions") short requiredPermissions);

    /**
     * Проверить наличие конкретных прав доступа к блоку
     */
    @Query(value = "SELECT CASE WHEN COUNT(*) > 0 THEN true ELSE false END " +
            "FROM item_user_permission p " +
            "WHERE p.user_id = :userId " +
            "AND p.block_id = :blockId " +
            "AND (p.permissions & :requiredPermissions) = :requiredPermissions",
            nativeQuery = true)
    boolean hasBlockPermissions(@Param("userId") UUID userId,
                                @Param("blockId") UUID blockId,
                                @Param("requiredPermissions") short requiredPermissions);

    // ==================== ДОПОЛНИТЕЛЬНЫЕ МЕТОДЫ ====================

    /**
     * Найти все права доступа для пользователя
     */
    List<ItemUserPermission> findByUserId(UUID userId);

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
     * Найти все элементы (любого типа) пользователя в пределах тенанта
     */
    @Query(value = "SELECT * FROM item_user_permission p " +
            "WHERE p.user_id = :userId " +
            "AND p.tenant_id = :tenantId",
            nativeQuery = true)
    List<ItemUserPermission> findByUserIdAndTenantId(@Param("userId") UUID userId,
                                                     @Param("tenantId") String tenantId);

    /**
     * Подсчитать количество пользователей с доступом к проекту
     */
    long countByProjectId(UUID projectId);

    /**
     * Подсчитать количество проектов, к которым у пользователя есть доступ
     */
    @Query(value = "SELECT COUNT(*) FROM item_user_permission p " +
            "WHERE p.user_id = :userId " +
            "AND p.project_id IS NOT NULL",
            nativeQuery = true)
    long countUserProjects(@Param("userId") UUID userId);
}