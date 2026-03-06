package com.example.exception.playground.common;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class IdempotencyFilter extends OncePerRequestFilter {

    private static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";
    private static final Set<String> IDEMPOTENT_METHODS = Set.of("POST", "PUT", "PATCH");

    private final long ttlMillis;
    private final ConcurrentHashMap<String, IdempotencyEntry> cache = new ConcurrentHashMap<>();

    public IdempotencyFilter(long ttlMillis) {
        this.ttlMillis = ttlMillis;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (!IDEMPOTENT_METHODS.contains(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String idempotencyKey = request.getHeader(IDEMPOTENCY_KEY_HEADER);
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            FilterErrorResponseWriter.write(response, ErrorCode.IDEMPOTENCY_KEY_MISSING,
                    "Idempotency-Key header is required for " + request.getMethod() + " requests");
            return;
        }

        evictExpired();

        // Read body for fingerprint (use CachedBodyHttpServletRequest)
        byte[] body = request.getInputStream().readAllBytes();
        String fingerprint = buildFingerprint(request.getMethod(), request.getRequestURI(), body);

        // Try to claim this key atomically
        IdempotencyEntry newEntry = IdempotencyEntry.inProgress(fingerprint);
        IdempotencyEntry existing = cache.putIfAbsent(idempotencyKey, newEntry);

        if (existing != null) {
            if (existing.isExpired(System.currentTimeMillis(), ttlMillis)) {
                // Expired entry - remove and retry
                cache.remove(idempotencyKey, existing);
                // Allow re-processing by falling through
                cache.putIfAbsent(idempotencyKey, newEntry);
            } else {
                handleExistingEntry(existing, fingerprint, response);
                return;
            }
        }

        // Process the request with a wrapped request that replays the body
        CachedBodyHttpServletRequest cachedRequest = new CachedBodyHttpServletRequest(request, body);
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);

        try {
            filterChain.doFilter(cachedRequest, wrappedResponse);
        } finally {
            wrappedResponse.copyBodyToResponse();
        }

        // Cache only 2xx responses
        if (wrappedResponse.getStatus() >= 200 && wrappedResponse.getStatus() < 300) {
            cache.put(idempotencyKey, IdempotencyEntry.completed(fingerprint, wrappedResponse.getStatus()));
        } else {
            cache.remove(idempotencyKey);
        }
    }

    private void handleExistingEntry(IdempotencyEntry existing, String fingerprint,
                                     HttpServletResponse response) throws IOException {
        if (!existing.fingerprint().equals(fingerprint)) {
            FilterErrorResponseWriter.write(response, ErrorCode.IDEMPOTENCY_FINGERPRINT_MISMATCH,
                    "Request does not match the original request for this Idempotency-Key");
            return;
        }

        if (existing.status() == IdempotencyStatus.IN_PROGRESS) {
            FilterErrorResponseWriter.write(response, ErrorCode.IDEMPOTENCY_KEY_REUSED,
                    "A request with this Idempotency-Key is already in progress");
            return;
        }

        // Return cached response status
        response.setStatus(existing.httpStatus());
    }

    private String buildFingerprint(String method, String uri, byte[] body) {
        String raw = method + "|" + uri + "|" + new String(body, StandardCharsets.UTF_8);
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            return raw;
        }
    }

    private void evictExpired() {
        long now = System.currentTimeMillis();
        cache.values().removeIf(entry -> entry.isExpired(now, ttlMillis));
    }

    enum IdempotencyStatus {
        IN_PROGRESS, COMPLETED
    }

    record IdempotencyEntry(String fingerprint, IdempotencyStatus status, int httpStatus, long createdAt) {

        static IdempotencyEntry inProgress(String fingerprint) {
            return new IdempotencyEntry(fingerprint, IdempotencyStatus.IN_PROGRESS, 0, System.currentTimeMillis());
        }

        static IdempotencyEntry completed(String fingerprint, int httpStatus) {
            return new IdempotencyEntry(fingerprint, IdempotencyStatus.COMPLETED, httpStatus, System.currentTimeMillis());
        }

        boolean isExpired(long now, long ttlMillis) {
            return now - createdAt > ttlMillis;
        }
    }
}
