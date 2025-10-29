package ru.platik777.backauth.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.platik777.backauth.entity.File;

import java.util.List;

/**
 * Репозиторий для работы с файлами
 * Включает методы получения файлов с учетом прав доступа пользователей
 */
@Repository
public interface FileRepository extends JpaRepository<File, String> {

    // ==================== ПОЛУЧЕНИЕ ПРАВ ДЛЯ МНОЖЕСТВА ФАЙЛОВ ====================

    /**
     * Получить права для нескольких файлов одновременно
     */
    @Query(value = """
        SELECT DISTINCT ON (f.id)
            f.id as file_id,
            COALESCE(iup.permission, 0) as permissions
        FROM file f
        LEFT JOIN LATERAL (
            SELECT iup.permission
            FROM item_user_permission iup
            WHERE iup.user_id = :userId
            AND (
                iup.file_id = f.id
                OR iup.folder_id = f.folder_id
                OR (f.all_parent_ids @> ARRAY[iup.folder_id])
            )
            ORDER BY
                CASE
                    WHEN iup.file_id = f.id THEN -1
                    WHEN iup.folder_id = f.folder_id THEN 0
                    ELSE array_position(f.all_parent_ids, iup.folder_id)
                END ASC NULLS LAST
            LIMIT 1
        ) iup ON true
        WHERE f.id = ANY(:fileIds)
        """, nativeQuery = true)
    List<Object[]> findEffectivePermissionsForFiles(
            @Param("userId") String userId,
            @Param("fileIds") String[] fileIds
    );

    // ==================== ПОЛУЧЕНИЕ ВСЕХ ДОСТУПНЫХ ФАЙЛОВ ====================

    /**
     * Получить все файлы с правами доступа для пользователя в конкретном tenant
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
        FROM file f
        LEFT JOIN LATERAL (
            SELECT iup.permission
            FROM item_user_permission iup
            WHERE iup.user_id = :userId
            AND (
                iup.file_id = f.id
                OR iup.folder_id = f.folder_id
                OR (f.all_parent_ids @> ARRAY[iup.folder_id])
            )
            ORDER BY
                CASE
                    WHEN iup.file_id = f.id THEN -1
                    WHEN iup.folder_id = f.folder_id THEN 0
                    ELSE array_position(f.all_parent_ids, iup.folder_id)
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

    // ==================== ПОЛУЧЕНИЕ ФАЙЛОВ В ПАПКЕ ====================

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
        FROM file f
        LEFT JOIN LATERAL (
            SELECT iup.permission
            FROM item_user_permission iup
            WHERE iup.user_id = :userId
            AND (
                iup.file_id = f.id
                OR iup.folder_id = f.folder_id
                OR (f.all_parent_ids @> ARRAY[iup.folder_id])
            )
            ORDER BY
                CASE
                    WHEN iup.file_id = f.id THEN -1
                    WHEN iup.folder_id = f.folder_id THEN 0
                    ELSE array_position(f.all_parent_ids, iup.folder_id)
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
}