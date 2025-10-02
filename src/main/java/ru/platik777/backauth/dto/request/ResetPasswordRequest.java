package ru.platik777.backauth.dto.request;

import lombok.Data;

@Data
public class ResetPasswordRequest {
    private String email;
    private String locale;
}
