package ru.platik777.backauth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.platik777.backauth.dto.response.StatusResponse;

/**
 * Сервис внутренних операций
 * Соответствует internal.go
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InternalService {

    //private final EmailService emailService;
    private final ValidationService validationService;

    /**
     * SendMessageToSupport - отправка сообщения в техподдержку
     * Go: func (a *AuthService) SendMessageToSupport(...)
     */
    public StatusResponse sendMessageToSupport(String userName, String email, String message, String targetSubject) {
        log.debug("SendMessageToSupport started from: {}", email);

        // Валидация
        validationService.validateUserName(userName);
        validationService.validateEmail(email);
        validationService.validateMessage(message);

        // Отправка письма
        //emailService.sendMessageToSupport(userName, email, message, targetSubject);

        return StatusResponse.builder()
                .status(true)
                .build();
    }
}