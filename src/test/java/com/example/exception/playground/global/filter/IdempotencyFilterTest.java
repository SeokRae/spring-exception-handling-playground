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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("IdempotencyFilter")
class IdempotencyFilterTest {

    @Nested
    @DisplayName("dev 프로파일에서 Idempotency 필터 활성화")
    @SpringBootTest
    @AutoConfigureMockMvc
    @ActiveProfiles("dev")
    class DevProfileTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ApplicationContext context;

        @Test
        @DisplayName("Idempotency 필터 빈이 등록되어 있다")
        @SuppressWarnings("rawtypes")
        void filterBeanExists() {
            boolean hasIdempotencyFilter = context.getBeansOfType(FilterRegistrationBean.class).values().stream()
                    .anyMatch(bean -> bean.getFilter() instanceof IdempotencyFilter);
            assertThat(hasIdempotencyFilter).isTrue();
        }

        @Test
        @DisplayName("GET 요청에는 Idempotency-Key가 불필요")
        void getRequestNoKeyRequired() throws Exception {
            mockMvc.perform(get("/api/samples/1"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("POST 요청 시 Idempotency-Key 헤더 없으면 400")
        void postWithoutKey() throws Exception {
            mockMvc.perform(post("/api/samples")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"name": "test", "age": 25}
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("I001"));
        }

        @Test
        @DisplayName("POST 요청 시 Idempotency-Key 포함하면 정상 처리")
        void postWithKey() throws Exception {
            String key = UUID.randomUUID().toString();
            mockMvc.perform(post("/api/samples")
                            .header("Idempotency-Key", key)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"name": "test", "age": 25}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result").value("created"));
        }

        @Test
        @DisplayName("동일 키 + 동일 바디 재요청 시 캐싱된 응답 반환 (200)")
        void duplicateRequestReturnsCached() throws Exception {
            String key = UUID.randomUUID().toString();
            String body = """
                    {"name": "idempotent", "age": 30}
                    """;

            // First request
            mockMvc.perform(post("/api/samples")
                            .header("Idempotency-Key", key)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk());

            // Second request with same key + same body
            mockMvc.perform(post("/api/samples")
                            .header("Idempotency-Key", key)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("동일 키 + 다른 바디 시 422 응답")
        void sameKeyDifferentBody() throws Exception {
            String key = UUID.randomUUID().toString();

            // First request
            mockMvc.perform(post("/api/samples")
                            .header("Idempotency-Key", key)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"name": "first", "age": 20}
                                    """))
                    .andExpect(status().isOk());

            // Second request with same key but different body
            mockMvc.perform(post("/api/samples")
                            .header("Idempotency-Key", key)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"name": "second", "age": 30}
                                    """))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.code").value("I003"));
        }
    }

    @Nested
    @DisplayName("기본 프로파일에서 Idempotency 필터 비활성화")
    @SpringBootTest
    @AutoConfigureMockMvc
    class DefaultProfileTest {

        @Autowired
        private ApplicationContext context;

        @Test
        @DisplayName("Idempotency 필터 빈이 등록되지 않는다")
        @SuppressWarnings("rawtypes")
        void filterBeanNotExists() {
            boolean hasIdempotencyFilter = context.getBeansOfType(FilterRegistrationBean.class).values().stream()
                    .anyMatch(bean -> bean.getFilter() instanceof IdempotencyFilter);
            assertThat(hasIdempotencyFilter).isFalse();
        }
    }
}
