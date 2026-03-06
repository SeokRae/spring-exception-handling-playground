package com.example.exception.playground.global.exception;

import com.example.exception.playground.global.error.ErrorCode;

public class UnauthorizedException extends BusinessException {

    public UnauthorizedException(String message) {
        super(ErrorCode.UNAUTHORIZED, message);
    }
}
