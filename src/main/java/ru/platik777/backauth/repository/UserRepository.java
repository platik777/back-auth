package ru.platik777.backauth.repository;

import ru.platik777.backauth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

    /**
     * Поиск пользователя по логину
     */
    Optional<User> findByLoginIgnoreCase(String login);

    /**
     * Проверка существования пользователя по логину
     */
    boolean existsByLoginIgnoreCase(String login);

    /**
     * Поиск пользователя с полными данными по логину
     */
    @Query("SELECT u FROM User u " +
            "LEFT JOIN FETCH u.userData ud " +
            "WHERE LOWER(u.login) = LOWER(:login)")
    Optional<User> findByLoginWithUserData(@Param("login") String login);

    /**
     * Поиск пользователя с полными данными по ID
     */
    @Query("SELECT u FROM User u " +
            "LEFT JOIN FETCH u.userData ud " +
            "WHERE u.id = :id")
    Optional<User> findByIdWithUserData(@Param("id") Integer id);

    /**
     * Получение всех ID пользователей
     */
    @Query("SELECT u.id FROM User u")
    java.util.List<Integer> findAllUserIds();
}