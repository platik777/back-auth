package ru.platik777.backauth.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class SaltRepository {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public SaltRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Получение значения соли для генерации ключей
     */
    public String getSalt() {
        String sql = "SELECT salt_value FROM salt_values_table LIMIT 1";
        return jdbcTemplate.queryForObject(sql, String.class);
    }

    /**
     * Обновление значения соли
     */
    public void updateSalt(String saltValue) {
        String sql = "UPDATE salt_values_table SET salt_value = ? WHERE id = 1";
        jdbcTemplate.update(sql, saltValue);
    }

    /**
     * Проверка существования записи с солью
     */
    public boolean saltExists() {
        String sql = "SELECT COUNT(*) FROM salt_values_table";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class);
        return count != null && count > 0;
    }
}