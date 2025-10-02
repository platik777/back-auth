package ru.platik777.backauth.repository;

import ru.platik777.backauth.entity.ApiKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKey, Integer> {

    /**
     * GetApiKeys - получение активных API ключей пользователя
     */
    List<ApiKey> findByUserIdAndIsDeletedFalse(@Param("userId") Integer userId);

    /**
     * CheckApiKey - проверка существования активного API ключа
     */
    boolean existsByApiKeyAndUserIdAndIsDeletedFalse(@Param("apiKey") String apiKey, @Param("userId") Integer userId);

    /**
     * DeleteApiKey - мягкое удаление API ключа
     */
    @Modifying
    @Query("UPDATE ApiKey ak SET ak.isDeleted = true " +
            "WHERE ak.apiKey = :apiKey AND ak.user.id = :userId AND ak.isDeleted = false")
    int softDeleteByApiKeyAndUserId(@Param("apiKey") String apiKey, @Param("userId") Integer userId);

    // AddApiKey реализуется через save() из JpaRepository
}