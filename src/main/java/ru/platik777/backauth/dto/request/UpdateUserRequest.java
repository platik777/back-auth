package ru.platik777.backauth.dto.request;

import lombok.Data;

@Data
public class UpdateUserRequest {
    private Integer id;
    private String login;
    private String email;
    private String userName;
    private String phone;
    private String password;
    private String newPassword;
}
