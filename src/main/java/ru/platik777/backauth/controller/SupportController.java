package ru.platik777.backauth.controller;

import ru.platik777.backauth.dto.request.SendMessageToSupportRequestDto;
import ru.platik777.backauth.dto.response.ApiResponseDto;
import ru.platik777.backauth.service.SupportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Контроллер для работы с поддержкой
 * Реализует эндпоинты из handler.go
 *
 * Paths:
 * - /api/v1/internal/sendMessageToSupport - отправка сообщения в поддержку
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/internal")
@RequiredArgsConstructor
public class SupportController {

    private final SupportService supportService;

    /**
     * Отправка сообщения в поддержку
     * POST /api/v1/internal/sendMessageToSupport
     * Аналог sendMessageToSupport из Go handler
     */
    @PostMapping("/sendMessageToSupport")
    public ResponseEntity<ApiResponseDto<String>> sendMessageToSupport(
            @Valid @RequestBody SendMessageToSupportRequestDto requestDto) {

        log.info("Send message to support request from: {} ({})",
                requestDto.getSenderName(), requestDto.getSenderEmail());

        ApiResponseDto<String> response = supportService.sendMessageToSupport(requestDto);

        return ResponseEntity.ok(response);
    }
}