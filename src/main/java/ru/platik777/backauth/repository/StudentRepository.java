package ru.platik777.backauth.repository;

import ru.platik777.backauth.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Репозиторий для работы со студентами
 */
@Repository
public interface StudentRepository extends JpaRepository<Student, UUID> {

    /**
     * GetStudentDataByUserId - получение данных студента по ID пользователя
     * Go: SELECT ... FROM students WHERE user_id=$1
     */
    Optional<Student> findByUserId(Integer userId);

    // createStudent реализуется через save() из JpaRepository
}