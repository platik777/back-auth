package ru.platik777.backauth.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "students", schema = "public")
@Data
@EqualsAndHashCode(callSuper = false)
public class Student {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "student_uuid")
    private UUID uuid;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_confirmation")
    private LocalDateTime lastConfirmation;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "start_year", nullable = false)
    private Integer startYear;

    @Column(name = "end_year", nullable = false)
    private Integer endYear;

    @Column(name = "student_id", nullable = false)
    private String studentId;

    @Column(name = "educational_institutions_uuid", nullable = false)
    private String educationalInstitutionsUuid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "educational_institutions_uuid", referencedColumnName = "educational_institutions_uuid", insertable = false, updatable = false)
    private EducationalInstitution educationalInstitution;
}