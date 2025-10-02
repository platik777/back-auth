package ru.platik777.backauth.dto.request;

import lombok.Data;

@Data
public class TokenRefreshRequest {
    private String refreshToken;
}
