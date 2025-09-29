package ru.platik777.backauth.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Репозиторий для проверки доступа к проектам пользователя
 * Работает с динамическими схемами userN
 */
@Repository
public class ProjectAccessRepository {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public ProjectAccessRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Проверка принадлежности проекта пользователю
     * @param userId ID пользователя
     * @param projectId ID проекта
     * @return true если проект принадлежит пользователю
     */
    public boolean checkProjectOwnership(Integer userId, Integer projectId) {
        String schemaName = "user" + userId;
        String sql = String.format(
                "SELECT project_name FROM %s.projects WHERE status = true AND project_id = ?",
                schemaName
        );

        try {
            String projectName = jdbcTemplate.queryForObject(sql, String.class, projectId);
            return projectName != null && !projectName.trim().isEmpty();
        } catch (EmptyResultDataAccessException e) {
            return false;
        } catch (Exception e) {
            // Схема может не существовать или другие ошибки
            return false;
        }
    }

    /**
     * Проверка существования схемы пользователя
     */
    public boolean userSchemaExists(Integer userId) {
        String sql = "SELECT COUNT(*) FROM information_schema.schemata WHERE schema_name = ?";
        String schemaName = "user" + userId;

        try {
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class, schemaName);
            return count != null && count > 0;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Получение информации о проекте
     */
    public String getProjectName(Integer userId, Integer projectId) {
        if (!userSchemaExists(userId)) {
            return null;
        }

        String schemaName = "user" + userId;
        String sql = String.format(
                "SELECT project_name FROM %s.projects WHERE project_id = ?",
                schemaName
        );

        try {
            return jdbcTemplate.queryForObject(sql, String.class, projectId);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }
}