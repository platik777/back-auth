package ru.platik777.backauth.mapper;

import org.springframework.stereotype.Component;
import ru.platik777.backauth.dto.request.SignUpRequest;
import ru.platik777.backauth.entity.Company;

/**
 * Маппер для преобразования DTO в Company Entity
 */
@Component
public class CompanyMapper {

    /**
     * Маппинг CompanyDto в Company Entity
     */
    public Company toEntity(SignUpRequest.CompanyDto dto) {
        if (dto == null) {
            return null;
        }

        Company company = new Company();
        company.setFullTitle(dto.getFullTitle());
        company.setCountry(dto.getCountry());
        company.setLegalAddress(dto.getLegalAddress());
        company.setPostAddress(dto.getPostAddress());
        company.setAccountNumber(dto.getAccountNumber());
        company.setBankName(dto.getBankName());
        company.setBic(dto.getBic());
        company.setTaxBank(dto.getTaxBank());
        company.setKppBank(dto.getKppBank());
        company.setCorrespondentAccount(dto.getCorrespondentAccount());
        company.setTaxNumber(dto.getTaxNumber());
        company.setOgrn(dto.getOgrn());
        company.setOgrnip(dto.getOgrnip());
        company.setKpp(dto.getKpp());
        company.setKio(dto.getKio());
        company.setLicenseNumber(dto.getLicenseNumber());
        company.setLicenseIssueDate(dto.getLicenseIssueDate());

        return company;
    }
}
