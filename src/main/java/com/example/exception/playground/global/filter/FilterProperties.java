package com.example.exception.playground.global.filter;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.filter")
@Validated
public record FilterProperties(
        @Valid @NotNull RateLimitProperties rateLimit,
        @Valid @NotNull IdempotencyProperties idempotency
) {
    public record RateLimitProperties(
            @Positive int maxRequests,
            @NotNull Duration window
    ) {}

    public record IdempotencyProperties(
            @NotNull Duration ttl
    ) {}
}
