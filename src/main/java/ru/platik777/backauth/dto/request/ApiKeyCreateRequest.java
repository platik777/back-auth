package ru.platik777.backauth.dto.request;

import lombok.Data;

@Data
public class ApiKeyCreateRequest {
    private String name;
    private String expireAt;
}
