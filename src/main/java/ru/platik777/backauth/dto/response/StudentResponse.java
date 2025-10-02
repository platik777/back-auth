package ru.platik777.backauth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.platik777.backauth.entity.Student;

import java.time.LocalDateTime;

/**
 * DTO студента для ответа
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentResponse {
    private String uuid;
    private Integer userId;
    private Integer startYear;
    private Integer endYear;
    private String studentId;
    private String educationalInstitutionsUuid;
    private LocalDateTime createdAt;
    private LocalDateTime lastConfirmation;

    public static StudentResponse fromStudent(Student student) {
        return StudentResponse.builder()
                .uuid(student.getUuid() != null ? student.getUuid().toString() : null)
                .userId(student.getUser() != null ? student.getUser().getId() : null)
                .startYear(student.getStartYear())
                .endYear(student.getEndYear())
                .studentId(student.getStudentId())
                .educationalInstitutionsUuid(student.getEducationalInstitutionsUuid())
                .createdAt(student.getCreatedAt())
                .lastConfirmation(student.getLastConfirmation())
                .build();
    }
}
