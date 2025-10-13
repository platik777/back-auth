package ru.platik777.backauth.repository;

import ru.platik777.backauth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.platik777.backauth.entity.types.AccountType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;


@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * GetUserByLogin / SearchUserIdByLogin
     */
    Optional<User> findByLoginIgnoreCase(@Param("login") String login);

    /**
     * FieldValueUniqueness (login)
     */
    boolean existsByLoginIgnoreCase(@Param("login") String login);

    /**
     * SearchUserIdByLogin
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