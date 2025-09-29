package ru.platik777.backauth.controller;

import ru.platik777.backauth.dto.request.ResetPasswordForgotRequestDto;
import ru.platik777.backauth.dto.request.ResetPasswordUpdateRequestDto;
import ru.platik777.backauth.dto.response.ApiResponseDto;
import ru.platik777.backauth.service.ResetPasswordService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Контроллер для сброса пароля
 * Реализует эндпоинты из handler.go
 * <p>
 * Paths:
 * - /api/v1/resetPassword/* - эндпоинты для сброса пароля
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/resetPassword")
@RequiredArgsConstructor
public class ResetPasswordController {

    private final ResetPasswordService resetPasswordService;

    /**
     * Запрос на сброс пароля - отправка email с токеном
     * POST /api/v1/resetPassword/forgot
     * Аналог resetPasswordForgot из Go handler
     */
    @PostMapping("/forgot")
    public ResponseEntity<ApiResponseDto<String>> forgotPassword(
            @Valid @RequestBody ResetPasswordForgotRequestDto requestDto) {

        log.info("Forgot password request for email: {}", requestDto.getEmail());

        ApiResponseDto<String> response = resetPasswordService.forgotPassword(requestDto);
        return ResponseEntity.ok(response);
    }

    /**
     * Проверка токена сброса пароля
     * GET /api/v1/resetPassword/check?token={token}
     * Аналог resetPasswordCheck из Go handler
     */
    @GetMapping("/check")
    public ResponseEntity<ApiResponseDto<?>> checkResetToken(
            @RequestParam String token) {

        log.info("Check reset password token request");

        ApiResponseDto<?> response = resetPasswordService.checkResetToken(token);
        return ResponseEntity.ok(response);
    }

    /**
     * Обновление пароля по токену сброса
     * POST /api/v1/resetPassword/update
     * Аналог resetPasswordUpdate из Go handler
     */
    @PostMapping("/update")
    public ResponseEntity<ApiResponseDto<String>> updatePassword(
            @Valid @RequestBody ResetPasswordUpdateRequestDto requestDto) {

        log.info("Update password request");

        ApiResponseDto<String> response = resetPasswordService.updatePassword(requestDto);
        return ResponseEntity.ok(response);
    }
}