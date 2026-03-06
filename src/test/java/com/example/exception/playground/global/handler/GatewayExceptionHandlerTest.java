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
        @DisplayName("GatewayErrorException returns 502 with RESUBMIT strategy")
        void gatewayError() throws Exception {
            mockMvc.perform(get("/api/samples/gateway-error"))
                    .andDo(print())
                    .andExpect(status().isBadGateway())
                    .andExpect(jsonPath("$.code").value("G001"))
                    .andExpect(jsonPath("$.status").value(502))
                    .andExpect(jsonPath("$.message").value("Payment service connection refused"))
                    .andExpect(jsonPath("$.retryable").value(true))
                    .andExpect(jsonPath("$.retryStrategy").value("RESUBMIT"))
                    .andExpect(jsonPath("$.traceId").exists())
                    .andExpect(jsonPath("$.timestamp").exists());
        }
    }

    @Nested
    @DisplayName("504 Gateway Timeout - 하위 서비스 응답 시간 초과")
    class GatewayTimeoutTests {

        @Test
        @DisplayName("GatewayTimeoutException returns 504 with RESUBMIT strategy")
        void gatewayTimeout() throws Exception {
            mockMvc.perform(get("/api/samples/gateway-timeout"))
                    .andDo(print())
                    .andExpect(status().isGatewayTimeout())
                    .andExpect(jsonPath("$.code").value("G002"))
                    .andExpect(jsonPath("$.status").value(504))
                    .andExpect(jsonPath("$.message").value("Payment service did not respond within 5000ms"))
                    .andExpect(jsonPath("$.retryable").value(true))
                    .andExpect(jsonPath("$.retryStrategy").value("RESUBMIT"))
                    .andExpect(jsonPath("$.traceId").exists())
                    .andExpect(jsonPath("$.timestamp").exists());
        }
    }

    @Nested
    @DisplayName("503 Service Unavailable - 하위 서비스 과부하/점검")
    class ServiceUnavailableTests {

        @Test
        @DisplayName("ServiceUnavailableException returns 503 with Retry-After header")
        void serviceUnavailableWithRetryAfter() throws Exception {
            mockMvc.perform(get("/api/samples/service-unavailable"))
                    .andDo(print())
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(jsonPath("$.code").value("G003"))
                    .andExpect(jsonPath("$.status").value(503))
                    .andExpect(jsonPath("$.message").value("Payment service is under maintenance"))
                    .andExpect(jsonPath("$.retryable").value(true))
                    .andExpect(jsonPath("$.retryStrategy").value("RESUBMIT"))
                    .andExpect(header().string("Retry-After", "30"));
        }

        @Test
        @DisplayName("ServiceUnavailableException without retryAfterSeconds does not include Retry-After header")
        void serviceUnavailableWithoutRetryAfter() throws Exception {
            mockMvc.perform(get("/api/samples/service-unavailable-no-retry"))
                    .andDo(print())
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(jsonPath("$.code").value("G003"))
                    .andExpect(jsonPath("$.retryable").value(true))
                    .andExpect(jsonPath("$.retryStrategy").value("RESUBMIT"))
                    .andExpect(header().doesNotExist("Retry-After"));
        }
    }

    @Nested
    @DisplayName("202 Accepted - 하위 서비스 처리 중")
    class RequestInProgressTests {

        @Test
        @DisplayName("RequestInProgressException returns 202 with POLL_STATUS strategy")
        void requestInProgress() throws Exception {
            mockMvc.perform(get("/api/samples/request-in-progress"))
                    .andDo(print())
                    .andExpect(status().isAccepted())
                    .andExpect(jsonPath("$.code").value("P001"))
                    .andExpect(jsonPath("$.status").value(202))
                    .andExpect(jsonPath("$.message").value("Request is being processed by payment service"))
                    .andExpect(jsonPath("$.retryable").value(true))
                    .andExpect(jsonPath("$.retryStrategy").value("POLL_STATUS"))
                    .andExpect(jsonPath("$.traceId").exists())
                    .andExpect(jsonPath("$.timestamp").exists());
        }
    }
}
