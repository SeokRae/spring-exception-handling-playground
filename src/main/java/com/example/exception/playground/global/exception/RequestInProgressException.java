package com.example.exception.playground.global.exception;

import com.example.exception.playground.global.error.ErrorCode;

public class RequestInProgressException extends RuntimeException {

    private final ErrorCode errorCode;

    public RequestInProgressException(String message) {
        super(message);
        this.errorCode = ErrorCode.REQUEST_IN_PROGRESS;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
