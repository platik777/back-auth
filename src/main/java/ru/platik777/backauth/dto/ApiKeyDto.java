package ru.platik777.backauth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import ru.platik777.backauth.entity.ApiKey;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiKeyDto {

    private Integer id;

    @JsonProperty("key")
    private String apiKey;

    @JsonProperty("userId")
    private Integer userId;

    @JsonProperty("expireAt")
    private String expireAt; // Может быть null для бесконечных ключей

    @JsonProperty("createdAt")
    private LocalDateTime createdAt;

    private String name;

    // Конструкторы
    public ApiKeyDto() {}

    public ApiKeyDto(ApiKey apiKey) {
        this.id = apiKey.getId();
        this.apiKey = apiKey.getApiKey();
        this.userId = apiKey.getUser().getId();
        this.createdAt = apiKey.getCreatedAt();
        this.name = apiKey.getName();

        // Логика для бесконечного ключа (из Go кода)
        LocalDateTime infiniteDate = LocalDateTime.of(2099, 1, 1, 23, 59, 59);
        if (apiKey.getExpireAt().equals(infiniteDate)) {
            this.expireAt = null;
        } else {
            this.expireAt = apiKey.getExpireAt().toLocalDate().toString();
        }
    }
}