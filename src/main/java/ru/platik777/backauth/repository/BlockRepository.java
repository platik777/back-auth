package ru.platik777.backauth.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.platik777.backauth.entity.Block;

import java.util.List;

/**
 * Репозиторий для работы с блоками
 * Включает методы получения блоков с учетом прав доступа пользователей
 */
@Repository
public interface BlockRepository extends JpaRepository<Block, String> {

    // ==================== ПОЛУЧЕНИЕ ПРАВ ДЛЯ МНОЖЕСТВА БЛОКОВ ====================

    /**
     * Получить права для нескольких блоков одновременно
     */
    @Query(value = """
        SELECT DISTINCT ON (b.id)
            b.id as block_id,
            COALESCE(iup.permission, 0) as permissions
        FROM block b
        LEFT JOIN LATERAL (
            SELECT iup.permission
            FROM item_user_permission iup
            WHERE iup.user_id = :userId
            AND (
                iup.block_id = b.id
                OR iup.folder_id = b.folder_id
                OR (b.all_parent_ids @> ARRAY[iup.folder_id])
            )
            ORDER BY
                CASE
                    WHEN iup.block_id = b.id THEN -1
                    WHEN iup.folder_id = b.folder_id THEN 0
                    ELSE array_position(b.all_parent_ids, iup.folder_id)
                END ASC NULLS LAST
            LIMIT 1
        ) iup ON true
        WHERE b.id = ANY(:blockIds)
        """, nativeQuery = true)
    List<Object[]> findEffectivePermissionsForBlocks(
            @Param("userId") String userId,
            @Param("blockIds") String[] blockIds
    );

    // ==================== ПОЛУЧЕНИЕ ВСЕХ ДОСТУПНЫХ БЛОКОВ ====================

    /**
     * Получить все блоки с правами доступа для пользователя в конкретном tenant
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
        FROM block b
        LEFT JOIN LATERAL (
            SELECT iup.permission
            FROM item_user_permission iup
            WHERE iup.user_id = :userId
            AND (
                iup.block_id = b.id
                OR iup.folder_id = b.folder_id
                OR (b.all_parent_ids @> ARRAY[iup.folder_id])
            )
            ORDER BY
                CASE
                    WHEN iup.block_id = b.id THEN -1
                    WHEN iup.folder_id = b.folder_id THEN 0
                    ELSE array_position(b.all_parent_ids, iup.folder_id)
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

    // ==================== ПОЛУЧЕНИЕ БЛОКОВ В ПАПКЕ ====================

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
        FROM block b
        LEFT JOIN LATERAL (
            SELECT iup.permission
            FROM item_user_permission iup
            WHERE iup.user_id = :userId
            AND (
                iup.block_id = b.id
                OR iup.folder_id = b.folder_id
                OR (b.all_parent_ids @> ARRAY[iup.folder_id])
            )
            ORDER BY
                CASE
                    WHEN iup.block_id = b.id THEN -1
                    WHEN iup.folder_id = b.folder_id THEN 0
                    ELSE array_position(b.all_parent_ids, iup.folder_id)
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

    // ==================== ПОДСЧЕТ ДОСТУПНЫХ БЛОКОВ ====================

    /**
     * Подсчитать количество блоков с минимальным уровнем доступа
     */
    @Query(value = """
        SELECT COUNT(DISTINCT b.id)
        FROM block b
        INNER JOIN item_user_permission iup ON (
            iup.user_id = :userId
            AND (
                iup.block_id = b.id
                OR iup.folder_id = b.folder_id
                OR (b.all_parent_ids @> ARRAY[iup.folder_id])
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