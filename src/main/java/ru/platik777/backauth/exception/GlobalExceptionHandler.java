package ru.platik777.backauth.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Глобальный обработчик исключений
 * Соответствует error handling в Go
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Обработка исключений аутентификации/авторизации
     * Go: возвращает 401 Unauthorized
     */
    @ExceptionHandler(AuthException.class)
    public ResponseEntity<ErrorResponse> handleAuthException(
            AuthException ex,
            WebRequest request) {

        log.warn("Authentication/Authorization error: {}", ex.getMessage());

        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.UNAUTHORIZED.value())
                .error("Unauthorized")
                .message(ex.getMessage())
                .path(extractPath(request))
                .build();

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    /**
     * Обработка исключений валидации
     * Go: возвращает 400 Bad Request
     */
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ValidationErrorResponse> handleValidationException(
            ValidationException ex,
            WebRequest request) {

        log.warn("Validation error: {} (field: {})", ex.getMessage(), ex.getFieldName());

        ValidationErrorResponse error = ValidationErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Validation Error")
                .message(ex.getMessage())
                .fieldName(ex.getFieldName())
                .path(extractPath(request))
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Обработка исключений API ключей
     * Go: возвращает 400 или 401
     */
    @ExceptionHandler(ApiKeyException.class)
    public ResponseEntity<ErrorResponse> handleApiKeyException(
            ApiKeyException ex,
            WebRequest request) {

        log.warn("API Key error: {}", ex.getMessage());

        // Определяем статус: если ключ невалидный - 401, если другая ошибка - 400
        HttpStatus status = ex.getMessage().contains("invalid") || ex.getMessage().contains("expired")
                ? HttpStatus.UNAUTHORIZED
                : HttpStatus.BAD_REQUEST;

        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(ex.getMessage())
                .path(extractPath(request))
                .build();

        return ResponseEntity.status(status).body(error);
    }

    /**
     * Обработка исключений сброса пароля
     * Go: возвращает 400 Bad Request
     */
    @ExceptionHandler(ResetPasswordException.class)
    public ResponseEntity<ErrorResponse> handleResetPasswordException(
            ResetPasswordException ex,
            WebRequest request) {

        log.warn("Reset password error: {}", ex.getMessage());

        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Bad Request")
                .message(ex.getMessage())
                .path(extractPath(request))
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Обработка JWT исключений
     * Go: возвращает 401 Unauthorized
     */
    @ExceptionHandler(CustomJwtException.class)
    public ResponseEntity<ErrorResponse> handleJwtException(
            CustomJwtException ex,
            WebRequest request) {

        log.warn("JWT error: {}", ex.getMessage());

        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.UNAUTHORIZED.value())
                .error("Unauthorized")
                .message(ex.getMessage())
                .path(extractPath(request))
                .build();

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    /**
     * Обработка исключений отправки email
     * Go: обычно логируется, но не прерывает выполнение
     */
    @ExceptionHandler(EmailSendException.class)
    public ResponseEntity<ErrorResponse> handleEmailSendException(
            EmailSendException ex,
            WebRequest request) {

        log.error("Email send error: {}", ex.getMessage(), ex);

        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("Internal Server Error")
                .message("Failed to send email")
                .path(extractPath(request))
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    /**
     * Обработка IllegalArgumentException
     * Go: возвращает 400 Bad Request
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException ex,
            WebRequest request) {

        log.warn("Illegal argument: {}", ex.getMessage());

        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Bad Request")
                .message(ex.getMessage())
                .path(extractPath(request))
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Обработка всех остальных исключений
     * Go: возвращает 500 Internal Server Error
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(
            Exception ex,
            WebRequest request) {

        log.error("Unexpected error", ex);

        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("Internal Server Error")
                .message("An unexpected error occurred")
                .path(extractPath(request))
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    // ==================== HELPER METHODS ====================

    /**
     * Извлечение пути запроса
     */
    private String extractPath(WebRequest request) {
        return request.getDescription(false).replace("uri=", "");
    }

    // ==================== ERROR RESPONSE DTOs ====================

    /**
     * Базовый DTO для ошибок
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ErrorResponse {
        private LocalDateTime timestamp;
        private Integer status;
        private String error;
        private String message;
        private String path;

        /**
         * Альтернативный формат для совместимости с Go
         */
        public Map<String, Object> toMap() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("timestamp", timestamp);
            map.put("status", status);
            map.put("error", error);
            map.put("message", message);
            map.put("path", path);
            return map;
        }
    }

    /**
     * DTO для ошибок валидации с указанием поля
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ValidationErrorResponse {
        private LocalDateTime timestamp;
        private Integer status;
        private String error;
        private String message;
        private String fieldName;
        private String path;
    }
}