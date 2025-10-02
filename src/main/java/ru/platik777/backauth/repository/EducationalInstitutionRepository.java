package ru.platik777.backauth.repository;

import ru.platik777.backauth.entity.EducationalInstitution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Репозиторий для работы с образовательными учреждениями
 * Соответствует методам из PostgreSqlEducationalInstitutions.go
 */
@Repository
public interface EducationalInstitutionRepository extends JpaRepository<EducationalInstitution, String> {

    /**
     * FindEducationalInstitution - поиск учреждений по названию
     * Go: WHERE full_name ILIKE '%name%'
     */
    @Query("SELECT ei FROM EducationalInstitution ei " +
            "WHERE LOWER(ei.fullName) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<EducationalInstitution> findByFullNameContainingIgnoreCase(@Param("name") String name);

    // GetEducationalInstitution реализуется через findById() из JpaRepository
    // Go: WHERE educational_institutions_uuid=$1
}