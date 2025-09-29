package ru.platik777.backauth.exception;

import ru.platik777.backauth.dto.response.ApiResponseDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.HashMap;
import java.util.Map;

/**
 * Глобальный обработчик исключений
 * Аналог системы обработки ошибок из Go (go_logger с кодами ошибок)
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Обработка общих RuntimeException
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponseDto<Void>> handleRuntimeException(RuntimeException ex) {
        log.error("Runtime exception occurred", ex);

        ApiResponseDto<Void> response = ApiResponseDto.error(
                ex.getMessage() != null ? ex.getMessage() : "Internal server error"
        );

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(response);
    }

    /**
     * Обработка ошибок валидации (Bean Validation)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponseDto<Map<String, String>>> handleValidationException(
            MethodArgumentNotValidException ex) {

        log.warn("Validation exception occurred");

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        ApiResponseDto<Map<String, String>> response = new ApiResponseDto<>();
        response.setStatus(false);
        response.setMessage("Validation failed");
        response.setData(errors);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }

    /**
     * Обработка отсутствующего обязательного заголовка
     */
    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ApiResponseDto<Void>> handleMissingHeaderException(
            MissingRequestHeaderException ex) {

        log.warn("Missing required header: {}", ex.getHeaderName());

        ApiResponseDto<Void> response = ApiResponseDto.error(
                String.format("Required header '%s' is missing", ex.getHeaderName())
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }

    /**
     * Обработка ошибок преобразования типов параметров
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponseDto<Void>> handleTypeMismatchException(
            MethodArgumentTypeMismatchException ex) {

        log.warn("Type mismatch for parameter: {}", ex.getName());

        ApiResponseDto<Void> response = ApiResponseDto.error(
                String.format("Invalid value for parameter '%s'", ex.getName())
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }

    /**
     * Обработка IllegalArgumentException
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponseDto<Void>> handleIllegalArgumentException(
            IllegalArgumentException ex) {

        log.warn("Illegal argument exception: {}", ex.getMessage());

        ApiResponseDto<Void> response = ApiResponseDto.error(
                ex.getMessage() != null ? ex.getMessage() : "Invalid argument"
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }

    /**
     * Обработка NullPointerException
     */
    @ExceptionHandler(NullPointerException.class)
    public ResponseEntity<ApiResponseDto<Void>> handleNullPointerException(
            NullPointerException ex) {

        log.error("Null pointer exception occurred", ex);

        ApiResponseDto<Void> response = ApiResponseDto.error(
                "A required value was not provided"
        );

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(response);
    }

    /**
     * Обработка всех остальных исключений
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponseDto<Void>> handleGenericException(Exception ex) {
        log.error("Unexpected exception occurred", ex);

        ApiResponseDto<Void> response = ApiResponseDto.error(
                "An unexpected error occurred. Please try again later."
        );

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(response);
    }
}