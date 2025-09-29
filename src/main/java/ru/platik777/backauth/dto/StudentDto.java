package ru.platik777.backauth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import ru.platik777.backauth.entity.Student;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StudentDto {

    private UUID uuid;

    @JsonProperty("createdAt")
    private LocalDateTime createdAt;

    @JsonProperty("lastConfirmation")
    private LocalDateTime lastConfirmation;

    private Integer user;

    @JsonProperty("startYear")
    private Integer startYear;

    @JsonProperty("endYear")
    private Integer endYear;

    @JsonProperty("studentId")
    private String studentId;

    @JsonProperty("educationalInstitutionsUuid")
    private String educationalInstitutionsUuid;

    // Конструкторы
    public StudentDto() {}

    public StudentDto(Student student) {
        this.uuid = student.getUuid();
        this.createdAt = student.getCreatedAt();
        this.lastConfirmation = student.getLastConfirmation();
        this.user = student.getUser().getId();
        this.startYear = student.getStartYear();
        this.endYear = student.getEndYear();
        this.studentId = student.getStudentId();
        this.educationalInstitutionsUuid = student.getEducationalInstitutionsUuid();
    }
}