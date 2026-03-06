package com.example.exception.playground.global.filter;

import com.example.exception.playground.global.error.ErrorCode;
import com.example.exception.playground.global.error.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.exception.playground.global.error.ErrorCode;
import com.example.exception.playground.global.error.ErrorResponse;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.http.MediaType;

import java.io.IOException;

public final class FilterErrorResponseWriter {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    private FilterErrorResponseWriter() {
    }

    public static void write(HttpServletResponse response, ErrorCode errorCode, String message) throws IOException {
        ErrorResponse errorResponse = ErrorResponse.of(errorCode, message);
        response.setStatus(errorCode.getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(OBJECT_MAPPER.writeValueAsString(errorResponse));
    }
}
