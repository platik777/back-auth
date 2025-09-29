package ru.platik777.backauth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import ru.platik777.backauth.entity.EducationalInstitution;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EducationalInstitutionDto {

    private String uuid;

    @JsonProperty("isActive")
    private Boolean isActive;

    @JsonProperty("issueDate")
    private LocalDateTime issueDate;

    @JsonProperty("endDate")
    private LocalDateTime endDate;

    private String id;

    @JsonProperty("fullName")
    private String fullName;

    @JsonProperty("shortName")
    private String shortName;

    @JsonProperty("headEduOrgId")
    private String headEduOrgId;

    @JsonProperty("isBranch")
    private Integer isBranch;

    @JsonProperty("postAddress")
    private String postAddress;

    private String phone;
    private String fax;
    private String email;

    @JsonProperty("webSite")
    private String webSite;

    private String ogrn;
    private String inn;
    private String kpp;

    @JsonProperty("headPost")
    private String headPost;

    @JsonProperty("headName")
    private String headName;

    @JsonProperty("formName")
    private String formName;

    @JsonProperty("kindName")
    private String kindName;

    @JsonProperty("typeName")
    private String typeName;

    @JsonProperty("regionName")
    private String regionName;

    @JsonProperty("federalDistrictShortName")
    private String federalDistrictShortName;

    @JsonProperty("federalDistrictName")
    private String federalDistrictName;

    // Конструкторы
    public EducationalInstitutionDto() {
    }

    public EducationalInstitutionDto(EducationalInstitution institution) {
        this.uuid = institution.getUuid();
        this.isActive = institution.getIsActive();
        this.issueDate = institution.getIssueDate();
        this.endDate = institution.getEndDate();
        this.id = institution.getInstitutionId();
        this.fullName = institution.getFullName();
        this.shortName = institution.getShortName();
        this.headEduOrgId = institution.getHeadEduOrgId();
        this.isBranch = institution.getIsBranch();
        this.postAddress = institution.getPostAddress();
        this.phone = institution.getPhone();
        this.fax = institution.getFax();
        this.email = institution.getEmail();
        this.webSite = institution.getWebSite();
        this.ogrn = institution.getOgrn();
        this.inn = institution.getInn();
        this.kpp = institution.getKpp();
        this.headPost = institution.getHeadPost();
        this.headName = institution.getHeadName();
        this.formName = institution.getFormName();
        this.kindName = institution.getKindName();
        this.typeName = institution.getTypeName();
        this.regionName = institution.getRegionName();
        this.federalDistrictShortName = institution.getFederalDistrictShortName();
        this.federalDistrictName = institution.getFederalDistrictName();
    }
}