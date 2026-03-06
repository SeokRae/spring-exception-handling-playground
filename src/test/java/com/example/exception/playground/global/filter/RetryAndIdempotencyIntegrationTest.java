package com.example.exception.playground.global.filter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("Retry & Idempotency 통합 테스트")
class RetryAndIdempotencyIntegrationTest {

    @Nested
    @DisplayName("Idempotency 엣지 케이스")
    @SpringBootTest
    @AutoConfigureMockMvc
    @ActiveProfiles("test-filter")
    class IdempotencyEdgeCaseTest {

        @Autowired
        private MockMvc mockMvc;

        @Test
        @DisplayName("PUT 요청에도 Idempotency-Key 필수")
        void putRequiresIdempotencyKey() throws Exception {
            mockMvc.perform(put("/api/samples/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"name": "update", "age": 25}
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("I001"))
                    .andExpect(jsonPath("$.message").value("Idempotency-Key header is required for PUT requests"));
        }

        @Test
        @DisplayName("PATCH 요청에도 Idempotency-Key 필수")
        void patchRequiresIdempotencyKey() throws Exception {
            mockMvc.perform(patch("/api/samples/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"name": "patch"}
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("I001"))
                    .andExpect(jsonPath("$.message").value("Idempotency-Key header is required for PATCH requests"));
        }

        @Test
        @DisplayName("DELETE 요청에는 Idempotency-Key 불필요")
        void deleteNoKeyRequired() throws Exception {
            mockMvc.perform(delete("/api/samples/1"))
                    .andExpect(status().isMethodNotAllowed());
        }

        @Test
        @DisplayName("4xx 응답은 캐시하지 않음 - 같은 키로 재시도 가능")
        void clientErrorNotCached() throws Exception {
            String key = UUID.randomUUID().toString();

            mockMvc.perform(post("/api/samples")
                            .header("Idempotency-Key", key)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"name": "te", "age": 0}
                                    """))
                    .andExpect(status().isBadRequest());

            mockMvc.perform(post("/api/samples")
                            .header("Idempotency-Key", key)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"name": "te", "age": 0}
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("C001"));
        }

        @Test
        @DisplayName("비즈니스 예외(422) 응답은 캐시하지 않음 - 같은 키로 재시도 가능")
        void businessErrorNotCached() throws Exception {
            String key = UUID.randomUUID().toString();
            String body = """
                    {"name": "test", "age": 150}
                    """;

            mockMvc.perform(post("/api/samples/business-rule")
                            .header("Idempotency-Key", key)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isUnprocessableEntity());

            mockMvc.perform(post("/api/samples/business-rule")
                            .header("Idempotency-Key", key)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.code").value("B002"));
        }
    }

    @Nested
    @DisplayName("Rate Limit 엣지 케이스")
    @SpringBootTest
    @AutoConfigureMockMvc
    @ActiveProfiles("test-filter")
    @DirtiesContext
    class RateLimitEdgeCaseTest {

        @Autowired
        private MockMvc mockMvc;

        @Test
        @DisplayName("429 응답 바디가 ErrorResponse 형식")
        void rateLimitResponseFormat() throws Exception {
            for (int i = 0; i < 20; i++) {
                mockMvc.perform(get("/api/samples/1"));
            }

            mockMvc.perform(get("/api/samples/1"))
                    .andExpect(status().isTooManyRequests())
                    .andExpect(jsonPath("$.code").value("R001"))
                    .andExpect(jsonPath("$.message").exists())
                    .andExpect(jsonPath("$.status").value(429))
                    .andExpect(jsonPath("$.timestamp").exists())
                    .andExpect(jsonPath("$.traceId").exists());
        }

        @Test
        @DisplayName("Retry-After 헤더 값이 숫자")
        void retryAfterIsNumeric() throws Exception {
            for (int i = 0; i < 20; i++) {
                mockMvc.perform(get("/api/samples/1"));
            }

            mockMvc.perform(get("/api/samples/1"))
                    .andExpect(status().isTooManyRequests())
                    .andExpect(header().string("Retry-After",
                            org.hamcrest.Matchers.matchesPattern("\\d+")));
        }
    }

    @Nested
    @DisplayName("필터 순서 검증")
    @SpringBootTest
    @AutoConfigureMockMvc
    @ActiveProfiles("test-filter")
    class FilterOrderTest {

        @Autowired
        private ApplicationContext context;

        @Test
        @DisplayName("Rate Limit → Idempotency → Logging 순서로 등록")
        @SuppressWarnings("rawtypes")
        void filterOrderIsCorrect() {
            List<FilterRegistrationBean> filters = context.getBeansOfType(FilterRegistrationBean.class)
                    .values().stream()
                    .filter(bean -> bean.getFilter() instanceof RateLimitFilter
                            || bean.getFilter() instanceof IdempotencyFilter
                            || bean.getFilter() instanceof RequestResponseLoggingFilter)
                    .sorted(Comparator.comparingInt(FilterRegistrationBean::getOrder))
                    .toList();

            assertThat(filters).hasSize(3);
            assertThat(filters.get(0).getFilter()).isInstanceOf(RateLimitFilter.class);
            assertThat(filters.get(1).getFilter()).isInstanceOf(IdempotencyFilter.class);
            assertThat(filters.get(2).getFilter()).isInstanceOf(RequestResponseLoggingFilter.class);
        }
    }

    @Nested
    @DisplayName("Rate Limit 윈도우 리셋 재시도")
    @SpringBootTest
    @AutoConfigureMockMvc
    @ActiveProfiles("test-ratelimit-reset")
    @DirtiesContext
    class RateLimitWindowResetTest {

        @Autowired
        private MockMvc mockMvc;

        @Test
        @DisplayName("Rate Limit 초과 후 윈도우 리셋 시 재시도 성공")
        void rateLimitWindowResetAllowsRetry() throws Exception {
            for (int i = 0; i < 3; i++) {
                mockMvc.perform(get("/api/samples/1"))
                        .andExpect(status().isOk());
            }

            mockMvc.perform(get("/api/samples/1"))
                    .andExpect(status().isTooManyRequests());

            Thread.sleep(1_100);

            mockMvc.perform(get("/api/samples/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1));
        }
    }

    @Nested
    @DisplayName("재시도(Retry) 시나리오 - 짧은 TTL")
    @SpringBootTest
    @AutoConfigureMockMvc
    @ActiveProfiles("test-retry")
    @DirtiesContext
    class RetryScenarioTest {

        @Autowired
        private MockMvc mockMvc;

        @Test
        @DisplayName("TTL 만료 후 같은 키 재사용 시 새 요청으로 처리")
        void ttlExpiredAllowsReuse() throws Exception {
            String key = UUID.randomUUID().toString();
            String body = """
                    {"name": "ttl-test", "age": 25}
                    """;

            mockMvc.perform(post("/api/samples")
                            .header("Idempotency-Key", key)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk());

            Thread.sleep(600);

            mockMvc.perform(post("/api/samples")
                            .header("Idempotency-Key", key)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result").value("created"));
        }

        @Test
        @DisplayName("멱등성 키로 실패 요청 후 TTL 만료 뒤 재시도 성공")
        void idempotencyRetryAfterFailureAndTtlExpiry() throws Exception {
            String key = UUID.randomUUID().toString();

            mockMvc.perform(post("/api/samples")
                            .header("Idempotency-Key", key)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"name": "x", "age": 0}
                                    """))
                    .andExpect(status().isBadRequest());

            Thread.sleep(600);

            mockMvc.perform(post("/api/samples")
                            .header("Idempotency-Key", key)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"name": "valid", "age": 25}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result").value("created"));
        }

        @Test
        @DisplayName("멱등성 키로 성공 요청 재시도 시 캐시 응답 반환")
        void idempotencyRetryAfterSuccess() throws Exception {
            String key = UUID.randomUUID().toString();
            String body = """
                    {"name": "retry-ok", "age": 30}
                    """;

            mockMvc.perform(post("/api/samples")
                            .header("Idempotency-Key", key)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result").value("created"));

            mockMvc.perform(post("/api/samples")
                            .header("Idempotency-Key", key)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk());
        }
    }
}
