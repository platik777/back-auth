package ru.platik777.backauth.repository;

import ru.platik777.backauth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Репозиторий для работы с таблицей public.users
 * Соответствует методам из PostgreSqlAuth.go
 *
 * ВАЖНО: Этот репозиторий работает ТОЛЬКО с public.users
 * Для работы с userN.user_data используйте UserDataRepository
 */
@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

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
     * getUserIdsList - получение всех ID пользователей
     * Go: SELECT user_id FROM public.users
     *
     * Используется в SearchUserIdByEmail и SearchUserIdByPhone
     * для перебора всех схем userN.user_data
     */
    List<Integer> findAllUserIds();

    /**
     * GetCompaniesOfUser - получение пользователя со всеми компаниями
     * Go: используется в getCompany для получения owner
     */
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.ownedCompanies WHERE u.id = :userId")
    Optional<User> findByIdWithCompanies(@Param("userId") Integer userId);

    /**
     * GetStudentDataByUserId - получение пользователя со студенческими данными
     */
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.student WHERE u.id = :userId")
    Optional<User> findByIdWithStudent(@Param("userId") Integer userId);

    /**
     * Получение пользователей по типу аккаунта
     * Может использоваться для фильтрации по account_type
     */
    List<User> findByAccountType(@Param("accountType") User.AccountType accountType);
}