package ru.platik777.backauth.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TokenResponseDto {

    @JsonProperty("accessToken")
    private String accessToken;

    @JsonProperty("refreshToken")
    private String refreshToken;

    @JsonProperty("accessBaseToken")
    private String accessBaseToken;

    @JsonProperty("refreshBaseToken")
    private String refreshBaseToken;

    private Object user; // UserDto будет сериализован как объект
}