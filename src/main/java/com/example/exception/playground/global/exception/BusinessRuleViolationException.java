package com.example.exception.playground.global.exception;

import com.example.exception.playground.global.error.ErrorCode;

public class BusinessRuleViolationException extends BusinessException {

    public BusinessRuleViolationException(String message) {
        super(ErrorCode.BUSINESS_RULE_VIOLATION, message);
    }
}
