package com.example.exception.playground.global.error;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    // Common
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "C001", "Invalid input value"),
    MISSING_PARAMETER(HttpStatus.BAD_REQUEST, "C002", "Missing required parameter"),
    UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "C003", "Unsupported media type"),
    MESSAGE_NOT_READABLE(HttpStatus.BAD_REQUEST, "C004", "Malformed request body"),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "C005", "Method not allowed"),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "C006", "Resource not found"),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C999", "Internal server error"),

    // Business
    DUPLICATE_RESOURCE(HttpStatus.CONFLICT, "B001", "Duplicate resource exists"),
    BUSINESS_RULE_VIOLATION(HttpStatus.UNPROCESSABLE_ENTITY, "B002", "Business rule violation"),

    // Auth
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "A001", "Authentication required"),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "A002", "Access denied"),

    // Rate Limit
    RATE_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "R001", "Too many requests"),

    // Idempotency
    IDEMPOTENCY_KEY_MISSING(HttpStatus.BAD_REQUEST, "I001", "Idempotency-Key header is required"),
    IDEMPOTENCY_KEY_REUSED(HttpStatus.CONFLICT, "I002", "Request in progress with this key"),
    IDEMPOTENCY_FINGERPRINT_MISMATCH(HttpStatus.UNPROCESSABLE_ENTITY, "I003", "Request does not match original"),

    // Gateway (하위 서비스 통신 오류)
    GATEWAY_ERROR(HttpStatus.BAD_GATEWAY, "G001", "Downstream service unavailable"),
    GATEWAY_TIMEOUT(HttpStatus.GATEWAY_TIMEOUT, "G002", "Downstream service timeout"),
    SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "G003", "Service temporarily unavailable"),

    // Accepted (비동기 처리 상태)
    REQUEST_IN_PROGRESS(HttpStatus.ACCEPTED, "P001", "Request is being processed");

    private final HttpStatus status;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
