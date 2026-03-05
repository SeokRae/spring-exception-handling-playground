package com.example.exception.playground.common;

public class DuplicateResourceException extends BusinessException {

    public DuplicateResourceException(String message) {
        super(ErrorCode.DUPLICATE_RESOURCE, message);
    }
}
