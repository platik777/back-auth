package ru.platik777.backauth.repository;

import ru.platik777.backauth.entity.ApiKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKey, UUID> {

    /**
     * GetApiKeys - получение активных API ключей пользователя
     */
    List<ApiKey> findByUserIdAndIsDeletedFalse(UUID user_id);

    /**
     * CheckApiKey - проверка существования активного API ключа
     */
    boolean existsByApiKeyAndUserIdAndIsDeletedFalse(String apiKey, UUID user_id);

    /**
     * DeleteApiKey - мягкое удаление API ключа
     */
    @Modifying
    @Query("UPDATE ApiKey ak SET ak.isDeleted = true " +
            "WHERE ak.apiKey = :apiKey AND ak.user.id = :userId AND ak.isDeleted = false")
    int softDeleteByApiKeyAndUserId(@Param("apiKey") String apiKey, @Param("userId") UUID userId);
}