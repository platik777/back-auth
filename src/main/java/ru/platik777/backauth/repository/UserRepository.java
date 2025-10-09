package ru.platik777.backauth.repository;

import ru.platik777.backauth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.platik777.backauth.entity.types.AccountType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Репозиторий для работы с таблицей public.users
 * Соответствует методам из PostgreSqlAuth.go
 *
 * ВАЖНО: Этот репозиторий работает ТОЛЬКО с public.users
 * Для работы с userN.user_data используйте UserDataRepository
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * GetUserByLogin / SearchUserIdByLogin
     * Go: SELECT user_id, login, billing_id, created_at, account_type, settings
     *     FROM public.users WHERE login=$1
     */
    Optional<User> findByLoginIgnoreCase(@Param("login") String login);

    /**
     * FieldValueUniqueness (login)
     * Go: SELECT CASE WHEN EXISTS(SELECT * FROM public.users WHERE login=$1)
     *     THEN 'false' ELSE 'true' END
     */
    boolean existsByLoginIgnoreCase(@Param("login") String login);

    /**
     * SearchUserIdByLogin
     * Go: SELECT user_id FROM public.users WHERE login=$1
     */
    Optional<Integer> findUserIdByLogin(@Param("login") String login);

    /**
     * Получение пользователей по типу аккаунта
     * Может использоваться для фильтрации по account_type
     */
    List<User> findByAccountType(@Param("accountType") AccountType accountType);

    boolean existsByPhone(String phone);

    boolean existsByEmail(String email);

    Optional<User> findByLogin(String login);

    Optional<User> findByEmail(String email);
}