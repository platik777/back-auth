package ru.platik777.backauth.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.platik777.backauth.entity.Folder;

import java.util.List;

/**
 * Репозиторий для работы с папками
 * Включает методы получения папок с учетом прав доступа пользователей
 */
@Repository
public interface FolderRepository extends JpaRepository<Folder, String> {

    // ==================== ПОЛУЧЕНИЕ ПРАВ ДЛЯ МНОЖЕСТВА ПАПОК ====================

    /**
     * Получить права для нескольких папок одновременно
     */
    @Query(value = """
        SELECT DISTINCT ON (f.id)
            f.id as folder_id,
            COALESCE(iup.permission, 0) as permissions
        FROM folder f
        LEFT JOIN LATERAL (
            SELECT iup.permission
            FROM item_user_permission iup
            WHERE iup.user_id = :userId
            AND (
                iup.folder_id = f.id
                OR (f.all_parent_ids @> ARRAY[iup.folder_id])
            )
            ORDER BY
                CASE
                    WHEN iup.folder_id = f.id THEN -1
                    ELSE array_position(f.all_parent_ids, iup.folder_id)
                END ASC NULLS LAST
            LIMIT 1
        ) iup ON true
        WHERE f.id = ANY(:folderIds)
        """, nativeQuery = true)
    List<Object[]> findEffectivePermissionsForFolders(
            @Param("userId") String userId,
            @Param("folderIds") String[] folderIds
    );

    // ==================== ПОЛУЧЕНИЕ ВСЕХ ДОСТУПНЫХ ПАПОК ====================

    /**
     * Получить все папки с правами доступа для пользователя в конкретном tenant
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
        FROM folder f
        LEFT JOIN LATERAL (
            SELECT iup.permission
            FROM item_user_permission iup
            WHERE iup.user_id = :userId
            AND (
                iup.folder_id = f.id
                OR (f.all_parent_ids @> ARRAY[iup.folder_id])
            )
            ORDER BY
                CASE
                    WHEN iup.folder_id = f.id THEN -1
                    ELSE array_position(f.all_parent_ids, iup.folder_id)
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

    // ==================== ПОЛУЧЕНИЕ ДОЧЕРНИХ ПАПОК ====================

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
        FROM folder f
        LEFT JOIN LATERAL (
            SELECT iup.permission
            FROM item_user_permission iup
            WHERE iup.user_id = :userId
            AND (
                iup.folder_id = f.id
                OR (f.all_parent_ids @> ARRAY[iup.folder_id])
            )
            ORDER BY
                CASE
                    WHEN iup.folder_id = f.id THEN -1
                    ELSE array_position(f.all_parent_ids, iup.folder_id)
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
}