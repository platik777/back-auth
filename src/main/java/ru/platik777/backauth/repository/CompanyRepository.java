package ru.platik777.backauth.repository;

import ru.platik777.backauth.entity.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CompanyRepository extends JpaRepository<Company, Integer> {

    /**
     * Поиск компаний по владельцу
     */
    List<Company> findByOwnerId(Integer ownerId);

    /**
     * Поиск компаний где пользователь является участником
     */
    @Query(value = "SELECT c.* FROM company c WHERE c.users_list::jsonb ? :userId::text", nativeQuery = true)
    List<Company> findByUserInUsersList(@Param("userId") String userId);

    /**
     * Поиск компаний по ИНН
     */
    List<Company> findByTaxNumber(String taxNumber);

    /**
     * Поиск компаний по ОГРН
     */
    List<Company> findByOgrn(String ogrn);

    /**
     * Поиск компаний по ОГРНИП
     */
    List<Company> findByOgrnip(String ogrnip);

    /**
     * Получение компании с владельцем
     */
    @Query("SELECT c FROM Company c " +
            "LEFT JOIN FETCH c.owner " +
            "WHERE c.companyId = :companyId")
    Company findByIdWithOwner(@Param("companyId") Integer companyId);
}