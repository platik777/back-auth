package ru.platik777.backauth.repository;

import ru.platik777.backauth.entity.UserData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserDataRepository extends JpaRepository<UserData, Integer> {

    /**
     * Поиск данных пользователя по email
     */
    @Query("SELECT ud FROM UserData ud WHERE LOWER(ud.email) = LOWER(:email)")
    Optional<UserData> findByEmailIgnoreCase(@Param("email") String email);

    /**
     * Поиск данных пользователя по телефону
     */
    @Query("SELECT ud FROM UserData ud WHERE LOWER(ud.phone) = LOWER(:phone)")
    Optional<UserData> findByPhoneIgnoreCase(@Param("phone") String phone);

    /**
     * Проверка существования email
     */
    @Query("SELECT CASE WHEN COUNT(ud) > 0 THEN true ELSE false END FROM UserData ud WHERE LOWER(ud.email) = LOWER(:email)")
    boolean existsByEmailIgnoreCase(@Param("email") String email);

    /**
     * Проверка существования телефона
     */
    @Query("SELECT CASE WHEN COUNT(ud) > 0 THEN true ELSE false END FROM UserData ud WHERE LOWER(ud.phone) = LOWER(:phone)")
    boolean existsByPhoneIgnoreCase(@Param("phone") String phone);

    /**
     * Получение всех email адресов
     */
    @Query("SELECT ud.email FROM UserData ud")
    List<String> findAllEmails();

    /**
     * Получение всех телефонов
     */
    @Query("SELECT ud.phone FROM UserData ud")
    List<String> findAllPhones();
}