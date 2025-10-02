package ru.platik777.backauth.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.platik777.backauth.entity.UserData;
import ru.platik777.backauth.entity.User;

import java.util.List;
import java.util.Optional;

/**
 * Репозиторий для работы с динамическими таблицами userN.user_data
 * Соответствует методам из PostgreSqlAuth.go
 *
 * ВАЖНО: Каждый пользователь имеет свою схему userN
 * Эти таблицы создаются динамически при создании пользователя
 *
 * В Go есть только одна структура models.User, которая содержит:
 * - Поля из public.users (хранятся в User entity)
 * - Поля из userN.user_data (хранятся в @Transient полях User)
 *
 * Методы этого репозитория заполняют @Transient поля User из userN.user_data
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class UserDataRepository {

    private final JdbcTemplate jdbcTemplate;
    private final UserRepository userRepository;

    /**
     * GetUser - получение полных данных пользователя
     * Go: читает из public.users + userN.user_data и возвращает models.User
     *
     * @param userId ID пользователя
     * @return пользователь с заполненными transient полями из userN.user_data
     */
    public Optional<User> getUserWithData(Integer userId) {
        try {
            // 1. Получаем данные из public.users
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty()) {
                log.warn("User not found in public.users. UserId: {}", userId);
                return Optional.empty();
            }
            User user = userOpt.get();

            // 2. Получаем данные из userN.user_data и заполняем transient поля
            String sql = String.format(
                    "SELECT password_hash, email, user_name, phone FROM user%d.user_data",
                    userId
            );

            jdbcTemplate.queryForObject(sql, (rs, rowNum) -> {
                user.setPasswordHash(rs.getString("password_hash"));
                user.setEmail(rs.getString("email"));
                user.setUserName(rs.getString("user_name"));
                user.setPhone(rs.getString("phone"));
                return null;
            });

            // 3. Роли и разрешения будут заполнены в Service через SDK

            return Optional.of(user);

        } catch (EmptyResultDataAccessException e) {
            log.error("User data not found in userN.user_data. UserId: {}", userId);
            return Optional.empty();
        } catch (Exception e) {
            log.error("Error getting user data. UserId: {}", userId, e);
            return Optional.empty();
        }
    }

    /**
     * GetUserByLogin - получение полных данных пользователя по логину
     * Go: читает из public.users WHERE login=$1, затем из userN.user_data
     *
     * @param login логин пользователя (case-insensitive)
     * @return пользователь с заполненными transient полями
     */
    public Optional<User> getUserByLogin(String login) {
        Optional<User> userOpt = userRepository.findByLoginIgnoreCase(login);
        if (userOpt.isEmpty()) {
            log.warn("User not found by login: {}", login);
            return Optional.empty();
        }

        return getUserWithData(userOpt.get().getId());
    }

    /**
     * SearchUserIdByEmail - поиск ID пользователя по email
     * Go: перебирает все схемы userN.user_data и ищет email
     *
     * @param email email пользователя (case-insensitive)
     * @return ID пользователя или null
     */
    public Integer findUserIdByEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return null;
        }

        String emailLower = email.toLowerCase();
        List<Integer> userIds = userRepository.findAllUserIds();

        log.debug("Searching email '{}' in {} user schemas", email, userIds.size());

        for (Integer userId : userIds) {
            try {
                String sql = String.format("SELECT email FROM user%d.user_data", userId);
                String emailFromDb = jdbcTemplate.queryForObject(sql, String.class);

                if (emailFromDb != null && emailFromDb.toLowerCase().equals(emailLower)) {
                    log.debug("Email found in user{}.user_data", userId);
                    return userId;
                }
            } catch (EmptyResultDataAccessException e) {
                // Таблица существует, но пустая - пропускаем
                continue;
            } catch (Exception e) {
                // Схема может не существовать или другие ошибки - пропускаем
                log.trace("Error checking user{}.user_data: {}", userId, e.getMessage());
                continue;
            }
        }

        log.debug("Email '{}' not found in any user schema", email);
        return null;
    }

    /**
     * SearchUserIdByPhone - поиск ID пользователя по телефону
     * Go: перебирает все схемы userN.user_data и ищет phone
     *
     * @param phone телефон пользователя (case-insensitive)
     * @return ID пользователя или null
     */
    public Integer findUserIdByPhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return null;
        }

        String phoneLower = phone.toLowerCase();
        List<Integer> userIds = userRepository.findAllUserIds();

        log.debug("Searching phone '{}' in {} user schemas", phone, userIds.size());

        for (Integer userId : userIds) {
            try {
                String sql = String.format("SELECT phone FROM user%d.user_data", userId);
                String phoneFromDb = jdbcTemplate.queryForObject(sql, String.class);

                if (phoneFromDb != null && phoneFromDb.toLowerCase().equals(phoneLower)) {
                    log.debug("Phone found in user{}.user_data", userId);
                    return userId;
                }
            } catch (EmptyResultDataAccessException ignored) {
            } catch (Exception e) {
                log.trace("Error checking user{}.user_data: {}", userId, e.getMessage());
            }
        }

        log.debug("Phone '{}' not found in any user schema", phone);
        return null;
    }

    /**
     * FieldValueUniqueness - проверка уникальности email
     * Go: emailUniqueness - перебирает все схемы и проверяет email
     *
     * @param email email для проверки
     * @return true если email уникален (не найден)
     */
    public boolean isEmailUnique(String email) {
        return findUserIdByEmail(email) == null;
    }

    /**
     * FieldValueUniqueness - проверка уникальности телефона
     * Go: phoneUniqueness - перебирает все схемы и проверяет phone
     *
     * @param phone телефон для проверки
     * @return true если телефон уникален (не найден)
     */
    public boolean isPhoneUnique(String phone) {
        return findUserIdByPhone(phone) == null;
    }

    /**
     * EditUser - обновление данных пользователя в userN.user_data
     * Go: обновляет password_hash, email, user_name, phone в userN.user_data
     *
     * @param userId ID пользователя
     * @param userData новые данные (null значения не обновляются)
     */
    public void updateUserData(Integer userId, UserData userData) {
        if (userData == null) {
            throw new IllegalArgumentException("UserData cannot be null");
        }

        // Обновление пароля
        if (userData.getPasswordHash() != null && !userData.getPasswordHash().isEmpty()) {
            String sql = String.format("UPDATE user%d.user_data SET password_hash = ?", userId);
            jdbcTemplate.update(sql, userData.getPasswordHash());
            log.debug("Password updated for userId: {}", userId);
        }

        // Обновление email
        if (userData.getEmail() != null && !userData.getEmail().isEmpty()) {
            String sql = String.format("UPDATE user%d.user_data SET email = ?", userId);
            jdbcTemplate.update(sql, userData.getEmail());
            log.debug("Email updated for userId: {}", userId);
        }

        // Обновление имени
        if (userData.getUserName() != null && !userData.getUserName().isEmpty()) {
            String sql = String.format("UPDATE user%d.user_data SET user_name = ?", userId);
            jdbcTemplate.update(sql, userData.getUserName());
            log.debug("UserName updated for userId: {}", userId);
        }

        // Обновление телефона
        if (userData.getPhone() != null && !userData.getPhone().isEmpty()) {
            String sql = String.format("UPDATE user%d.user_data SET phone = ?", userId);
            jdbcTemplate.update(sql, userData.getPhone());
            log.debug("Phone updated for userId: {}", userId);
        }
    }

    /**
     * createUserDataTable - создание таблицы userN.user_data
     * Go: CREATE TABLE IF NOT EXISTS userN.user_data (...)
     *
     * Вызывается при создании нового пользователя
     */
    public void createUserDataTable(Integer userId, String login, String passwordHash,
                                    String email, String userName, String phone) {
        try {
            // 1. Создание таблицы
            String createTableSql = String.format(
                    """
                    CREATE TABLE IF NOT EXISTS user%d.user_data (
                        login character varying(255) NOT NULL,
                        password_hash text NOT NULL,
                        email character varying(255),
                        user_name character varying(255),
                        phone character varying(30)
                    )
                    """,
                    userId
            );
            jdbcTemplate.execute(createTableSql);
            log.debug("Created table user{}.user_data", userId);

            // 2. Установка владельца таблицы
            String alterTableSql = String.format(
                    "ALTER TABLE IF EXISTS user%d.user_data OWNER to repuser",
                    userId
            );
            jdbcTemplate.execute(alterTableSql);

            // 3. Вставка данных
            String insertSql = String.format(
                    "INSERT INTO user%d.user_data (login, password_hash, email, user_name, phone) VALUES (?, ?, ?, ?, ?)",
                    userId
            );
            jdbcTemplate.update(insertSql, login, passwordHash, email, userName, phone);
            log.debug("Inserted user data into user{}.user_data", userId);

        } catch (Exception e) {
            log.error("Error creating user_data table for userId: {}", userId, e);
            throw new RuntimeException("Failed to create user_data table", e);
        }
    }

    /**
     * Получение UserData по userId (без данных из public.users)
     */
    public Optional<UserData> getUserData(Integer userId) {
        try {
            String sql = String.format(
                    "SELECT login, password_hash, email, user_name, phone FROM user%d.user_data",
                    userId
            );

            UserData userData = jdbcTemplate.queryForObject(sql, (rs, rowNum) -> {
                UserData data = new UserData();
                data.setLogin(rs.getString("login"));
                data.setPasswordHash(rs.getString("password_hash"));
                data.setEmail(rs.getString("email"));
                data.setUserName(rs.getString("user_name"));
                data.setPhone(rs.getString("phone"));
                return data;
            });

            return Optional.ofNullable(userData);

        } catch (Exception e) {
            log.error("Error getting user data for userId: {}", userId, e);
            return Optional.empty();
        }
    }
}