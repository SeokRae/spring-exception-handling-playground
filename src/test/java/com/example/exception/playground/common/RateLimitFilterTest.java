package com.example.exception.playground.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("RateLimitFilter")
class RateLimitFilterTest {

    @Nested
    @DisplayName("dev 프로파일에서 Rate Limit 필터 활성화")
    @SpringBootTest
    @AutoConfigureMockMvc
    @ActiveProfiles("dev")
    @DirtiesContext
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class DevProfileTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ApplicationContext context;

        @Test
        @Order(1)
        @DisplayName("Rate Limit 필터 빈이 등록되어 있다")
        @SuppressWarnings("rawtypes")
        void filterBeanExists() {
            boolean hasRateLimitFilter = context.getBeansOfType(FilterRegistrationBean.class).values().stream()
                    .anyMatch(bean -> bean.getFilter() instanceof RateLimitFilter);
            assertThat(hasRateLimitFilter).isTrue();
        }

        @Test
        @Order(2)
        @DisplayName("제한 내 요청은 정상 응답")
        void withinLimit() throws Exception {
            mockMvc.perform(get("/api/samples/1"))
                    .andExpect(status().isOk());
        }

        @Test
        @Order(3)
        @DisplayName("제한 초과 시 429 응답 + Retry-After 헤더")
        void exceedsLimit() throws Exception {
            // 남은 quota를 모두 소진 (이전 테스트에서 사용된 횟수에 관계없이)
            for (int i = 0; i < 20; i++) {
                mockMvc.perform(get("/api/samples/1"));
            }

            // 확실히 초과된 상태에서 429 확인
            mockMvc.perform(get("/api/samples/1"))
                    .andExpect(status().isTooManyRequests())
                    .andExpect(jsonPath("$.code").value("R001"))
                    .andExpect(header().exists("Retry-After"));
        }
    }

    @Nested
    @DisplayName("기본 프로파일에서 Rate Limit 필터 비활성화")
    @SpringBootTest
    @AutoConfigureMockMvc
    class DefaultProfileTest {

        @Autowired
        private ApplicationContext context;

        @Test
        @DisplayName("Rate Limit 필터 빈이 등록되지 않는다")
        @SuppressWarnings("rawtypes")
        void filterBeanNotExists() {
            boolean hasRateLimitFilter = context.getBeansOfType(FilterRegistrationBean.class).values().stream()
                    .anyMatch(bean -> bean.getFilter() instanceof RateLimitFilter);
            assertThat(hasRateLimitFilter).isFalse();
        }
    }
}
