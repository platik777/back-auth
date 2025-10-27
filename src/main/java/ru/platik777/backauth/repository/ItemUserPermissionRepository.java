package ru.platik777.backauth.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.platik777.backauth.entity.ItemUserPermission;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Репозиторий для работы с правами доступа пользователей
 *
 * ОПТИМИЗАЦИЯ С GIN ИНДЕКСАМИ:
 * Использует оператор = ANY() для прямой работы с массивами all_parant_ids
 * Это эффективно работает с GIN индексами без необходимости unnest()
 * Все проверки прав используют OR вместо UNION для лучшей производительности
 *
 * ТРЕБУЕМЫЕ ИНДЕКСЫ:
 * CREATE INDEX idx_projects_all_parant_ids ON projects USING GIN(all_parant_ids);
 * CREATE INDEX idx_folders_all_parant_ids ON folders USING GIN(all_parant_ids);
 * CREATE INDEX idx_files_all_parant_ids ON files USING GIN(all_parant_ids);
 * CREATE INDEX idx_blocks_all_parant_ids ON blocks USING GIN(all_parant_ids);
 * CREATE INDEX idx_folders_rank ON folders(rank);
 */
@Repository
public interface ItemUserPermissionRepository extends JpaRepository<ItemUserPermission, String> {

    Optional<ItemUserPermission> findByUserIdAndProjectId(String userId, String projectId);

    Optional<ItemUserPermission> findByUserIdAndFolderId(String userId, String folderId);

    Optional<ItemUserPermission> findByUserIdAndFileId(String userId, String fileId);

    Optional<ItemUserPermission> findByUserIdAndBlockId(String userId, String blockId);

    // ==================== БАЗОВЫЕ МЕТОДЫ ДЛЯ ПОЛУЧЕНИЯ ПРАВ ====================

    /**
     * Получить эффективные права на проект (возвращает только битовую маску)
     * Избегает проблем с lazy loading
     */
    @Query(value = """
        SELECT iup.permission
        FROM item_user_permission iup
        INNER JOIN projects p ON p.id = :projectId
        WHERE iup.user_id = :userId
        AND (
            iup.project_id = :projectId
            OR iup.folder_id = p.folder_id
            OR (p.all_parant_ids @> ARRAY[iup.folder_id])
        )
        ORDER BY
            CASE
                WHEN iup.project_id = :projectId THEN -1
                WHEN iup.folder_id = p.folder_id THEN 0
                ELSE array_position(p.all_parant_ids, iup.folder_id)
            END ASC NULLS LAST
        LIMIT 1
        """, nativeQuery = true)
    Short findEffectivePermissionForProject(
            @Param("userId") String userId,
            @Param("projectId") String projectId
    );

    @Query(value = """
        SELECT iup.permission
        FROM item_user_permission iup
        INNER JOIN blocks b ON b.id = :blockId
        WHERE iup.user_id = :userId
        AND (
            iup.block_id = :blockId
            OR iup.folder_id = b.folder_id
            OR (b.all_parant_ids @> ARRAY[iup.folder_id])
        )
        ORDER BY
            CASE
                WHEN iup.block_id = :blockId THEN -1
                WHEN iup.folder_id = b.folder_id THEN 0
                ELSE array_position(b.all_parant_ids, iup.folder_id)
            END ASC NULLS LAST
        LIMIT 1
        """, nativeQuery = true)
    Short findEffectivePermissionForBlock(
            @Param("userId") String userId,
            @Param("blockId") String blockId
    );

    @Query(value = """
        SELECT iup.permission
        FROM item_user_permission iup
        INNER JOIN files f ON f.id = :fileId
        WHERE iup.user_id = :userId
        AND (
            iup.file_id = :fileId
            OR iup.folder_id = f.folder_id
            OR (f.all_parant_ids @> ARRAY[iup.folder_id])
        )
        ORDER BY
            CASE
                WHEN iup.file_id = :fileId THEN -1
                WHEN iup.folder_id = f.folder_id THEN 0
                ELSE array_position(f.all_parant_ids, iup.folder_id)
            END ASC NULLS LAST
        LIMIT 1
        """, nativeQuery = true)
    Short findEffectivePermissionForFile(
            @Param("userId") String userId,
            @Param("fileId") String fileId
    );

    @Query(value = """
        SELECT iup.permission
        FROM item_user_permission iup
        INNER JOIN folders f ON f.id = :folderId
        WHERE iup.user_id = :userId
        AND (
            iup.folder_id = :folderId
            OR (f.all_parant_ids @> ARRAY[iup.folder_id])
        )
        ORDER BY
            CASE
                WHEN iup.folder_id = :folderId THEN -1
                ELSE array_position(f.all_parant_ids, iup.folder_id)
            END ASC NULLS LAST
        LIMIT 1
        """, nativeQuery = true)
    Short findEffectivePermissionForFolder(
            @Param("userId") String userId,
            @Param("folderId") String folderId
    );

    // ==================== БЫСТРЫЕ ПРОВЕРКИ ПРАВ (boolean) ====================

    /**
     * Быстрая проверка наличия определенного права на проект
     * Возвращает boolean без извлечения всей записи
     */
    @Query(value = """
        SELECT EXISTS(
            SELECT 1
            FROM item_user_permission iup
            INNER JOIN projects p ON p.id = :projectId
            WHERE iup.user_id = :userId
            AND (
                iup.project_id = :projectId
                OR iup.folder_id = p.folder_id
                OR (p.all_parant_ids @> ARRAY[iup.folder_id])
            )
            AND (iup.permission & :permissionMask) = :permissionMask
            ORDER BY
                CASE
                    WHEN iup.project_id = :projectId THEN -1
                    WHEN iup.folder_id = p.folder_id THEN 0
                    ELSE array_position(p.all_parant_ids, iup.folder_id)
                END ASC NULLS LAST
            LIMIT 1
        )
        """, nativeQuery = true)
    boolean hasProjectPermission(
            @Param("userId") String userId,
            @Param("projectId") String projectId,
            @Param("permissionMask") short permissionMask
    );

    /**
     * Быстрая проверка права на блок
     */
    @Query(value = """
        SELECT EXISTS(
            SELECT 1
            FROM item_user_permission iup
            INNER JOIN blocks b ON b.id = :blockId
            WHERE iup.user_id = :userId
            AND (
                iup.block_id = :blockId
                OR iup.folder_id = b.folder_id
                OR (b.all_parant_ids @> ARRAY[iup.folder_id])
            )
            AND (iup.permission & :permissionMask) = :permissionMask
            ORDER BY
                CASE
                    WHEN iup.block_id = :blockId THEN -1
                    WHEN iup.folder_id = b.folder_id THEN 0
                    ELSE array_position(b.all_parant_ids, iup.folder_id)
                END ASC NULLS LAST
        )
        """, nativeQuery = true)
    boolean hasBlockPermission(
            @Param("userId") String userId,
            @Param("blockId") String blockId,
            @Param("permissionMask") short permissionMask
    );

    /**
     * Быстрая проверка права на файл
     */
    @Query(value = """
        SELECT EXISTS(
            SELECT 1
            FROM item_user_permission iup
            INNER JOIN files f ON f.id = :fileId
            WHERE iup.user_id = :userId
            AND (
                iup.file_id = :fileId
                OR iup.folder_id = f.folder_id
                OR (f.all_parant_ids @> ARRAY[iup.folder_id])
            )
            AND (iup.permission & :permissionMask) = :permissionMask
            ORDER BY
                CASE
                    WHEN iup.file_id = :fileId THEN -1
                    WHEN iup.folder_id = f.folder_id THEN 0
                    ELSE array_position(f.all_parant_ids, iup.folder_id)
                END ASC NULLS LAST
            LIMIT 1
        )
        """, nativeQuery = true)
    boolean hasFilePermission(
            @Param("userId") String userId,
            @Param("fileId") String fileId,
            @Param("permissionMask") short permissionMask
    );

    /**
     * Быстрая проверка права на папку
     */
    @Query(value = """
        SELECT EXISTS(
            SELECT 1
            FROM item_user_permission iup
            INNER JOIN folders f ON f.id = :folderId
            WHERE iup.user_id = :userId
            AND (
                iup.folder_id = :folderId
                OR (f.all_parant_ids @> ARRAY[iup.folder_id])
            )
            AND (iup.permission & :permissionMask) = :permissionMask
            ORDER BY
                CASE
                    WHEN iup.folder_id = :folderId THEN -1
                    ELSE array_position(f.all_parant_ids, iup.folder_id)
                END ASC NULLS LAST
            LIMIT 1
        )
        """, nativeQuery = true)
    boolean hasFolderPermission(
            @Param("userId") String userId,
            @Param("folderId") String folderId,
            @Param("permissionMask") short permissionMask
    );

    // ==================== МАССОВЫЕ ОПЕРАЦИИ (BATCH) ====================

    /**
     * Получить права для нескольких проектов одновременно
     * Возвращает пары (project_id, permissions)
     */
    @Query(value = """
        SELECT DISTINCT ON (p.id)
            p.id as project_id,
            COALESCE(iup.permissions, 0) as permissions
        FROM projects p
        LEFT JOIN LATERAL (
            SELECT iup.permission
            FROM item_user_permission iup
            WHERE iup.user_id = :userId
            AND (
                iup.project_id = p.id
                OR iup.folder_id = p.folder_id
                OR (p.all_parant_ids @> ARRAY[iup.folder_id])
            )
            ORDER BY
                CASE
                    WHEN iup.project_id = p.id THEN -1
                    WHEN iup.folder_id = p.folder_id THEN 0
                    ELSE array_position(p.all_parant_ids, iup.folder_id)
                END ASC NULLS LAST
            LIMIT 1
        ) iup ON true
        WHERE p.id = ANY(:projectIds)
        """, nativeQuery = true)
    List<Object[]> findEffectivePermissionsForProjects(
            @Param("userId") String userId,
            @Param("projectIds") String[] projectIds
    );

    /**
     * Получить права для нескольких блоков одновременно
     */
    @Query(value = """
        SELECT DISTINCT ON (b.id)
            b.id as block_id,
            COALESCE(iup.permission, 0) as permissions
        FROM blocks b
        LEFT JOIN LATERAL (
            SELECT iup.permission
            FROM item_user_permission iup
            WHERE iup.user_id = :userId
            AND (
                iup.block_id = b.id
                OR iup.folder_id = b.folder_id
                OR (b.all_parant_ids @> ARRAY[iup.folder_id])
            )
            ORDER BY
                CASE
                    WHEN iup.block_id = b.id THEN -1
                    WHEN iup.folder_id = b.folder_id THEN 0
                    ELSE array_position(b.all_parant_ids, iup.folder_id)
                END ASC NULLS LAST
            LIMIT 1
        ) iup ON true
        WHERE b.id = ANY(:blockIds)
        """, nativeQuery = true)
    List<Object[]> findEffectivePermissionsForBlocks(
            @Param("userId") String userId,
            @Param("blockIds") String[] blockIds
    );

    /**
     * Получить права для нескольких файлов одновременно
     */
    @Query(value = """
        SELECT DISTINCT ON (f.id)
            f.id as file_id,
            COALESCE(iup.permission, 0) as permissions
        FROM files f
        LEFT JOIN LATERAL (
            SELECT iup.permission
            FROM item_user_permission iup
            WHERE iup.user_id = :userId
            AND (
                iup.file_id = f.id
                OR iup.folder_id = f.folder_id
                OR (f.all_parant_ids @> ARRAY[iup.folder_id])
            )
            ORDER BY
                CASE
                    WHEN iup.file_id = f.id THEN -1
                    WHEN iup.folder_id = f.folder_id THEN 0
                    ELSE array_position(f.all_parant_ids, iup.folder_id)
                END ASC NULLS LAST
            LIMIT 1
        ) iup ON true
        WHERE f.id = ANY(:fileIds)
        """, nativeQuery = true)
    List<Object[]> findEffectivePermissionsForFiles(
            @Param("userId") String userId,
            @Param("fileIds") String[] fileIds
    );

    /**
     * Получить права для нескольких папок одновременно
     */
    @Query(value = """
        SELECT DISTINCT ON (f.id)
            f.id as folder_id,
            COALESCE(iup.permission, 0) as permissions
        FROM folders f
        LEFT JOIN LATERAL (
            SELECT iup.permission
            FROM item_user_permission iup
            WHERE iup.user_id = :userId
            AND (
                iup.folder_id = f.id
                OR (f.all_parant_ids @> ARRAY[iup.folder_id])
            )
            ORDER BY
                CASE
                    WHEN iup.folder_id = f.id THEN -1
                    ELSE array_position(f.all_parant_ids, iup.folder_id)
                END ASC NULLS LAST
            LIMIT 1
        ) iup ON true
        WHERE f.id = ANY(:folderIds)
        """, nativeQuery = true)
    List<Object[]> findEffectivePermissionsForFolders(
            @Param("userId") String userId,
            @Param("folderIds") String[] folderIds
    );

    // ==================== ПОЛУЧЕНИЕ ВСЕХ ДОСТУПНЫХ ЭЛЕМЕНТОВ ====================

    /**
     * Получить все проекты с правами доступа для пользователя
     * Возвращает только те проекты, к которым есть хоть какой-то доступ
     */
    @Query(value = """
        SELECT
            p.id,
            p.folder_id,
            p.rank,
            p.created_at,
            p.updated_at,
            p.tenant_id,
            COALESCE(iup.permission, 0) as effective_permissions
        FROM projects p
        LEFT JOIN LATERAL (
            SELECT iup.permission
            FROM item_user_permission iup
            WHERE iup.user_id = :userId
            AND (
                iup.project_id = p.id
                OR iup.folder_id = p.folder_id
                OR (p.all_parant_ids @> ARRAY[iup.folder_id])
            )
            ORDER BY
                CASE
                    WHEN iup.project_id = p.id THEN -1
                    WHEN iup.folder_id = p.folder_id THEN 0
                    ELSE array_position(p.all_parant_ids, iup.folder_id)
                END ASC NULLS LAST
            LIMIT 1
        ) iup ON true
        WHERE p.tenant_id = :tenantId
        AND COALESCE(iup.permission, 0) > 0
        ORDER BY p.rank, p.created_at
        """, nativeQuery = true)
    List<Object[]> findAllAccessibleProjects(
            @Param("userId") String userId,
            @Param("tenantId") String tenantId
    );

    /**
     * Получить все блоки с правами доступа для пользователя
     */
    @Query(value = """
        SELECT
            b.id,
            b.folder_id,
            b.rank,
            b.created_at,
            b.updated_at,
            b.tenant_id,
            COALESCE(iup.permission, 0) as effective_permissions
        FROM blocks b
        LEFT JOIN LATERAL (
            SELECT iup.permission
            FROM item_user_permission iup
            WHERE iup.user_id = :userId
            AND (
                iup.block_id = b.id
                OR iup.folder_id = b.folder_id
                OR (b.all_parant_ids @> ARRAY[iup.folder_id])
            )
            ORDER BY
                CASE
                    WHEN iup.block_id = b.id THEN -1
                    WHEN iup.folder_id = b.folder_id THEN 0
                    ELSE array_position(b.all_parant_ids, iup.folder_id)
                END ASC NULLS LAST
            LIMIT 1
        ) iup ON true
        WHERE b.tenant_id = :tenantId
        AND COALESCE(iup.permission, 0) > 0
        ORDER BY b.rank, b.created_at
        """, nativeQuery = true)
    List<Object[]> findAllAccessibleBlocks(
            @Param("userId") String userId,
            @Param("tenantId") String tenantId
    );

    /**
     * Получить все файлы с правами доступа для пользователя
     */
    @Query(value = """
        SELECT
            f.id,
            f.folder_id,
            f.rank,
            f.created_at,
            f.updated_at,
            f.tenant_id,
            COALESCE(iup.permission, 0) as effective_permissions
        FROM files f
        LEFT JOIN LATERAL (
            SELECT iup.permission
            FROM item_user_permission iup
            WHERE iup.user_id = :userId
            AND (
                iup.file_id = f.id
                OR iup.folder_id = f.folder_id
                OR (f.all_parant_ids @> ARRAY[iup.folder_id])
            )
            ORDER BY
                CASE
                    WHEN iup.file_id = f.id THEN -1
                    WHEN iup.folder_id = f.folder_id THEN 0
                    ELSE array_position(f.all_parant_ids, iup.folder_id)
                END ASC NULLS LAST
            LIMIT 1
        ) iup ON true
        WHERE f.tenant_id = :tenantId
        AND COALESCE(iup.permission, 0) > 0
        ORDER BY f.rank, f.created_at
        """, nativeQuery = true)
    List<Object[]> findAllAccessibleFiles(
            @Param("userId") String userId,
            @Param("tenantId") String tenantId
    );

    /**
     * Получить все папки с правами доступа для пользователя
     */
    @Query(value = """
        SELECT 
            f.id,
            f.name,
            f.parent_id,
            f.rank,
            f.has_children,
            f.created_at,
            f.updated_at,
            f.tenant_id,
            COALESCE(iup.permission, 0) as effective_permissions
        FROM folders f
        LEFT JOIN LATERAL (
            SELECT iup.permission
            FROM item_user_permission iup
            WHERE iup.user_id = :userId
            AND (
                iup.folder_id = f.id
                OR (f.all_parant_ids @> ARRAY[iup.folder_id])
            )
            ORDER BY
                CASE
                    WHEN iup.folder_id = f.id THEN -1
                    ELSE array_position(f.all_parant_ids, iup.folder_id)
                END ASC NULLS LAST
            LIMIT 1
        ) iup ON true
        WHERE f.tenant_id = :tenantId
        AND COALESCE(iup.permission, 0) > 0
        ORDER BY f.rank, f.created_at
        """, nativeQuery = true)
    List<Object[]> findAllAccessibleFolders(
            @Param("userId") String userId,
            @Param("tenantId") String tenantId
    );

    // ==================== ПОЛУЧЕНИЕ ЭЛЕМЕНТОВ В ПАПКЕ С ПРАВАМИ ====================

    /**
     * Получить все проекты в конкретной папке с правами доступа
     */
    @Query(value = """
        SELECT
            p.id,
            p.folder_id,
            p.rank,
            p.created_at,
            COALESCE(iup.permission, 0) as effective_permissions
        FROM projects p
        LEFT JOIN LATERAL (
            SELECT iup.permission
            FROM item_user_permission iup
            WHERE iup.user_id = :userId
            AND (
                iup.project_id = p.id
                OR iup.folder_id = p.folder_id
                OR (p.all_parant_ids @> ARRAY[iup.folder_id])
            )
            ORDER BY
                CASE
                    WHEN iup.project_id = p.id THEN -1
                    WHEN iup.folder_id = p.folder_id THEN 0
                    ELSE array_position(p.all_parant_ids, iup.folder_id)
                END ASC NULLS LAST
            LIMIT 1
        ) iup ON true
        WHERE p.folder_id = :folderId
        AND COALESCE(iup.permission, 0) > 0
        ORDER BY p.rank, p.created_at
        """, nativeQuery = true)
    List<Object[]> findProjectsInFolderWithPermissions(
            @Param("userId") String userId,
            @Param("folderId") String folderId
    );

    /**
     * Получить все блоки в конкретной папке с правами доступа
     */
    @Query(value = """
        SELECT
            b.id,
            b.folder_id,
            b.rank,
            b.created_at,
            COALESCE(iup.permission, 0) as effective_permissions
        FROM blocks b
        LEFT JOIN LATERAL (
            SELECT iup.permission
            FROM item_user_permission iup
            WHERE iup.user_id = :userId
            AND (
                iup.block_id = b.id
                OR iup.folder_id = b.folder_id
                OR (b.all_parant_ids @> ARRAY[iup.folder_id])
            )
            ORDER BY
                CASE
                    WHEN iup.block_id = b.id THEN -1
                    WHEN iup.folder_id = b.folder_id THEN 0
                    ELSE array_position(b.all_parant_ids, iup.folder_id)
                END ASC NULLS LAST
            LIMIT 1
        ) iup ON true
        WHERE b.folder_id = :folderId
        AND COALESCE(iup.permission, 0) > 0
        ORDER BY b.rank, b.created_at
        """, nativeQuery = true)
    List<Object[]> findBlocksInFolderWithPermissions(
            @Param("userId") String userId,
            @Param("folderId") String folderId
    );

    /**
     * Получить все файлы в конкретной папке с правами доступа
     */
    @Query(value = """
        SELECT 
            f.id,
            f.folder_id,
            f.rank,
            f.created_at,
            COALESCE(iup.permission, 0) as effective_permissions
        FROM files f
        LEFT JOIN LATERAL (
            SELECT iup.permission
            FROM item_user_permission iup
            WHERE iup.user_id = :userId
            AND (
                iup.file_id = f.id
                OR iup.folder_id = f.folder_id
                OR (f.all_parant_ids @> ARRAY[iup.folder_id])
            )
            ORDER BY
                CASE
                    WHEN iup.file_id = f.id THEN -1
                    WHEN iup.folder_id = f.folder_id THEN 0
                    ELSE array_position(f.all_parant_ids, iup.folder_id)
                END ASC NULLS LAST
            LIMIT 1
        ) iup ON true
        WHERE f.folder_id = :folderId
        AND COALESCE(iup.permission, 0) > 0
        ORDER BY f.rank, f.created_at
        """, nativeQuery = true)
    List<Object[]> findFilesInFolderWithPermissions(
            @Param("userId") String userId,
            @Param("folderId") String folderId
    );

    /**
     * Получить дочерние папки в конкретной папке с правами доступа
     */
    @Query(value = """
        SELECT 
            f.id,
            f.name,
            f.parent_id,
            f.rank,
            f.has_children,
            f.created_at,
            COALESCE(iup.permission, 0) as effective_permissions
        FROM folders f
        LEFT JOIN LATERAL (
            SELECT iup.permission
            FROM item_user_permission iup
            WHERE iup.user_id = :userId
            AND (
                iup.folder_id = f.id
                OR (f.all_parant_ids @> ARRAY[iup.folder_id])
            )
            ORDER BY
                CASE
                    WHEN iup.folder_id = f.id THEN -1
                    ELSE array_position(f.all_parant_ids, iup.folder_id)
                END ASC NULLS LAST
            LIMIT 1
        ) iup ON true
        WHERE f.parent_id = :parentFolderId
        AND COALESCE(iup.permission, 0) > 0
        ORDER BY f.rank, f.created_at
        """, nativeQuery = true)
    List<Object[]> findSubfoldersWithPermissions(
            @Param("userId") String userId,
            @Param("parentFolderId") String parentFolderId
    );

    // ==================== ПОДСЧЕТ ЭЛЕМЕНТОВ С ДОСТУПОМ ====================

    /**
     * Подсчитать количество проектов с минимальным уровнем доступа
     */
    @Query(value = """
        SELECT COUNT(DISTINCT p.id)
        FROM projects p
        INNER JOIN item_user_permission iup ON (
            iup.user_id = :userId
            AND (
                iup.project_id = p.id
                OR iup.folder_id = p.folder_id
                OR (p.all_parant_ids @> ARRAY[iup.folder_id])
            )
            AND (iup.permission & :minPermissionMask) > 0
        )
        WHERE p.tenant_id = :tenantId
        """, nativeQuery = true)
    long countAccessibleProjects(
            @Param("userId") String userId,
            @Param("tenantId") String tenantId,
            @Param("minPermissionMask") short minPermissionMask
    );

    /**
     * Подсчитать количество блоков с минимальным уровнем доступа
     */
    @Query(value = """
        SELECT COUNT(DISTINCT b.id)
        FROM blocks b
        INNER JOIN item_user_permission iup ON (
            iup.user_id = :userId
            AND (
                iup.block_id = b.id
                OR iup.folder_id = b.folder_id
                OR (b.all_parant_ids @> ARRAY[iup.folder_id])
            )
            AND (iup.permission & :minPermissionMask) > 0
        )
        WHERE b.tenant_id = :tenantId
        """, nativeQuery = true)
    long countAccessibleBlocks(
            @Param("userId") String userId,
            @Param("tenantId") String tenantId,
            @Param("minPermissionMask") short minPermissionMask
    );
}