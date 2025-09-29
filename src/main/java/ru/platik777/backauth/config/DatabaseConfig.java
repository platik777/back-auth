package ru.platik777.backauth.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.hibernate.type.format.jackson.JacksonJsonFormatMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;

@Configuration
@EnableJpaRepositories(basePackages = "ru.platik777.backauth.repository")
@EnableJpaAuditing
@EnableTransactionManagement
public class DatabaseConfig {

    /**
     * Настройка JdbcTemplate для нативных SQL запросов
     */
    @Bean
    @ConditionalOnMissingBean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    /**
     * Настройка ObjectMapper для JSON полей в PostgreSQL
     */
    @Bean
    public JacksonJsonFormatMapper jsonFormatMapper(ObjectMapper objectMapper) {
        return new JacksonJsonFormatMapper(objectMapper);
    }
}