package com.example.exception.playground.common;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.UUID;

public class RequestResponseLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestResponseLoggingFilter.class);
    private static final String TRACE_ID = "traceId";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String traceId = UUID.randomUUID().toString().substring(0, 8);
        MDC.put(TRACE_ID, traceId);

        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);

        long startTime = System.currentTimeMillis();

        try {
            logRequest(wrappedRequest, traceId);
            filterChain.doFilter(wrappedRequest, wrappedResponse);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            logResponse(wrappedRequest, wrappedResponse, traceId, duration);
            wrappedResponse.copyBodyToResponse();
            MDC.remove(TRACE_ID);
        }
    }

    private void logRequest(ContentCachingRequestWrapper request, String traceId) {
        StringBuilder headers = new StringBuilder();
        Collections.list(request.getHeaderNames()).forEach(name ->
                headers.append(name).append(": ").append(request.getHeader(name)).append(", "));

        log.info("[{}] >>> {} {} | Headers: [{}]",
                traceId, request.getMethod(), request.getRequestURI(),
                headers.length() > 2 ? headers.substring(0, headers.length() - 2) : "");
    }

    private void logResponse(ContentCachingRequestWrapper request, ContentCachingResponseWrapper response,
                             String traceId, long duration) {
        int status = response.getStatus();
        String body = new String(response.getContentAsByteArray(), StandardCharsets.UTF_8);

        String requestBody = new String(request.getContentAsByteArray(), StandardCharsets.UTF_8);
        if (!requestBody.isBlank()) {
            log.info("[{}] >>> Request Body: {}", traceId, requestBody);
        }

        if (status >= 400) {
            log.warn("[{}] <<< {} {} | Status: {} | {}ms | Body: {}",
                    traceId, request.getMethod(), request.getRequestURI(), status, duration, body);
        } else {
            log.info("[{}] <<< {} {} | Status: {} | {}ms",
                    traceId, request.getMethod(), request.getRequestURI(), status, duration);
        }
    }
}
