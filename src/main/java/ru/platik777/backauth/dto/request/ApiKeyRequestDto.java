package ru.platik777.backauth.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ApiKeyRequestDto {

    @JsonProperty("userId")
    private Integer userId;

    @JsonProperty("expireAt")
    private String expireAt = "2099-01-01"; // По умолчанию бесконечный ключ

    @NotBlank(message = "Name cannot be empty")
    private String name;
}