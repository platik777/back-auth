package ru.platik777.backauth.mapper;

import org.springframework.stereotype.Component;
import ru.platik777.backauth.dto.request.SignUpRequest;
import ru.platik777.backauth.entity.Student;

/**
 * Маппер для преобразования DTO в Student Entity
 */
@Component
public class StudentMapper {

    /**
     * Маппинг StudentDto в Student Entity
     */
    public Student toEntity(SignUpRequest.StudentDto dto) {
        if (dto == null) {
            return null;
        }

        Student student = new Student();
        student.setStartYear(dto.getStartYear());
        student.setEndYear(dto.getEndYear());
        student.setStudentId(dto.getStudentId());
        student.setEducationalInstitutionsUuid(dto.getEducationalInstitutionsUuid());

        return student;
    }
}