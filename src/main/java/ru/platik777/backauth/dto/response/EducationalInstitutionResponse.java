package ru.platik777.backauth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.platik777.backauth.entity.EducationalInstitution;

import java.time.LocalDateTime;

/**
 * DTO образовательного учреждения для ответа
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EducationalInstitutionResponse {
    private String uuid;
    private String institutionId;
    private String fullName;
    private String shortName;
    private String email;
    private String phone;
    private String webSite;
    private String inn;
    private String kpp;
    private String ogrn;
    private Boolean isActive;
    private LocalDateTime issueDate;
    private LocalDateTime endDate;

    public static EducationalInstitutionResponse fromEntity(EducationalInstitution entity) {
        return EducationalInstitutionResponse.builder()
                .uuid(entity.getUuid())
                .institutionId(entity.getInstitutionId())
                .fullName(entity.getFullName())
                .shortName(entity.getShortName())
                .email(entity.getEmail())
                .phone(entity.getPhone())
                .webSite(entity.getWebSite())
                .inn(entity.getInn())
                .kpp(entity.getKpp())
                .ogrn(entity.getOgrn())
                .isActive(entity.getIsActive())
                .issueDate(entity.getIssueDate())
                .endDate(entity.getEndDate())
                .build();
    }
}
