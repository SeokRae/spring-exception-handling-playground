package com.example.exception.playground.global.filter;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration
@ConditionalOnProperty(prefix = "app.filter", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(FilterProperties.class)
public class FilterConfig {

    private static final int LOGGING_ORDER = Ordered.HIGHEST_PRECEDENCE;
    private static final int RATE_LIMIT_ORDER = Ordered.HIGHEST_PRECEDENCE + 1;
    private static final int IDEMPOTENCY_ORDER = Ordered.HIGHEST_PRECEDENCE + 2;

    @Bean
    public FilterRegistrationBean<RateLimitFilter> rateLimitFilter(FilterProperties properties) {
        FilterRegistrationBean<RateLimitFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new RateLimitFilter(
                properties.rateLimit().maxRequests(),
                properties.rateLimit().window().toMillis()));
        registration.addUrlPatterns("/api/*");
        registration.setOrder(RATE_LIMIT_ORDER);
        return registration;
    }

    @Bean
    public FilterRegistrationBean<IdempotencyFilter> idempotencyFilter(FilterProperties properties) {
        FilterRegistrationBean<IdempotencyFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new IdempotencyFilter(properties.idempotency().ttl().toMillis()));
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
