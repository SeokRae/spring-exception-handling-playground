package com.example.exception.playground.global.exception;

import com.example.exception.playground.global.error.ErrorCode;

public class GatewayTimeoutException extends GatewayException {

    public GatewayTimeoutException(String message) {
        super(ErrorCode.GATEWAY_TIMEOUT, message);
    }

    public GatewayTimeoutException(String message, Throwable cause) {
        super(ErrorCode.GATEWAY_TIMEOUT, message, cause);
    }
}
