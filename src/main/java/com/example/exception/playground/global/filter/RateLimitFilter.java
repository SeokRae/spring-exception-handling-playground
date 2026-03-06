package com.example.exception.playground.global.filter;

import com.example.exception.playground.global.error.ErrorCode;
import jakarta.servlet.FilterChain;
import com.example.exception.playground.global.error.ErrorCode;
import jakarta.servlet.ServletException;
import com.example.exception.playground.global.error.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import com.example.exception.playground.global.error.ErrorCode;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class RateLimitFilter extends OncePerRequestFilter {

    private final int maxRequests;
    private final long windowMillis;
    private final ConcurrentHashMap<String, RateLimitBucket> buckets = new ConcurrentHashMap<>();

    public RateLimitFilter(int maxRequests, long windowMillis) {
        this.maxRequests = maxRequests;
        this.windowMillis = windowMillis;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String clientIp = request.getRemoteAddr();
        long now = System.currentTimeMillis();

        RateLimitBucket bucket = buckets.compute(clientIp, (key, existing) -> {
            if (existing == null || existing.isExpired(now)) {
                return new RateLimitBucket(now, windowMillis);
            }
            return existing;
        });

        if (bucket.incrementAndCheck(maxRequests)) {
            filterChain.doFilter(request, response);
        } else {
            long retryAfterSeconds = Math.max(1, (bucket.getResetTime() - now) / 1000);
            response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
            FilterErrorResponseWriter.write(response, ErrorCode.RATE_LIMIT_EXCEEDED,
                    "Rate limit exceeded. Try again in " + retryAfterSeconds + " seconds");
        }
    }

    static class RateLimitBucket {
        private final long windowStart;
        private final long windowMillis;
        private final AtomicInteger count = new AtomicInteger(0);

        RateLimitBucket(long windowStart, long windowMillis) {
            this.windowStart = windowStart;
            this.windowMillis = windowMillis;
        }

        boolean isExpired(long now) {
            return now >= windowStart + windowMillis;
        }

        long getResetTime() {
            return windowStart + windowMillis;
        }

        boolean incrementAndCheck(int maxRequests) {
            return count.incrementAndGet() <= maxRequests;
        }
    }
}
