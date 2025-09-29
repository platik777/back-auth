package ru.platik777.backauth.service;

import ru.platik777.backauth.dto.request.SupportMessageRequestDto;
import ru.platik777.backauth.dto.response.ApiResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Сервис для работы с обращениями в техподдержку
 * Реализует логику из Go версии SendMessageToSupport
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SupportService {

    private final EmailService emailService;

    /**
     * Отправка сообщения в техподдержку (аналог SendMessageToSupport из Go)
     */
    public ApiResponseDto<Boolean> sendMessageToSupport(SupportMessageRequestDto requestDto) {
        log.debug("Sending support message from: {}", requestDto.getEmail());

        // Валидация входных данных
        validateSupportMessage(requestDto);

        try {
            // Отправка email в техподдержку
            emailService.sendSupportMessage(
                    requestDto.getUserName(),
                    requestDto.getEmail(),
                    requestDto.getMessage(),
                    requestDto.getSubject()
            );

            log.info("Support message sent successfully from: {}", requestDto.getEmail());
            return ApiResponseDto.success(true);

        } catch (Exception e) {
            log.error("Error sending support message from: {}", requestDto.getEmail(), e);
            throw new RuntimeException("Error sending support message: " + e.getMessage());
        }
    }

    /**
     * Валидация данных сообщения в техподдержку
     */
    private void validateSupportMessage(SupportMessageRequestDto requestDto) {
        if (requestDto.getUserName() == null || requestDto.getUserName().trim().isEmpty()) {
            throw new RuntimeException("Username cannot be empty");
        }

        if (requestDto.getEmail() == null || requestDto.getEmail().trim().isEmpty()) {
            throw new RuntimeException("Email cannot be empty");
        }

        if (requestDto.getMessage() == null || requestDto.getMessage().trim().isEmpty()) {
            throw new RuntimeException("Message cannot be empty");
        }

        if (requestDto.getMessage().length() > 4096) {
            throw new RuntimeException("Message is too big");
        }

        // Валидация email формата
        if (!isValidEmail(requestDto.getEmail())) {
            throw new RuntimeException("Invalid email format");
        }
    }

    /**
     * Проверка валидности email
     */
    private boolean isValidEmail(String email) {
        return email != null && email.contains("@") && email.contains(".");
    }

    /**
     * Получение списка доступных тем обращений
     */
    public ApiResponseDto<?> getSupportSubjects() {
        var subjects = new SupportSubjects(
                new SupportSubject("features-consult", "Консультация по возможностям продукта"),
                new SupportSubject("tariff-consult", "Консультация по тарифам и приобретению продукта"),
                new SupportSubject("auth-help", "Требуется помощь с авторизацией/регистрацией"),
                new SupportSubject("other-help", "Другой вопрос")
        );

        return ApiResponseDto.success(subjects);
    }

    // Внутренние классы для ответов

    public record SupportSubject(String code, String description) {}

    public record SupportSubjects(
            SupportSubject featuresConsult,
            SupportSubject tariffConsult,
            SupportSubject authHelp,
            SupportSubject otherHelp
    ) {}
}