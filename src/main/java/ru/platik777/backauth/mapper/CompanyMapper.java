package ru.platik777.backauth.mapper;

import ru.platik777.backauth.dto.CompanyDto;
import ru.platik777.backauth.dto.request.SignUpRequestDto;
import ru.platik777.backauth.entity.Company;
import ru.platik777.backauth.entity.User;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class CompanyMapper {

    public CompanyDto toDto(Company company) {
        return new CompanyDto(company);
    }

    public Company toEntity(SignUpRequestDto requestDto, User owner) {
        Company company = new Company();
        company.setOwner(owner);
        company.setBankName(requestDto.getBankName());
        company.setAccountNumber(requestDto.getAccountNumber());
        company.setBic(requestDto.getBic());
        company.setKppBank(requestDto.getKppBank());
        company.setTaxBank(requestDto.getTaxBank());
        company.setCorrespondentAccount(requestDto.getCorrespondentAccount());
        company.setFullTitle(requestDto.getFullTitle());
        company.setOgrn(requestDto.getOgrn());
        company.setOgrnip(requestDto.getOgrnip());
        company.setKpp(requestDto.getKpp());
        company.setLegalAddress(requestDto.getLegalAddress());
        company.setPostAddress(requestDto.getPostAddress());
        company.setTaxNumber(requestDto.getTaxNumber());
        company.setCountry(requestDto.getCountry() != null ? requestDto.getCountry() : "russia");
        company.setKio(requestDto.getKio());
        company.setLicenseNumber(requestDto.getLicenseNumber());
        company.setLicenseIssueDate(requestDto.getLicenseIssueDate());

        // Добавляем владельца в список пользователей компании
        List<Integer> usersList = new ArrayList<>();
        usersList.add(owner.getId());
        company.setUsersList(usersList);

        return company;
    }
}