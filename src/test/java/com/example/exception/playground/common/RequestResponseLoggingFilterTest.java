package com.example.exception.playground.common;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("RequestResponseLoggingFilter")
class RequestResponseLoggingFilterTest {

    @Nested
    @DisplayName("dev 프로파일에서 로깅 필터 활성화")
    @SpringBootTest
    @AutoConfigureMockMvc
    @ActiveProfiles("dev")
    class DevProfileTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ApplicationContext context;

        @Test
        @DisplayName("로깅 필터 빈이 등록되어 있다")
        void filterBeanExists() {
            assertThat(context.getBeansOfType(FilterRegistrationBean.class))
                    .isNotEmpty();
        }

        @Test
        @DisplayName("정상 요청 시 로깅 필터를 거쳐 200 응답")
        void successRequestLogged() throws Exception {
            mockMvc.perform(get("/api/samples/1"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1));
        }

        @Test
        @DisplayName("에러 요청 시 로깅 필터를 거쳐 에러 응답 바디 로깅")
        void errorRequestLogged() throws Exception {
            mockMvc.perform(get("/api/samples/0"))
                    .andDo(print())
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("C006"))
                    .andExpect(jsonPath("$.debugMessage").exists());
        }

        @Test
        @DisplayName("POST 요청 바디도 로깅")
        void postRequestBodyLogged() throws Exception {
            mockMvc.perform(post("/api/samples")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"name": "test", "age": 25}
                                    """))
                    .andDo(print())
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("기본 프로파일에서 로깅 필터 비활성화")
    @SpringBootTest
    @AutoConfigureMockMvc
    class DefaultProfileTest {

        @Autowired
        private ApplicationContext context;

        @Test
        @DisplayName("로깅 필터 빈이 등록되지 않는다")
        void filterBeanNotExists() {
            assertThat(context.getBeansOfType(LoggingFilterConfig.class)).isEmpty();
        }
    }
}
