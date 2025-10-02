package ru.platik777.backauth.dto.request;

import lombok.Data;

@Data
public class ResetPasswordUpdateRequest {
    private String token;
    private String password;
}
