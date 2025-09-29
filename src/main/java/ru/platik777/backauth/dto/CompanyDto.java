package ru.platik777.backauth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import ru.platik777.backauth.entity.Company;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CompanyDto {

    @JsonProperty("companyId")
    private Integer companyId;

    @JsonProperty("createdAt")
    private LocalDateTime createdAt;

    @JsonProperty("ownerId")
    private Integer ownerId;

    @JsonProperty("bankName")
    private String bankName;

    @JsonProperty("accountNumber")
    private String accountNumber;

    private String bic;

    @JsonProperty("kppBank")
    private String kppBank;

    @JsonProperty("taxBank")
    private String taxBank;

    @JsonProperty("correspondentAccount")
    private String correspondentAccount;

    @JsonProperty("fullTitle")
    private String fullTitle;

    private String ogrn;
    private String ogrnip;
    private String kpp;

    @JsonProperty("legalAddress")
    private String legalAddress;

    @JsonProperty("postAddress")
    private String postAddress;

    @JsonProperty("taxNumber")
    private String taxNumber;

    private String country;
    private String kio;

    @JsonProperty("licenseNumber")
    private String licenseNumber;

    @JsonProperty("licenseIssueDate")
    private String licenseIssueDate;

    @JsonProperty("usersList")
    private List<Integer> usersList;

    // Конструкторы
    public CompanyDto() {}

    public CompanyDto(Company company) {
        this.companyId = company.getCompanyId();
        this.createdAt = company.getCreatedAt();
        this.ownerId = company.getOwner().getId();
        this.bankName = company.getBankName();
        this.accountNumber = company.getAccountNumber();
        this.bic = company.getBic();
        this.kppBank = company.getKppBank();
        this.taxBank = company.getTaxBank();
        this.correspondentAccount = company.getCorrespondentAccount();
        this.fullTitle = company.getFullTitle();
        this.ogrn = company.getOgrn();
        this.ogrnip = company.getOgrnip();
        this.kpp = company.getKpp();
        this.legalAddress = company.getLegalAddress();
        this.postAddress = company.getPostAddress();
        this.taxNumber = company.getTaxNumber();
        this.country = company.getCountry();
        this.kio = company.getKio();
        this.licenseNumber = company.getLicenseNumber();
        this.licenseIssueDate = company.getLicenseIssueDate();
        this.usersList = company.getUsersList();
    }
}