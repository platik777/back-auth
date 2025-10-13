package ru.platik777.backauth.exception;

import lombok.Getter;

@Getter
public class ApiKeyException extends RuntimeException {
    private final String fieldName;

    public ApiKeyException(String message, String fieldName) {
        super(message);
        this.fieldName = fieldName;
    }

    public ApiKeyException(String message, Throwable cause) {
        super(message, cause);
        this.fieldName = null;
    }

}
