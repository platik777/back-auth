package ru.platik777.backauth.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "tenant")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Tenant extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(name = "bank_name", length = 255)
    private String bankName;

    @Column(name = "account_number", length = 100)
    private String accountNumber;

    @Column(length = 50)
    private String bic;

    @Column(name = "kpp_bank", length = 50)
    private String kppBank;

    @Column(name = "tax_bank", length = 50)
    private String taxBank;

    @Column(name = "correspondent_account", length = 100)
    private String correspondentAccount;

    @Column(name = "full_title", length = 500)
    private String fullTitle;

    @Column(length = 50)
    private String ogrn;

    @Column(length = 50)
    private String ogrnip;

    @Column(length = 50)
    private String kpp;

    @Column(name = "legal_address", columnDefinition = "TEXT")
    private String legalAddress;

    @Column(name = "post_address", columnDefinition = "TEXT")
    private String postAddress;

    @Column(name = "tax_number", length = 50)
    private String taxNumber;

    @Column(length = 100)
    private String country;

    @Column(length = 50)
    private String kio;

    @Column(name = "license_number", length = 100)
    private String licenseNumber;

    @Column(name = "license_issue_date")
    private LocalDate licenseIssueDate;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "users_list", columnDefinition = "uuid[]")
    private List<UUID> usersList;
}