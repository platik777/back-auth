package ru.platik777.backauth.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "educational_institutions", schema = "public")
@Data
@EqualsAndHashCode(callSuper = false)
public class EducationalInstitution {

    @Id
    @Column(name = "educational_institutions_uuid")
    private String uuid;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Column(name = "issue_date")
    private LocalDateTime issueDate;

    @Column(name = "end_date")
    private LocalDateTime endDate;

    @Column(name = "id", nullable = false)
    private String institutionId;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "short_name")
    private String shortName;

    @Column(name = "head_edu_org_id")
    private String headEduOrgId;

    @Column(name = "is_branch")
    private Integer isBranch;

    @Column(name = "post_address")
    private String postAddress;

    @Column(name = "phone")
    private String phone;

    @Column(name = "fax")
    private String fax;

    @Column(name = "email")
    private String email;

    @Column(name = "web_site")
    private String webSite;

    @Column(name = "ogrn")
    private String ogrn;

    @Column(name = "inn")
    private String inn;

    @Column(name = "kpp")
    private String kpp;

    @Column(name = "head_post")
    private String headPost;

    @Column(name = "head_name")
    private String headName;

    @Column(name = "form_name")
    private String formName;

    @Column(name = "kind_name")
    private String kindName;

    @Column(name = "type_name")
    private String typeName;

    @Column(name = "region_name")
    private String regionName;

    @Column(name = "federal_district_short_name")
    private String federalDistrictShortName;

    @Column(name = "federal_district_name")
    private String federalDistrictName;

    @OneToMany(mappedBy = "educationalInstitution", fetch = FetchType.LAZY)
    private List<Student> students;
}