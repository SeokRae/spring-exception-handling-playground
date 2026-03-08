package com.example.exception.playground.global.exception;

import com.example.exception.playground.global.error.ErrorCode;

public class ServiceUnavailableException extends GatewayException {

    public ServiceUnavailableException(String message) {
        super(ErrorCode.SERVICE_UNAVAILABLE, message);
    }
}
