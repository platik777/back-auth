package ru.platik777.backauth.service;

import ru.platik777.backauth.dto.request.SendMessageToSupportRequestDto;
import ru.platik777.backauth.dto.response.ApiResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Сервис для работы с поддержкой
 * Реализует логику SendMessageToSupport из Go
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SupportService {

    private final EmailService emailService;

    /**
     * Отправка сообщения в поддержку
     * Аналог SendMessageToSupport из Go
     *
     * @param requestDto DTO с данными сообщения
     * @return Ответ об успешной отправке
     */
    public ApiResponseDto<String> sendMessageToSupport(SendMessageToSupportRequestDto requestDto) {
        log.debug("Sending message to support from: {} ({})",
                requestDto.getSenderName(), requestDto.getSenderEmail());

        // Валидация
        if (requestDto.getSenderName() == null || requestDto.getSenderName().trim().isEmpty()) {
            throw new RuntimeException("Sender name cannot be empty");
        }

        if (requestDto.getSenderEmail() == null || requestDto.getSenderEmail().trim().isEmpty()) {
            throw new RuntimeException("Sender email cannot be empty");
        }

        if (requestDto.getMessage() == null || requestDto.getMessage().trim().isEmpty()) {
            throw new RuntimeException("Message cannot be empty");
        }

        // Отправка email (асинхронно)
        emailService.sendMessageToSupport(
                requestDto.getSenderName(),
                requestDto.getSenderEmail(),
                requestDto.getMessage(),
                requestDto.getTargetSubject()
        );

        log.info("Support message sent successfully from: {}", requestDto.getSenderEmail());

        return ApiResponseDto.success("Message sent to support successfully");
    }
}