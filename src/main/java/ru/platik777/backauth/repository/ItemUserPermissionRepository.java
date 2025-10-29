package ru.platik777.backauth.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.platik777.backauth.entity.ItemUserPermission;

import java.util.Optional;

/**
 * Репозиторий для работы с правами доступа пользователей
 * Содержит ТОЛЬКО методы проверки прав (hasPermission)
 *
 * Методы получения элементов с учетом прав перенесены в:
 * - ProjectRepository - для работы с проектами
 * - BlockRepository - для работы с блоками
 * - FileRepository - для работы с файлами
 * - FolderRepository - для работы с папками
 */
@Repository
public interface ItemUserPermissionRepository extends JpaRepository<ItemUserPermission, String> {

    // ==================== БАЗОВЫЕ МЕТОДЫ ДЛЯ ПОЛУЧЕНИЯ СУЩНОСТЕЙ ПРАВ ====================

    Optional<ItemUserPermission> findByUserIdAndProjectId(String userId, String projectId);

    Optional<ItemUserPermission> findByUserIdAndFolderId(String userId, String folderId);

    Optional<ItemUserPermission> findByUserIdAndFileId(String userId, String fileId);

    Optional<ItemUserPermission> findByUserIdAndBlockId(String userId, String blockId);

    // ==================== ПОЛУЧЕНИЕ ЭФФЕКТИВНЫХ ПРАВ (для одного элемента) ====================

    /**
     * Получить эффективные права на проект (возвращает только битовую маску)
     * Учитывает наследование от родительских папок
     */
    @Query(value = """
        SELECT iup.permission
        FROM item_user_permission iup
        INNER JOIN project p ON p.id = :projectId
        WHERE iup.user_id = :userId
        AND (
            iup.project_id = :projectId
            OR iup.folder_id = p.folder_id
            OR (p.all_parent_ids @> ARRAY[iup.folder_id])
        )
        ORDER BY
            CASE
                WHEN iup.project_id = :projectId THEN -1
                WHEN iup.folder_id = p.folder_id THEN 0
                ELSE array_position(p.all_parent_ids, iup.folder_id)
            END ASC NULLS LAST
        LIMIT 1
        """, nativeQuery = true)
    Short findEffectivePermissionForProject(
            @Param("userId") String userId,
            @Param("projectId") String projectId
    );

    /**
     * Получить эффективные права на блок
     */
    @Query(value = """
        SELECT iup.permission
        FROM item_user_permission iup
        INNER JOIN block b ON b.id = :blockId
        WHERE iup.user_id = :userId
        AND (
            iup.block_id = :blockId
            OR iup.folder_id = b.folder_id
            OR (b.all_parent_ids @> ARRAY[iup.folder_id])
        )
        ORDER BY
            CASE
                WHEN iup.block_id = :blockId THEN -1
                WHEN iup.folder_id = b.folder_id THEN 0
                ELSE array_position(b.all_parent_ids, iup.folder_id)
            END ASC NULLS LAST
        LIMIT 1
        """, nativeQuery = true)
    Short findEffectivePermissionForBlock(
            @Param("userId") String userId,
            @Param("blockId") String blockId
    );

    /**
     * Получить эффективные права на файл
     */
    @Query(value = """
        SELECT iup.permission
        FROM item_user_permission iup
        INNER JOIN file f ON f.id = :fileId
        WHERE iup.user_id = :userId
        AND (
            iup.file_id = :fileId
            OR iup.folder_id = f.folder_id
            OR (f.all_parent_ids @> ARRAY[iup.folder_id])
        )
        ORDER BY
            CASE
                WHEN iup.file_id = :fileId THEN -1
                WHEN iup.folder_id = f.folder_id THEN 0
                ELSE array_position(f.all_parent_ids, iup.folder_id)
            END ASC NULLS LAST
        LIMIT 1
        """, nativeQuery = true)
    Short findEffectivePermissionForFile(
            @Param("userId") String userId,
            @Param("fileId") String fileId
    );

    /**
     * Получить эффективные права на папку
     */
    @Query(value = """
        SELECT iup.permission
        FROM item_user_permission iup
        INNER JOIN folder f ON f.id = :folderId
        WHERE iup.user_id = :userId
        AND (
            iup.folder_id = :folderId
            OR (f.all_parent_ids @> ARRAY[iup.folder_id])
        )
        ORDER BY
            CASE
                WHEN iup.folder_id = :folderId THEN -1
                ELSE array_position(f.all_parent_ids, iup.folder_id)
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
            INNER JOIN project p ON p.id = :projectId
            WHERE iup.user_id = :userId
            AND (
                iup.project_id = :projectId
                OR iup.folder_id = p.folder_id
                OR (p.all_parent_ids @> ARRAY[iup.folder_id])
            )
            AND (iup.permission & :permissionMask) = :permissionMask
            ORDER BY
                CASE
                    WHEN iup.project_id = :projectId THEN -1
                    WHEN iup.folder_id = p.folder_id THEN 0
                    ELSE array_position(p.all_parent_ids, iup.folder_id)
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
            INNER JOIN block b ON b.id = :blockId
            WHERE iup.user_id = :userId
            AND (
                iup.block_id = :blockId
                OR iup.folder_id = b.folder_id
                OR (b.all_parent_ids @> ARRAY[iup.folder_id])
            )
            AND (iup.permission & :permissionMask) = :permissionMask
            ORDER BY
                CASE
                    WHEN iup.block_id = :blockId THEN -1
                    WHEN iup.folder_id = b.folder_id THEN 0
                    ELSE array_position(b.all_parent_ids, iup.folder_id)
                END ASC NULLS LAST
            LIMIT 1
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
            INNER JOIN file f ON f.id = :fileId
            WHERE iup.user_id = :userId
            AND (
                iup.file_id = :fileId
                OR iup.folder_id = f.folder_id
                OR (f.all_parent_ids @> ARRAY[iup.folder_id])
            )
            AND (iup.permission & :permissionMask) = :permissionMask
            ORDER BY
                CASE
                    WHEN iup.file_id = :fileId THEN -1
                    WHEN iup.folder_id = f.folder_id THEN 0
                    ELSE array_position(f.all_parent_ids, iup.folder_id)
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
            INNER JOIN folder f ON f.id = :folderId
            WHERE iup.user_id = :userId
            AND (
                iup.folder_id = :folderId
                OR (f.all_parent_ids @> ARRAY[iup.folder_id])
            )
            AND (iup.permission & :permissionMask) = :permissionMask
            ORDER BY
                CASE
                    WHEN iup.folder_id = :folderId THEN -1
                    ELSE array_position(f.all_parent_ids, iup.folder_id)
                END ASC NULLS LAST
            LIMIT 1
        )
        """, nativeQuery = true)
    boolean hasFolderPermission(
            @Param("userId") String userId,
            @Param("folderId") String folderId,
            @Param("permissionMask") short permissionMask
    );
}