package ru.platik777.backauth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Ответ на поиск образовательных учреждений
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EducationalInstitutionSearchResponse {
    private Boolean status;
    private List<EducationalInstitutionResponse> educationalInstitutions;
}
