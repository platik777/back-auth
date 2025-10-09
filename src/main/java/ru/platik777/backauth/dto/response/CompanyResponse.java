package ru.platik777.backauth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.platik777.backauth.entity.Tenant;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

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
    private List<UUID> usersList;

    public static CompanyResponse fromCompany(Tenant tenant) {
        return CompanyResponse.builder()
                .fullTitle(tenant.getFullTitle())
                .country(tenant.getCountry())
                .legalAddress(tenant.getLegalAddress())
                .postAddress(tenant.getPostAddress())
                .accountNumber(tenant.getAccountNumber())
                .bankName(tenant.getBankName())
                .bic(tenant.getBic())
                .taxBank(tenant.getTaxBank())
                .kppBank(tenant.getKppBank())
                .correspondentAccount(tenant.getCorrespondentAccount())
                .taxNumber(tenant.getTaxNumber())
                .ogrn(tenant.getOgrn())
                .ogrnip(tenant.getOgrnip())
                .kpp(tenant.getKpp())
                .kio(tenant.getKio())
                .licenseNumber(tenant.getLicenseNumber())
                .licenseIssueDate(String.valueOf(tenant.getLicenseIssueDate()))
                .createdAt(LocalDateTime.from(tenant.getCreatedAt()))
                .usersList(tenant.getUsersList())
                .build();
    }
}
