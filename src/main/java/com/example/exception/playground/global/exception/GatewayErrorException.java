package com.example.exception.playground.global.exception;

import com.example.exception.playground.global.error.ErrorCode;

public class GatewayErrorException extends GatewayException {

    public GatewayErrorException(String message) {
        super(ErrorCode.GATEWAY_ERROR, message);
    }

    public GatewayErrorException(String message, Throwable cause) {
        super(ErrorCode.GATEWAY_ERROR, message, cause);
    }
}
