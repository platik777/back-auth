package ru.platik777.backauth.repository;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Репозиторий для проверки доступа к проектам пользователя
 * Соответствует методу CheckProjectId из PostgreSqlAuth.go
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class ProjectAccessRepository {

    private final JdbcTemplate jdbcTemplate;

    /**
     * CheckProjectId - проверка принадлежности проекта пользователю
     * Go: SELECT project_name FROM userN.projects WHERE status = true AND project_id = $1
     *
     * @param userId ID пользователя
     * @param projectId ID проекта
     * @return true если проект принадлежит пользователю
     */
    public boolean checkProjectOwnership(Integer userId, Integer projectId) {
        String sql = String.format(
                "SELECT project_name FROM user%d.projects WHERE status = true AND project_id = ?",
                userId
        );

        try {
            String projectName = jdbcTemplate.queryForObject(sql, String.class, projectId);

            // В Go есть warning при пустом projectName
            if (projectName == null || projectName.trim().isEmpty()) {
                log.warn("Project name is empty for projectId: {}", projectId);
            }

            return projectName != null && !projectName.trim().isEmpty();
        } catch (EmptyResultDataAccessException e) {
            // Проект не найден (аналог sql.ErrNoRows в Go)
            return false;
        } catch (Exception e) {
            // Схема может не существовать или другие ошибки
            log.error("Error checking project ownership. UserId: {}, ProjectId: {}",
                    userId, projectId, e);
            return false;
        }
    }
}