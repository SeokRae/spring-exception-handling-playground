package com.example.exception.playground.global.exception;

import com.example.exception.playground.global.error.ErrorCode;

public class AccessDeniedException extends BusinessException {

    public AccessDeniedException(String message) {
        super(ErrorCode.ACCESS_DENIED, message);
    }
}
