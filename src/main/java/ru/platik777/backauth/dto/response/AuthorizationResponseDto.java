package ru.platik777.backauth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthorizationResponseDto {

    private String status;
    private Integer userId;

    public static AuthorizationResponseDto success(Integer userId) {
        return new AuthorizationResponseDto("OK", userId);
    }

    public static AuthorizationResponseDto invalid() {
        return new AuthorizationResponseDto("invalid", null);
    }
}