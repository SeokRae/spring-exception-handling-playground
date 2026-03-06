package com.example.exception.playground.global.exception;

import com.example.exception.playground.global.error.ErrorCode;

public class DuplicateResourceException extends BusinessException {

    public DuplicateResourceException(String message) {
        super(ErrorCode.DUPLICATE_RESOURCE, message);
    }
}
