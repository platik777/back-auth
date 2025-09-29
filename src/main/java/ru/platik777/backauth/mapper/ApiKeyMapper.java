package ru.platik777.backauth.mapper;

import ru.platik777.backauth.dto.ApiKeyDto;
import ru.platik777.backauth.dto.request.ApiKeyRequestDto;
import ru.platik777.backauth.entity.ApiKey;
import ru.platik777.backauth.entity.User;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Component
public class ApiKeyMapper {

    public ApiKeyDto toDto(ApiKey apiKey) {
        return new ApiKeyDto(apiKey);
    }

    public ApiKey toEntity(ApiKeyRequestDto requestDto, User user, String token) {
        ApiKey apiKey = new ApiKey();
        apiKey.setApiKey(token);
        apiKey.setUser(user);
        apiKey.setName(requestDto.getName());
        apiKey.setIsDeleted(false);

        // Парсинг даты истечения
        LocalDateTime expireAt;
        if (requestDto.getExpireAt() == null || requestDto.getExpireAt().equals("2099-01-01")) {
            // Бесконечный ключ
            expireAt = LocalDateTime.of(2099, 1, 1, 23, 59, 59);
        } else {
            // Добавляем время до конца дня
            LocalDate expireDate = LocalDate.parse(requestDto.getExpireAt());
            expireAt = expireDate.atTime(23, 59, 59);
        }
        apiKey.setExpireAt(expireAt);

        return apiKey;
    }
}