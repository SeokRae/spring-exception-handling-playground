package com.example.exception.playground.global.error;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    private final String traceId;
    private final String code;
    private final String message;
    private final int status;
    private final LocalDateTime timestamp;
    private final List<FieldError> errors;
    private final String debugMessage;
    private final String stackTrace;

    private ErrorResponse(String code, String message, int status,
                          List<FieldError> errors, String debugMessage, String stackTrace) {
        this.traceId = UUID.randomUUID().toString().substring(0, 8);
        this.code = code;
        this.message = message;
        this.status = status;
        this.timestamp = LocalDateTime.now();
        this.errors = errors;
        this.debugMessage = debugMessage;
        this.stackTrace = stackTrace;
    }

    public static ErrorResponse of(ErrorCode errorCode) {
        return new ErrorResponse(errorCode.getCode(), errorCode.getMessage(),
                errorCode.getStatus().value(), List.of(), null, null);
    }

    public static ErrorResponse of(ErrorCode errorCode, String message) {
        return new ErrorResponse(errorCode.getCode(), message,
                errorCode.getStatus().value(), List.of(), null, null);
    }

    public static ErrorResponse of(ErrorCode errorCode, List<FieldError> errors) {
        return new ErrorResponse(errorCode.getCode(), errorCode.getMessage(),
                errorCode.getStatus().value(), errors, null, null);
    }

    public static ErrorResponse withDebug(ErrorCode errorCode, Exception e) {
        String debugMsg = e.getClass().getSimpleName() + ": " + e.getMessage();
        String trace = getStackTraceAsString(e);
        return new ErrorResponse(errorCode.getCode(), errorCode.getMessage(),
                errorCode.getStatus().value(), List.of(), debugMsg, trace);
    }

    public static ErrorResponse withDebug(ErrorCode errorCode, String message, Exception e) {
        String debugMsg = e.getClass().getSimpleName() + ": " + e.getMessage();
        String trace = getStackTraceAsString(e);
        return new ErrorResponse(errorCode.getCode(), message,
                errorCode.getStatus().value(), List.of(), debugMsg, trace);
    }

    public static ErrorResponse withDebug(ErrorCode errorCode, List<FieldError> errors, Exception e) {
        String debugMsg = e.getClass().getSimpleName() + ": " + e.getMessage();
        String trace = getStackTraceAsString(e);
        return new ErrorResponse(errorCode.getCode(), errorCode.getMessage(),
                errorCode.getStatus().value(), errors, debugMsg, trace);
    }

    private static String getStackTraceAsString(Exception e) {
        StringBuilder sb = new StringBuilder();
        for (StackTraceElement element : e.getStackTrace()) {
            sb.append(element.toString()).append("\n");
            if (sb.length() > 2000) {
                sb.append("... truncated");
                break;
            }
        }
        return sb.toString();
    }

    public String getTraceId() {
        return traceId;
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

    public String getDebugMessage() {
        return debugMessage;
    }

    public String getStackTrace() {
        return stackTrace;
    }

    public record FieldError(String field, String value, String reason) {

        public static FieldError of(String field, String value, String reason) {
            return new FieldError(field, value, reason);
        }
    }
}
