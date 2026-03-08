package com.example.exception.playground.global.exception;

import com.example.exception.playground.global.error.ErrorCode;

public class RequestInProgressException extends RuntimeException {

    private final ErrorCode errorCode;
    private final Long retryAfterSeconds;

    public RequestInProgressException(String message) {
        super(message);
        this.errorCode = ErrorCode.REQUEST_IN_PROGRESS;
        this.retryAfterSeconds = null;
    }

    public RequestInProgressException(String message, long retryAfterSeconds) {
        super(message);
        this.errorCode = ErrorCode.REQUEST_IN_PROGRESS;
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public Long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
