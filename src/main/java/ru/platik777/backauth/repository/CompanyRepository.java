package ru.platik777.backauth.repository;

import ru.platik777.backauth.entity.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CompanyRepository extends JpaRepository<Tenant, UUID> {

    /**
     * GetCompaniesOfUser - получение компаний пользователя по owner_id
     * Соответствует методу GetCompaniesOfUser из Go
     */
    List<Tenant> findByOwnerId(UUID ownerId);
}