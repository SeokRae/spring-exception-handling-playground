package com.example.exception.playground.global.handler;

import com.example.exception.playground.global.error.ErrorCode;
import com.example.exception.playground.global.error.ErrorResponse;
import com.example.exception.playground.global.exception.BusinessException;
import com.example.exception.playground.global.exception.GatewayException;
import com.example.exception.playground.global.exception.RequestInProgressException;
import com.example.exception.playground.global.exception.ServiceUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @Value("${app.debug:false}")
    private boolean debug;

    @ExceptionHandler(MethodArgumentNotValidException.class)
    protected ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        log.warn("Validation failed: {}", e.getMessage());
        List<ErrorResponse.FieldError> fieldErrors = e.getBindingResult().getFieldErrors().stream()
                .map(error -> ErrorResponse.FieldError.of(
                        error.getField(),
                        error.getRejectedValue() == null ? "" : error.getRejectedValue().toString(),
                        error.getDefaultMessage()))
                .toList();
        ErrorResponse response = debug
                ? ErrorResponse.withDebug(ErrorCode.INVALID_INPUT, fieldErrors, e)
                : ErrorResponse.of(ErrorCode.INVALID_INPUT, fieldErrors);
        return ResponseEntity.status(ErrorCode.INVALID_INPUT.getStatus()).body(response);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    protected ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException e) {
        log.warn("Type mismatch: {}", e.getMessage());
        String message = String.format("'%s' should be of type '%s'", e.getName(),
                e.getRequiredType() != null ? e.getRequiredType().getSimpleName() : "unknown");
        ErrorResponse response = debug
                ? ErrorResponse.withDebug(ErrorCode.INVALID_INPUT, message, e)
                : ErrorResponse.of(ErrorCode.INVALID_INPUT, message);
        return ResponseEntity.status(ErrorCode.INVALID_INPUT.getStatus()).body(response);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    protected ResponseEntity<ErrorResponse> handleMissingServletRequestParameter(MissingServletRequestParameterException e) {
        log.warn("Missing parameter: {}", e.getMessage());
        String message = String.format("Required parameter '%s' of type '%s' is missing",
                e.getParameterName(), e.getParameterType());
        ErrorResponse response = debug
                ? ErrorResponse.withDebug(ErrorCode.MISSING_PARAMETER, message, e)
                : ErrorResponse.of(ErrorCode.MISSING_PARAMETER, message);
        return ResponseEntity.status(ErrorCode.MISSING_PARAMETER.getStatus()).body(response);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    protected ResponseEntity<ErrorResponse> handleHttpMediaTypeNotSupported(HttpMediaTypeNotSupportedException e) {
        log.warn("Unsupported media type: {}", e.getMessage());
        ErrorResponse response = debug
                ? ErrorResponse.withDebug(ErrorCode.UNSUPPORTED_MEDIA_TYPE, e.getMessage(), e)
                : ErrorResponse.of(ErrorCode.UNSUPPORTED_MEDIA_TYPE, e.getMessage());
        return ResponseEntity.status(ErrorCode.UNSUPPORTED_MEDIA_TYPE.getStatus()).body(response);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    protected ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(HttpMessageNotReadableException e) {
        log.warn("Message not readable: {}", e.getMessage());
        ErrorResponse response = debug
                ? ErrorResponse.withDebug(ErrorCode.MESSAGE_NOT_READABLE, e)
                : ErrorResponse.of(ErrorCode.MESSAGE_NOT_READABLE);
        return ResponseEntity.status(ErrorCode.MESSAGE_NOT_READABLE.getStatus()).body(response);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    protected ResponseEntity<ErrorResponse> handleHttpRequestMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        log.warn("Method not supported: {}", e.getMessage());
        ErrorResponse response = debug
                ? ErrorResponse.withDebug(ErrorCode.METHOD_NOT_ALLOWED, e)
                : ErrorResponse.of(ErrorCode.METHOD_NOT_ALLOWED);
        return ResponseEntity.status(ErrorCode.METHOD_NOT_ALLOWED.getStatus()).body(response);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    protected ResponseEntity<ErrorResponse> handleNoResourceFound(NoResourceFoundException e) {
        log.warn("Resource not found: {}", e.getMessage());
        ErrorResponse response = debug
                ? ErrorResponse.withDebug(ErrorCode.RESOURCE_NOT_FOUND, e.getMessage(), e)
                : ErrorResponse.of(ErrorCode.RESOURCE_NOT_FOUND, e.getMessage());
        return ResponseEntity.status(ErrorCode.RESOURCE_NOT_FOUND.getStatus()).body(response);
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    protected ResponseEntity<ErrorResponse> handleNoHandlerFound(NoHandlerFoundException e) {
        log.warn("No handler found: {}", e.getMessage());
        ErrorResponse response = debug
                ? ErrorResponse.withDebug(ErrorCode.RESOURCE_NOT_FOUND, e.getMessage(), e)
                : ErrorResponse.of(ErrorCode.RESOURCE_NOT_FOUND, e.getMessage());
        return ResponseEntity.status(ErrorCode.RESOURCE_NOT_FOUND.getStatus()).body(response);
    }

    @ExceptionHandler(RequestInProgressException.class)
    protected ResponseEntity<ErrorResponse> handleRequestInProgress(RequestInProgressException e) {
        log.info("Request in progress: {}", e.getMessage());
        ErrorCode errorCode = e.getErrorCode();
        ErrorResponse response = debug
                ? ErrorResponse.retryableWithDebug(errorCode, e.getMessage(), "POLL_STATUS", e)
                : ErrorResponse.retryable(errorCode, e.getMessage(), "POLL_STATUS");
        return ResponseEntity.status(errorCode.getStatus()).body(response);
    }

    @ExceptionHandler(ServiceUnavailableException.class)
    protected ResponseEntity<ErrorResponse> handleServiceUnavailable(ServiceUnavailableException e) {
        log.warn("Service unavailable: {}", e.getMessage());
        ErrorCode errorCode = e.getErrorCode();
        ErrorResponse response = debug
                ? ErrorResponse.retryableWithDebug(errorCode, e.getMessage(), "RESUBMIT", e)
                : ErrorResponse.retryable(errorCode, e.getMessage(), "RESUBMIT");
        ResponseEntity.BodyBuilder builder = ResponseEntity.status(errorCode.getStatus());
        if (e.getRetryAfterSeconds() != null) {
            builder.header(HttpHeaders.RETRY_AFTER, String.valueOf(e.getRetryAfterSeconds()));
        }
        return builder.body(response);
    }

    @ExceptionHandler(GatewayException.class)
    protected ResponseEntity<ErrorResponse> handleGatewayException(GatewayException e) {
        log.error("Gateway error: {}", e.getMessage(), e);
        ErrorCode errorCode = e.getErrorCode();
        ErrorResponse response = debug
                ? ErrorResponse.retryableWithDebug(errorCode, e.getMessage(), "RESUBMIT", e)
                : ErrorResponse.retryable(errorCode, e.getMessage(), "RESUBMIT");
        return ResponseEntity.status(errorCode.getStatus()).body(response);
    }

    @ExceptionHandler(BusinessException.class)
    protected ResponseEntity<ErrorResponse> handleBusinessException(BusinessException e) {
        log.warn("Business exception: {}", e.getMessage());
        ErrorCode errorCode = e.getErrorCode();
        ErrorResponse response = debug
                ? ErrorResponse.withDebug(errorCode, e.getMessage(), e)
                : ErrorResponse.of(errorCode, e.getMessage());
        return ResponseEntity.status(errorCode.getStatus()).body(response);
    }

    @ExceptionHandler(Exception.class)
    protected ResponseEntity<ErrorResponse> handleException(Exception e) {
        log.error("Unexpected error occurred", e);
        ErrorResponse response = debug
                ? ErrorResponse.withDebug(ErrorCode.INTERNAL_SERVER_ERROR, e)
                : ErrorResponse.of(ErrorCode.INTERNAL_SERVER_ERROR);
        return ResponseEntity.status(ErrorCode.INTERNAL_SERVER_ERROR.getStatus()).body(response);
    }
}
