package com.example.exception.playground.common;

import java.time.LocalDateTime;
import java.util.List;

public class ErrorResponse {

    private final String code;
    private final String message;
    private final int status;
    private final LocalDateTime timestamp;
    private final List<FieldError> errors;

    private ErrorResponse(String code, String message, int status, List<FieldError> errors) {
        this.code = code;
        this.message = message;
        this.status = status;
        this.timestamp = LocalDateTime.now();
        this.errors = errors;
    }

    public static ErrorResponse of(ErrorCode errorCode) {
        return new ErrorResponse(errorCode.getCode(), errorCode.getMessage(),
                errorCode.getStatus().value(), List.of());
    }

    public static ErrorResponse of(ErrorCode errorCode, String message) {
        return new ErrorResponse(errorCode.getCode(), message,
                errorCode.getStatus().value(), List.of());
    }

    public static ErrorResponse of(ErrorCode errorCode, List<FieldError> errors) {
        return new ErrorResponse(errorCode.getCode(), errorCode.getMessage(),
                errorCode.getStatus().value(), errors);
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public int getStatus() {
        return status;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public List<FieldError> getErrors() {
        return errors;
    }

    public record FieldError(String field, String value, String reason) {

        public static FieldError of(String field, String value, String reason) {
            return new FieldError(field, value, reason);
        }
    }
}
