package ru.platik777.backauth.repository;

import ru.platik777.backauth.entity.EducationalInstitution;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EducationalInstitutionRepository extends JpaRepository<EducationalInstitution, String> {

    /**
     * Поиск образовательных учреждений по полному названию (частичное совпадение)
     */
    @Query("SELECT ei FROM EducationalInstitution ei WHERE LOWER(ei.fullName) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<EducationalInstitution> findByFullNameContainingIgnoreCase(@Param("name") String name);

    /**
     * Поиск образовательных учреждений по полному названию с пагинацией
     */
    @Query("SELECT ei FROM EducationalInstitution ei WHERE LOWER(ei.fullName) LIKE LOWER(CONCAT('%', :name, '%'))")
    Page<EducationalInstitution> findByFullNameContainingIgnoreCase(@Param("name") String name, Pageable pageable);

    /**
     * Поиск активных образовательных учреждений
     */
    List<EducationalInstitution> findByIsActiveTrue();

    /**
     * Поиск образовательных учреждений по региону
     */
    List<EducationalInstitution> findByRegionNameIgnoreCase(String regionName);

    /**
     * Поиск образовательных учреждений по типу
     */
    List<EducationalInstitution> findByTypeNameIgnoreCase(String typeName);

    /**
     * Поиск по ИНН
     */
    Optional<EducationalInstitution> findByInn(String inn);

    /**
     * Поиск по ОГРН
     */
    Optional<EducationalInstitution> findByOgrn(String ogrn);

    /**
     * Проверка существования по UUID
     */
    boolean existsByUuid(String uuid);

    List<EducationalInstitution> findByTypeName(String typeName);
}