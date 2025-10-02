package ru.platik777.backauth.entity;

import lombok.Data;

/**
 * DTO для данных из таблицы userN.user_data
 *
 * ВАЖНО: Это НЕ JPA Entity, так как таблица находится в динамической схеме userN
 * Каждый пользователь имеет свою схему: user1.user_data, user2.user_data и т.д.
 *
 * В Go эта таблица создается динамически:
 * CREATE TABLE IF NOT EXISTS userN.user_data (
 *     login character varying(255) NOT NULL,
 *     password_hash text NOT NULL,
 *     email character varying(255),
 *     user_name character varying(255),
 *     phone character varying(30)
 * )
 */
@Data
public class UserData {
    /**
     * Логин пользователя (дублируется из public.users для удобства)
     */
    private String login;

    /**
     * Хеш пароля пользователя
     */
    private String passwordHash;

    /**
     * Email пользователя
     */
    private String email;

    /**
     * Имя пользователя
     */
    private String userName;

    /**
     * Телефон пользователя
     */
    private String phone;
}