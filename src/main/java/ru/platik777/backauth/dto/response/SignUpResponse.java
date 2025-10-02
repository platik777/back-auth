package ru.platik777.backauth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Ответ на регистрацию
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SignUpResponse {
    private UserResponse user;
    private StudentResponse student;
    private CompanyResponse company;
}