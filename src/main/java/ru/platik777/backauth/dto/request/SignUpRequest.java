package ru.platik777.backauth.dto.request;

import lombok.Data;

@Data
public class SignUpRequest {
    private UserDto user;
    private StudentDto student;
    private CompanyDto company;
    private String locale;

    @Data
    public static class UserDto {
        private String login;
        private String password;
        private String email;
        private String userName;
        private String phone;
        private String accountType;
    }

    @Data
    public static class StudentDto {
        private Integer startYear;
        private Integer endYear;
        private String studentId;
        private String educationalInstitutionsUuid;
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