package ru.platik777.backauth.dto.request;

import lombok.Data;

@Data
public class SignUpRequest {
    private UserDto user;
    private CompanyDto company;
    private String locale;

    @Data
    public static class UserDto {
        private String name;
        private String login;
        private String password;
        private String email;
        private String phone;
        private String accountType;
    }

    @Data
    public static class CompanyDto {
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
    }
}