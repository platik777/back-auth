package ru.platik777.backauth.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.platik777.backauth.entity.Project;

import java.util.List;

/**
 * Репозиторий для работы с проектами
 * Включает методы получения проектов с учетом прав доступа пользователей
 */
@Repository
public interface ProjectRepository extends JpaRepository<Project, String> {

    // ==================== ПОЛУЧЕНИЕ ПРАВ ДЛЯ МНОЖЕСТВА ПРОЕКТОВ ====================

    /**
     * Получить права для нескольких проектов одновременно
     * Возвращает пары (project_id, permissions)
     */
    @Query(value = """
        SELECT DISTINCT ON (p.id)
            p.id as project_id,
            COALESCE(iup.permission, 0) as permissions
        FROM project p
        LEFT JOIN LATERAL (
            SELECT iup.permission
            FROM item_user_permission iup
            WHERE iup.user_id = :userId
            AND (
                iup.project_id = p.id
                OR iup.folder_id = p.folder_id
                OR (p.all_parent_ids @> ARRAY[iup.folder_id])
            )
            ORDER BY
                CASE
                    WHEN iup.project_id = p.id THEN -1
                    WHEN iup.folder_id = p.folder_id THEN 0
                    ELSE array_position(p.all_parent_ids, iup.folder_id)
                END ASC NULLS LAST
            LIMIT 1
        ) iup ON true
        WHERE p.id = ANY(:projectIds)
        """, nativeQuery = true)
    List<Object[]> findEffectivePermissionsForProjects(
            @Param("userId") String userId,
            @Param("projectIds") String[] projectIds
    );

    // ==================== ПОЛУЧЕНИЕ ВСЕХ ДОСТУПНЫХ ПРОЕКТОВ ====================

    /**
     * Получить все проекты с правами доступа для пользователя в конкретном tenant
     * Возвращает только те проекты, к которым есть хоть какой-то доступ (permission > 0)
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
        FROM project p
        LEFT JOIN LATERAL (
            SELECT iup.permission
            FROM item_user_permission iup
            WHERE iup.user_id = :userId
            AND (
                iup.project_id = p.id
                OR iup.folder_id = p.folder_id
                OR (p.all_parent_ids @> ARRAY[iup.folder_id])
            )
            ORDER BY
                CASE
                    WHEN iup.project_id = p.id THEN -1
                    WHEN iup.folder_id = p.folder_id THEN 0
                    ELSE array_position(p.all_parent_ids, iup.folder_id)
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

    // ==================== ПОЛУЧЕНИЕ ПРОЕКТОВ В ПАПКЕ ====================

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
        FROM project p
        LEFT JOIN LATERAL (
            SELECT iup.permission
            FROM item_user_permission iup
            WHERE iup.user_id = :userId
            AND (
                iup.project_id = p.id
                OR iup.folder_id = p.folder_id
                OR (p.all_parent_ids @> ARRAY[iup.folder_id])
            )
            ORDER BY
                CASE
                    WHEN iup.project_id = p.id THEN -1
                    WHEN iup.folder_id = p.folder_id THEN 0
                    ELSE array_position(p.all_parent_ids, iup.folder_id)
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

    // ==================== ПОДСЧЕТ ДОСТУПНЫХ ПРОЕКТОВ ====================

    /**
     * Подсчитать количество проектов с минимальным уровнем доступа
     */
    @Query(value = """
        SELECT COUNT(DISTINCT p.id)
        FROM project p
        INNER JOIN item_user_permission iup ON (
            iup.user_id = :userId
            AND (
                iup.project_id = p.id
                OR iup.folder_id = p.folder_id
                OR (p.all_parent_ids @> ARRAY[iup.folder_id])
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
}