package ru.platik777.backauth.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "company", schema = "public")
@Data
@EqualsAndHashCode(callSuper = true)
public class Company extends Tenant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "company_id")
    private Integer companyId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "owner_id", nullable = false, insertable = false, updatable = false)
    private Integer ownerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(name = "bank_name")
    private String bankName;

    @Column(name = "account_number")
    private String accountNumber;

    @Column(name = "bic")
    private String bic;

    @Column(name = "kpp_bank")
    private String kppBank;

    @Column(name = "tax_bank")
    private String taxBank;

    @Column(name = "correspondent_account")
    private String correspondentAccount;

    @Column(name = "full_title")
    private String fullTitle;

    @Column(name = "ogrn")
    private String ogrn;

    @Column(name = "ogrnip")
    private String ogrnip;

    @Column(name = "kpp")
    private String kpp;

    @Column(name = "legal_address")
    private String legalAddress;

    @Column(name = "post_address")
    private String postAddress;

    @Column(name = "tax_number")
    private String taxNumber;

    @Column(name = "country", nullable = false)
    private String country = "russia";

    @Column(name = "kio")
    private String kio;

    @Column(name = "license_number")
    private String licenseNumber;

    @Column(name = "license_issue_date")
    private String licenseIssueDate;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "users_list", columnDefinition = "jsonb")
    private List<Integer> usersList;
}