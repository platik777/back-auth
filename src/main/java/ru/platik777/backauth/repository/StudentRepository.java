package ru.platik777.backauth.repository;

import ru.platik777.backauth.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface StudentRepository extends JpaRepository<Student, UUID> {

    /**
     * Поиск студента по ID пользователя
     */
    Optional<Student> findByUserId(Integer userId);

    /**
     * Поиск студента по студенческому билету
     */
    Optional<Student> findByStudentId(String studentId);

    /**
     * Поиск студентов по образовательному учреждению
     */
    java.util.List<Student> findByEducationalInstitutionsUuid(String educationalInstitutionsUuid);

    /**
     * Поиск студента с пользователем по ID пользователя
     */
    @Query("SELECT s FROM Student s " +
            "LEFT JOIN FETCH s.user u " +
            "LEFT JOIN FETCH u.userData ud " +
            "WHERE s.user.id = :userId")
    Optional<Student> findByUserIdWithUser(@Param("userId") Integer userId);

    /**
     * Поиск студента с образовательным учреждением
     */
    @Query("SELECT s FROM Student s " +
            "LEFT JOIN FETCH s.educationalInstitution ei " +
            "WHERE s.uuid = :uuid")
    Optional<Student> findByUuidWithEducationalInstitution(@Param("uuid") UUID uuid);

    /**
     * Проверка существования студента по пользователю
     */
    boolean existsByUserId(Integer userId);
}