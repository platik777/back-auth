package ru.platik777.backauth.repository;

import ru.platik777.backauth.entity.ApiKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKey, Integer> {

    /**
     * Поиск API ключа по значению ключа
     */
    Optional<ApiKey> findByApiKey(String apiKey);

    /**
     * Поиск активных API ключей пользователя
     */
    List<ApiKey> findByUserIdAndIsDeletedFalse(Integer userId);

    /**
     * Поиск всех API ключей пользователя (включая удаленные)
     */
    List<ApiKey> findByUserId(Integer userId);

    /**
     * Проверка существования активного API ключа
     */
    @Query("SELECT CASE WHEN COUNT(ak) > 0 THEN true ELSE false END FROM ApiKey ak " +
            "WHERE ak.apiKey = :apiKey AND ak.isDeleted = false AND ak.user.id = :userId")
    boolean existsByApiKeyAndUserIdAndNotDeleted(@Param("apiKey") String apiKey, @Param("userId") Integer userId);

    /**
     * Проверка валидности API ключа (не удален и не истек)
     */
    @Query("SELECT CASE WHEN COUNT(ak) > 0 THEN true ELSE false END FROM ApiKey ak " +
            "WHERE ak.apiKey = :apiKey AND ak.isDeleted = false AND ak.expireAt > :now")
    boolean isApiKeyValid(@Param("apiKey") String apiKey, @Param("now") LocalDateTime now);

    /**
     * Мягкое удаление API ключа
     */
    @Modifying
    @Query("UPDATE ApiKey ak SET ak.isDeleted = true WHERE ak.apiKey = :apiKey AND ak.user.id = :userId AND ak.isDeleted = false")
    int softDeleteByApiKeyAndUserId(@Param("apiKey") String apiKey, @Param("userId") Integer userId);

    /**
     * Поиск API ключа с пользователем
     */
    @Query("SELECT ak FROM ApiKey ak " +
            "LEFT JOIN FETCH ak.user u " +
            "WHERE ak.apiKey = :apiKey AND ak.isDeleted = false")
    Optional<ApiKey> findByApiKeyWithUser(@Param("apiKey") String apiKey);

    /**
     * Очистка просроченных ключей
     */
    @Modifying
    @Query("UPDATE ApiKey ak SET ak.isDeleted = true WHERE ak.expireAt < :now AND ak.isDeleted = false")
    int markExpiredKeysAsDeleted(@Param("now") LocalDateTime now);
}