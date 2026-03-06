package com.example.exception.playground.global.filter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;

@Configuration
@Profile("dev")
public class FilterConfig {

    private static final int RATE_LIMIT_ORDER = Ordered.HIGHEST_PRECEDENCE;
    private static final int IDEMPOTENCY_ORDER = Ordered.HIGHEST_PRECEDENCE + 1;
    private static final int LOGGING_ORDER = Ordered.HIGHEST_PRECEDENCE + 2;

    @Bean
    public FilterRegistrationBean<RateLimitFilter> rateLimitFilter(
            @Value("${app.filter.rate-limit.max-requests}") int maxRequests,
            @Value("${app.filter.rate-limit.window-millis}") long windowMillis) {
        FilterRegistrationBean<RateLimitFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new RateLimitFilter(maxRequests, windowMillis));
        registration.addUrlPatterns("/api/*");
        registration.setOrder(RATE_LIMIT_ORDER);
        return registration;
    }

    @Bean
    public FilterRegistrationBean<IdempotencyFilter> idempotencyFilter(
            @Value("${app.filter.idempotency.ttl-millis}") long ttlMillis) {
        FilterRegistrationBean<IdempotencyFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new IdempotencyFilter(ttlMillis));
        registration.addUrlPatterns("/api/*");
        registration.setOrder(IDEMPOTENCY_ORDER);
        return registration;
    }

    @Bean
    public FilterRegistrationBean<RequestResponseLoggingFilter> loggingFilter() {
        FilterRegistrationBean<RequestResponseLoggingFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new RequestResponseLoggingFilter());
        registration.addUrlPatterns("/api/*");
        registration.setOrder(LOGGING_ORDER);
        return registration;
    }
}
