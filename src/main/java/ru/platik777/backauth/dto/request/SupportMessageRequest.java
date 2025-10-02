package ru.platik777.backauth.dto.request;

import lombok.Data;

@Data
public class SupportMessageRequest {
    private String userName;
    private String email;
    private String message;
    private String targetSubject;
}
