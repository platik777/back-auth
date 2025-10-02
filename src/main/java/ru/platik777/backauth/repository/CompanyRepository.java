package ru.platik777.backauth.repository;

import ru.platik777.backauth.entity.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CompanyRepository extends JpaRepository<Company, Integer> {

    /**
     * GetCompaniesOfUser - получение компаний пользователя по owner_id
     * Соответствует методу GetCompaniesOfUser из Go
     */
    List<Company> findByOwnerId(Integer ownerId);

    /**
     * getCompany - получение компании с владельцем
     * Используется в GetCompaniesOfUser для получения полных данных
     */
    @Query("SELECT c FROM Company c " +
            "LEFT JOIN FETCH c.owner " +
            "WHERE c.companyId = :companyId")
    Optional<Company> findByIdWithOwner(@Param("companyId") Integer companyId);

    // Методы save(), findById() наследуются от JpaRepository
    // и соответствуют createCompany, getCompany из Go
}