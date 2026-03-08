package com.example.exception.playground.global.handler;

import com.example.exception.playground.sample.adapter.in.web.SampleGatewayController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SampleGatewayController.class)
class GatewayExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Nested
    @DisplayName("502 Bad Gateway - 하위 서비스 연결 실패")
    class BadGatewayTests {

        @Test
        @DisplayName("GatewayErrorException returns simple 502 without retry info")
        void gatewayError() throws Exception {
            mockMvc.perform(get("/api/samples/gateway-error"))
                    .andDo(print())
                    .andExpect(status().isBadGateway())
                    .andExpect(jsonPath("$.code").value("G001"))
                    .andExpect(jsonPath("$.status").value(502))
                    .andExpect(jsonPath("$.message").value("Payment service connection refused"))
                    .andExpect(jsonPath("$.retryable").doesNotExist())
                    .andExpect(jsonPath("$.retryStrategy").doesNotExist())
                    .andExpect(jsonPath("$.traceId").exists())
                    .andExpect(jsonPath("$.timestamp").exists());
        }
    }

    @Nested
    @DisplayName("504 Gateway Timeout - 하위 서비스 응답 시간 초과")
    class GatewayTimeoutTests {

        @Test
        @DisplayName("GatewayTimeoutException returns simple 504 without retry info")
        void gatewayTimeout() throws Exception {
            mockMvc.perform(get("/api/samples/gateway-timeout"))
                    .andDo(print())
                    .andExpect(status().isGatewayTimeout())
                    .andExpect(jsonPath("$.code").value("G002"))
                    .andExpect(jsonPath("$.status").value(504))
                    .andExpect(jsonPath("$.message").value("Payment service did not respond within 5000ms"))
                    .andExpect(jsonPath("$.retryable").doesNotExist())
                    .andExpect(jsonPath("$.retryStrategy").doesNotExist())
                    .andExpect(jsonPath("$.traceId").exists())
                    .andExpect(jsonPath("$.timestamp").exists());
        }
    }

    @Nested
    @DisplayName("503 Service Unavailable - 서버 접근 불가")
    class ServiceUnavailableTests {

        @Test
        @DisplayName("ServiceUnavailableException returns simple 503 without retry info")
        void serviceUnavailable() throws Exception {
            mockMvc.perform(get("/api/samples/service-unavailable"))
                    .andDo(print())
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(jsonPath("$.code").value("G003"))
                    .andExpect(jsonPath("$.status").value(503))
                    .andExpect(jsonPath("$.message").value("Payment service is under maintenance"))
                    .andExpect(jsonPath("$.retryable").doesNotExist())
                    .andExpect(jsonPath("$.retryStrategy").doesNotExist())
                    .andExpect(header().doesNotExist("Retry-After"));
        }
    }

    @Nested
    @DisplayName("503 Request In Progress - 처리 중 재시도 요청")
    class RequestInProgressTests {

        @Test
        @DisplayName("RequestInProgressException returns 503 with Retry-After header")
        void requestInProgressWithRetryAfter() throws Exception {
            mockMvc.perform(get("/api/samples/request-in-progress"))
                    .andDo(print())
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(jsonPath("$.code").value("P001"))
                    .andExpect(jsonPath("$.status").value(503))
                    .andExpect(jsonPath("$.message").value("Request is being processed by payment service"))
                    .andExpect(jsonPath("$.retryable").value(true))
                    .andExpect(jsonPath("$.retryStrategy").value("RETRY_AFTER"))
                    .andExpect(header().string("Retry-After", "30"));
        }

        @Test
        @DisplayName("RequestInProgressException without retryAfterSeconds does not include Retry-After header")
        void requestInProgressWithoutRetryAfter() throws Exception {
            mockMvc.perform(get("/api/samples/request-in-progress-no-retry"))
                    .andDo(print())
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(jsonPath("$.code").value("P001"))
                    .andExpect(jsonPath("$.retryable").value(true))
                    .andExpect(jsonPath("$.retryStrategy").value("RETRY_AFTER"))
                    .andExpect(header().doesNotExist("Retry-After"));
        }
    }
}
