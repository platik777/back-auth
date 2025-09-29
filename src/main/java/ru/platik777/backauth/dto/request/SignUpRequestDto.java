package ru.platik777.backauth.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class SignUpRequestDto {

    @NotBlank(message = "Login cannot be empty")
    @Pattern(regexp = "^[a-zA-Z][a-zA-Z0-9_]{2,}$", message = "Invalid login format")
    private String login;

    @NotBlank(message = "Password cannot be empty")
    private String password;

    @Email(message = "Invalid email format")
    @NotBlank(message = "Email cannot be empty")
    private String email;

    @JsonProperty("userName")
    @NotBlank(message = "Username cannot be empty")
    private String userName;

    @Pattern(regexp = "^\\+\\d{1,4} \\d{1,4} \\d{1,12}$", message = "Invalid phone format")
    @NotBlank(message = "Phone cannot be empty")
    private String phone;

    @JsonProperty("accountType")
    private String accountType = "individual";

    // Данные студента (если accountType = "student")
    private Integer startYear;
    private Integer endYear;
    private String studentId;
    private String educationalInstitutionsUuid;

    // Данные компании (для всех остальных типов)
    private String fullTitle;
    private String country = "russia";
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