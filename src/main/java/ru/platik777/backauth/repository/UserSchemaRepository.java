package ru.platik777.backauth.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;


// TODO: Избавится от этого
/**
 * Репозиторий для работы с пользовательскими схемами БД
 * Реализует логику создания схем userN для каждого пользователя
 */
@Repository
public class UserSchemaRepository {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public UserSchemaRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Создание схемы пользователя и всех необходимых таблиц
     */
    @Transactional
    public void createUserSchema(Integer userId, String login, String passwordHash,
                                 String email, String userName, String phone) {
        String schemaName = "user" + userId;

        // Создание схемы
        createSchema(schemaName);

        // Создание таблиц в схеме пользователя
        createUserDataTable(schemaName, login, passwordHash, email, userName, phone);
        createProjectsTable(schemaName);
        createDeltasTable(schemaName);
        createProjectFilesTable(schemaName);
        createUserSessionTable(schemaName);
        createUserMetricsTable(schemaName);
        createFoldersTable(schemaName);
        createFoldersTreepathTable(schemaName);
        createUserBlocksTable(schemaName);
        createUserCategoriesTable(schemaName);
        createUserCategoriesTreePathTable(schemaName);
        createUserImagesTable(schemaName);
    }

    private void createSchema(String schemaName) {
        String sql = "CREATE SCHEMA IF NOT EXISTS " + schemaName + " AUTHORIZATION repuser";
        jdbcTemplate.execute(sql);
    }

    private void createUserDataTable(String schemaName, String login, String passwordHash,
                                     String email, String userName, String phone) {
        // Создание таблицы
        String createTableSql = String.format("""
            CREATE TABLE IF NOT EXISTS %s.user_data (
                login character varying(255) COLLATE pg_catalog."default" NOT NULL,
                password_hash text COLLATE pg_catalog."default" NOT NULL,
                email character varying(255) COLLATE pg_catalog."default",
                user_name character varying(255) COLLATE pg_catalog."default",
                phone character varying(30) COLLATE pg_catalog."default"
            ) TABLESPACE pg_default
            """, schemaName);
        jdbcTemplate.execute(createTableSql);

        // Установка владельца
        String ownerSql = String.format("ALTER TABLE IF EXISTS %s.user_data OWNER to repuser", schemaName);
        jdbcTemplate.execute(ownerSql);

        // Вставка данных
        String insertSql = String.format("""
            INSERT INTO %s.user_data (login, password_hash, email, user_name, phone) 
            VALUES (?, ?, ?, ?, ?)
            """, schemaName);
        jdbcTemplate.update(insertSql, login, passwordHash, email, userName, phone);
    }

    private void createProjectsTable(String schemaName) {
        String sql = String.format("""
            CREATE TABLE IF NOT EXISTS %s.projects (
                project_id bigint NOT NULL,
                project_name character varying(255) COLLATE pg_catalog."default" NOT NULL,
                status boolean NOT NULL,
                role text COLLATE pg_catalog."default" NOT NULL,
                created_at timestamptz NOT NULL DEFAULT now(),
                favorites bool NOT NULL DEFAULT false,
                is_temp bool NOT NULL DEFAULT false,
                is_example bool NOT NULL DEFAULT false,
                folder int NULL DEFAULT 0,
                preview TEXT DEFAULT '',
                type TEXT DEFAULT 'project',
                CONSTRAINT "pk.project_id" PRIMARY KEY (project_id),
                CONSTRAINT project_id UNIQUE (project_id)
            ) TABLESPACE pg_default
            """, schemaName);
        jdbcTemplate.execute(sql);

        String ownerSql = String.format("ALTER TABLE IF EXISTS %s.projects OWNER to repuser", schemaName);
        jdbcTemplate.execute(ownerSql);
    }

    private void createDeltasTable(String schemaName) {
        String sql = String.format("""
            DROP TABLE IF EXISTS %s.deltas;
            CREATE TABLE IF NOT EXISTS %s.deltas (
                project_id INT NOT NULL,
                number_version int NOT NULL,
                comment text DEFAULT null,
                is_parent boolean DEFAULT false,
                delta JSONB NOT NULL,
                created_at TIMESTAMPTZ NOT NULL DEFAULT now()
            ) TABLESPACE pg_default;
            CREATE INDEX idx_project_version ON %s.deltas (project_id, number_version);
            """, schemaName, schemaName, schemaName);
        jdbcTemplate.execute(sql);

        String ownerSql = String.format("ALTER TABLE IF EXISTS %s.deltas OWNER to repuser", schemaName);
        jdbcTemplate.execute(ownerSql);
    }

    private void createProjectFilesTable(String schemaName) {
        String sql = String.format("""
            CREATE TABLE %s.project_files (
                uuid uuid NOT NULL DEFAULT gen_random_uuid(),
                created_at timestamptz NOT NULL DEFAULT now(),
                file_type text NOT NULL,
                content_type text NOT NULL,
                original_name text NOT NULL,
                size int8 NOT NULL,
                project_id int8 NOT NULL,
                element_id int8 NOT NULL DEFAULT 0,
                file BYTEA NOT NULL,
                CONSTRAINT project_files_pk PRIMARY KEY (project_id, file_type, element_id)
            ) TABLESPACE pg_default
            """, schemaName);
        jdbcTemplate.execute(sql);

        String ownerSql = String.format("ALTER TABLE IF EXISTS %s.project_files OWNER to repuser", schemaName);
        jdbcTemplate.execute(ownerSql);
    }

    private void createUserSessionTable(String schemaName) {
        String sql = String.format("""
            CREATE TABLE IF NOT EXISTS %s.user_session (
                id_session bigint NOT NULL GENERATED ALWAYS AS IDENTITY ( INCREMENT 1 START 1 MINVALUE 1 MAXVALUE 9223372036854775807 CACHE 1 ),
                agent text COLLATE pg_catalog."default" NOT NULL,
                created_at timestamptz NOT NULL DEFAULT now(),
                CONSTRAINT user_session_pkey PRIMARY KEY (id_session)
            ) TABLESPACE pg_default
            """, schemaName);
        jdbcTemplate.execute(sql);

        String ownerSql = String.format("ALTER TABLE IF EXISTS %s.user_session OWNER to repuser", schemaName);
        jdbcTemplate.execute(ownerSql);
    }

    private void createUserMetricsTable(String schemaName) {
        String sql = String.format("""
            CREATE TABLE IF NOT EXISTS %s.user_metrics (
                metrics_id bigint NOT NULL GENERATED ALWAYS AS IDENTITY ( INCREMENT 1 START 1 MINVALUE 1 MAXVALUE 9223372036854775807 CACHE 1 ),
                start_time timestamptz NOT NULL DEFAULT now(),
                stop_time timestamptz NOT NULL DEFAULT now(),
                duration bigint NOT NULL DEFAULT 0,
                CONSTRAINT metrics_pkey PRIMARY KEY (metrics_id)
            ) TABLESPACE pg_default
            """, schemaName);
        jdbcTemplate.execute(sql);

        String ownerSql = String.format("ALTER TABLE IF EXISTS %s.user_metrics OWNER to repuser", schemaName);
        jdbcTemplate.execute(ownerSql);
    }

    private void createFoldersTable(String schemaName) {
        String sql = String.format("""
            CREATE TABLE IF NOT EXISTS %s.folders (
                id SERIAL,
                name TEXT NOT NULL,
                created_at timestamptz NOT NULL DEFAULT now(),
                updated_at timestamptz NOT NULL DEFAULT now()
            ) TABLESPACE pg_default
            """, schemaName);
        jdbcTemplate.execute(sql);

        String ownerSql = String.format("ALTER TABLE IF EXISTS %s.folders OWNER to repuser", schemaName);
        jdbcTemplate.execute(ownerSql);
    }

    private void createFoldersTreepathTable(String schemaName) {
        String sql = String.format("""
            CREATE TABLE IF NOT EXISTS %s.folders_treepath (
                id SERIAL,
                ancestor INT NOT NULL DEFAULT 0,
                descendant INT NOT NULL DEFAULT 0
            ) TABLESPACE pg_default
            """, schemaName);
        jdbcTemplate.execute(sql);

        String ownerSql = String.format("ALTER TABLE IF EXISTS %s.folders_treepath OWNER to repuser", schemaName);
        jdbcTemplate.execute(ownerSql);
    }

    private void createUserBlocksTable(String schemaName) {
        String sql = String.format("""
            CREATE TABLE IF NOT EXISTS %s.blocks (
                id uuid NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
                project_id bigint NOT NULL,
                type TEXT NOT NULL DEFAULT 'UserBlock',
                name TEXT NOT NULL,
                block_json JSONB NOT NULL,
                is_deleted boolean NOT NULL,
                category_id uuid NOT NULL,
                CONSTRAINT project_id UNIQUE (project_id)
            ) TABLESPACE pg_default
            """, schemaName);
        jdbcTemplate.execute(sql);

        String ownerSql = String.format("ALTER TABLE IF EXISTS %s.blocks OWNER to repuser", schemaName);
        jdbcTemplate.execute(ownerSql);
    }

    private void createUserCategoriesTable(String schemaName) {
        String sql = String.format("""
            CREATE TABLE IF NOT EXISTS %s.categories (
                id uuid NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
                name VARCHAR(255) NOT NULL
            ) TABLESPACE pg_default
            """, schemaName);
        jdbcTemplate.execute(sql);

        String ownerSql = String.format("ALTER TABLE IF EXISTS %s.categories OWNER to repuser", schemaName);
        jdbcTemplate.execute(ownerSql);
    }

    private void createUserCategoriesTreePathTable(String schemaName) {
        String sql = String.format("""
            CREATE TABLE IF NOT EXISTS %s.categories_treepath (
                id SERIAL NOT NULL,
                ancestor uuid,
                descendant uuid,
                FOREIGN KEY (ancestor) REFERENCES %s.categories(id) ON DELETE CASCADE,
                FOREIGN KEY (descendant) REFERENCES %s.categories(id) ON DELETE CASCADE
            ) TABLESPACE pg_default
            """, schemaName, schemaName, schemaName);
        jdbcTemplate.execute(sql);

        String ownerSql = String.format("ALTER TABLE IF EXISTS %s.categories_treepath OWNER to repuser", schemaName);
        jdbcTemplate.execute(ownerSql);
    }

    private void createUserImagesTable(String schemaName) {
        String sql = String.format("""
            CREATE TABLE IF NOT EXISTS %s.images (
                id uuid NOT NULL DEFAULT gen_random_uuid(),
                block_id uuid NOT NULL,
                is_active bool NOT NULL DEFAULT true,
                state varchar(255),
                file TEXT NOT NULL,
                FOREIGN KEY (block_id) REFERENCES %s.blocks(id) ON DELETE CASCADE
            ) TABLESPACE pg_default
            """, schemaName, schemaName);
        jdbcTemplate.execute(sql);

        String ownerSql = String.format("ALTER TABLE IF EXISTS %s.images OWNER to repuser", schemaName);
        jdbcTemplate.execute(ownerSql);
    }

    /**
     * Проверка существования схемы пользователя
     */
    public boolean userSchemaExists(Integer userId) {
        String sql = "SELECT COUNT(*) FROM information_schema.schemata WHERE schema_name = ?";
        String schemaName = "user" + userId;
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, schemaName);
        return count != null && count > 0;
    }

    /**
     * Удаление схемы пользователя
     */
    @Transactional
    public void dropUserSchema(Integer userId) {
        String schemaName = "user" + userId;
        String sql = "DROP SCHEMA IF EXISTS " + schemaName + " CASCADE";
        jdbcTemplate.execute(sql);
    }
}