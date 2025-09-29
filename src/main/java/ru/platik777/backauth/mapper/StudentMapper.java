package ru.platik777.backauth.mapper;

import ru.platik777.backauth.dto.StudentDto;
import ru.platik777.backauth.dto.request.SignUpRequestDto;
import ru.platik777.backauth.entity.Student;
import ru.platik777.backauth.entity.User;
import org.springframework.stereotype.Component;

@Component
public class StudentMapper {

    public StudentDto toDto(Student student) {
        return new StudentDto(student);
    }

    public Student toEntity(SignUpRequestDto requestDto, User user) {
        Student student = new Student();
        student.setUser(user);
        student.setStartYear(requestDto.getStartYear());
        student.setEndYear(requestDto.getEndYear());
        student.setStudentId(requestDto.getStudentId());
        student.setEducationalInstitutionsUuid(requestDto.getEducationalInstitutionsUuid());

        return student;
    }
}