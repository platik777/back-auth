package ru.platik777.backauth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Полный ответ данных студента
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentFullResponse {
    private UserResponse userData;
    private StudentResponse studentData;
    private EducationalInstitutionResponse educationalInstitutionData;
}
