package ru.platik777.backauth.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.platik777.backauth.dto.request.ResetPasswordRequest;
import ru.platik777.backauth.dto.request.ResetPasswordUpdateRequest;
import ru.platik777.backauth.dto.response.ResetPasswordCheckResponse;
import ru.platik777.backauth.dto.response.StatusResponse;
import ru.platik777.backauth.dto.response.UserResponse;
import ru.platik777.backauth.service.ResetPasswordService;
import ru.platik777.backauth.util.EmailMasker;

/**
 * Контроллер восстановления пароля
 * Соответствует resetPassword.go endpoints
 *
 * Базовый путь: /api/v1/resetPassword
 * Go: apiv1ResetPasswordR := apiv1R.PathPrefix("/resetPassword").Subrouter()
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/resetPassword")
@RequiredArgsConstructor
public class ResetPasswordController {

    private final ResetPasswordService resetPasswordService;
    private final EmailMasker emailMasker;

    /**
     * POST /api/v1/resetPassword/forgot
     * Отправка email для восстановления пароля
     * Go: apiv1ResetPasswordR.HandleFunc("/forgot", h.resetPasswordForgot).Methods(http.MethodPost)
     *
     * ВАЖНО: Всегда возвращает успешный ответ для защиты от перебора email
     */
    @PostMapping("/forgot")
    public ResponseEntity<StatusResponse> resetPasswordForgot(
            @RequestBody ResetPasswordRequest request) {

        log.info("Reset password request for email: {}", emailMasker.mask(request.getEmail()));

        StatusResponse response = resetPasswordService.resetPasswordForgot(
                request.getEmail(),
                request.getLocale()
        );

        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/v1/resetPassword/check
     * Проверка валидности токена восстановления
     * Go: apiv1ResetPasswordR.HandleFunc("/check", h.resetPasswordCheck).Methods(http.MethodGet)
     */
    @GetMapping("/check")
    public ResponseEntity<ResetPasswordCheckResponse> resetPasswordCheck(
            @RequestParam String token) {

        log.debug("Checking reset password token");

        ResetPasswordCheckResponse response =
                resetPasswordService.resetPasswordCheck(token);

        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/v1/resetPassword/update
     * Обновление пароля через токен восстановления
     * Go: apiv1ResetPasswordR.HandleFunc("/update", h.resetPasswordUpdate).Methods(http.MethodPost)
     */
    @PostMapping("/update")
    public ResponseEntity<UserResponse> resetPasswordUpdate(
            @RequestBody ResetPasswordUpdateRequest request) {

        log.info("Updating password via reset token");

        UserResponse response = resetPasswordService.resetPasswordUpdate(
                request.getToken(),
                request.getPassword()
        );

        return ResponseEntity.ok(response);
    }
}