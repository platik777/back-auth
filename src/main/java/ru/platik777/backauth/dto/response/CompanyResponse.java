package ru.platik777.backauth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.platik777.backauth.entity.Company;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO компании для ответа
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompanyResponse {
    private Integer companyId;
    private Integer ownerId;
    private String fullTitle;
    private String country;
    private String legalAddress;
    private String postAddress;
    private String accountNumber;
    private String bankName;
    private String bic;
    private String taxBank;
    private String kppBank;
    private String correspondentAccount;
    private String taxNumber;
    private String ogrn;
    private String ogrnip;
    private String kpp;
    private String kio;
    private String licenseNumber;
    private String licenseIssueDate;
    private LocalDateTime createdAt;
    private List<Integer> usersList;

    public static CompanyResponse fromCompany(Company company) {
        return CompanyResponse.builder()
                .companyId(company.getCompanyId())
                .ownerId(company.getOwnerId())
                .fullTitle(company.getFullTitle())
                .country(company.getCountry())
                .legalAddress(company.getLegalAddress())
                .postAddress(company.getPostAddress())
                .accountNumber(company.getAccountNumber())
                .bankName(company.getBankName())
                .bic(company.getBic())
                .taxBank(company.getTaxBank())
                .kppBank(company.getKppBank())
                .correspondentAccount(company.getCorrespondentAccount())
                .taxNumber(company.getTaxNumber())
                .ogrn(company.getOgrn())
                .ogrnip(company.getOgrnip())
                .kpp(company.getKpp())
                .kio(company.getKio())
                .licenseNumber(company.getLicenseNumber())
                .licenseIssueDate(company.getLicenseIssueDate())
                .createdAt(company.getCreatedAt())
                .usersList(company.getUsersList())
                .build();
    }
}
