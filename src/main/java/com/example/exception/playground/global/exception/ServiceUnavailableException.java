package com.example.exception.playground.global.exception;

import com.example.exception.playground.global.error.ErrorCode;

public class ServiceUnavailableException extends GatewayException {

    private final Long retryAfterSeconds;

    public ServiceUnavailableException(String message) {
        super(ErrorCode.SERVICE_UNAVAILABLE, message);
        this.retryAfterSeconds = null;
    }

    public ServiceUnavailableException(String message, long retryAfterSeconds) {
        super(ErrorCode.SERVICE_UNAVAILABLE, message);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public Long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
