package ru.platik777.backauth.repository;

import ru.platik777.backauth.entity.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, Long> {

    /**
     * Поиск сессий пользователя
     */
    List<UserSession> findByUserId(Integer userId);

    /**
     * Поиск сессий пользователя с пагинацией (последние сессии)
     */
    @Query("SELECT us FROM UserSession us WHERE us.user.id = :userId ORDER BY us.createdAt DESC")
    List<UserSession> findByUserIdOrderByCreatedAtDesc(@Param("userId") Integer userId);

    /**
     * Поиск активных сессий за период
     */
    @Query("SELECT us FROM UserSession us WHERE us.user.id = :userId AND us.createdAt >= :since")
    List<UserSession> findByUserIdAndCreatedAtAfter(@Param("userId") Integer userId, @Param("since") LocalDateTime since);

    /**
     * Удаление старых сессий
     */
    @Modifying
    @Query("DELETE FROM UserSession us WHERE us.createdAt < :before")
    int deleteOldSessions(@Param("before") LocalDateTime before);

    /**
     * Удаление сессий пользователя
     */
    @Modifying
    @Query("DELETE FROM UserSession us WHERE us.user.id = :userId")
    int deleteByUserId(@Param("userId") Integer userId);

    /**
     * Подсчет сессий пользователя за период
     */
    @Query("SELECT COUNT(us) FROM UserSession us WHERE us.user.id = :userId AND us.createdAt >= :since")
    long countByUserIdAndCreatedAtAfter(@Param("userId") Integer userId, @Param("since") LocalDateTime since);
}