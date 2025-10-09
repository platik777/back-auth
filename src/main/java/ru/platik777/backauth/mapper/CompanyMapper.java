package ru.platik777.backauth.mapper;

import org.springframework.stereotype.Component;
import ru.platik777.backauth.dto.request.SignUpRequest;
import ru.platik777.backauth.entity.Tenant;

import java.time.LocalDate;

/**
 * Маппер для преобразования DTO в Company Entity
 */
@Component
public class CompanyMapper {

    /**
     * Маппинг CompanyDto в Company Entity
     */
    public Tenant toEntity(SignUpRequest.CompanyDto dto) {
        if (dto == null) {
            return null;
        }

        Tenant tenant = new Tenant();
        tenant.setFullTitle(dto.getFullTitle());
        tenant.setCountry(dto.getCountry());
        tenant.setLegalAddress(dto.getLegalAddress());
        tenant.setPostAddress(dto.getPostAddress());
        tenant.setAccountNumber(dto.getAccountNumber());
        tenant.setBankName(dto.getBankName());
        tenant.setBic(dto.getBic());
        tenant.setTaxBank(dto.getTaxBank());
        tenant.setKppBank(dto.getKppBank());
        tenant.setCorrespondentAccount(dto.getCorrespondentAccount());
        tenant.setTaxNumber(dto.getTaxNumber());
        tenant.setOgrn(dto.getOgrn());
        tenant.setOgrnip(dto.getOgrnip());
        tenant.setKpp(dto.getKpp());
        tenant.setKio(dto.getKio());
        tenant.setLicenseNumber(dto.getLicenseNumber());
        tenant.setLicenseIssueDate(LocalDate.parse(dto.getLicenseIssueDate()));

        return tenant;
    }
}
