package ru.platik777.backauth.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SupportMessageRequestDto {

    @JsonProperty("userName")
    @NotBlank(message = "Username cannot be empty")
    private String userName;

    @Email(message = "Invalid email format")
    @NotBlank(message = "Email cannot be empty")
    private String email;

    @NotBlank(message = "Message cannot be empty")
    @Size(max = 4096, message = "Message is too big")
    private String message;

    private String subject = "other-help"; // По умолчанию "Другой вопрос"
}