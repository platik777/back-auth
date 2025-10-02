package ru.platik777.backauth.dto.request;

import lombok.Data;

@Data
public class SignInRequest {
    private String login;
    private String password;
}
