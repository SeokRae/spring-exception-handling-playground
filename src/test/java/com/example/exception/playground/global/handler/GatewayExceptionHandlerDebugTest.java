package com.example.exception.playground.global.handler;

import com.example.exception.playground.sample.adapter.in.web.SampleGatewayController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SampleGatewayController.class)
@TestPropertySource(properties = "app.debug=true")
@DisplayName("Debug Mode - SampleGatewayController")
class GatewayExceptionHandlerDebugTest {

    @Autowired
    private MockMvc mockMvc;

    @Nested
    @DisplayName("502 Bad Gateway in debug mode")
    class GatewayErrorDebug {

        @Test
        @DisplayName("GatewayErrorException includes debug info without retry")
        void gatewayErrorWithDebug() throws Exception {
            mockMvc.perform(get("/api/samples/gateway-error"))
                    .andDo(print())
                    .andExpect(status().isBadGateway())
                    .andExpect(jsonPath("$.code").value("G001"))
                    .andExpect(jsonPath("$.retryable").doesNotExist())
                    .andExpect(jsonPath("$.retryStrategy").doesNotExist())
                    .andExpect(jsonPath("$.debugMessage").value("GatewayErrorException: Payment service connection refused"))
                    .andExpect(jsonPath("$.stackTrace").exists());
        }
    }

    @Nested
    @DisplayName("504 Gateway Timeout in debug mode")
    class GatewayTimeoutDebug {

        @Test
        @DisplayName("GatewayTimeoutException includes debug info")
        void gatewayTimeoutWithDebug() throws Exception {
            mockMvc.perform(get("/api/samples/gateway-timeout"))
                    .andDo(print())
                    .andExpect(status().isGatewayTimeout())
                    .andExpect(jsonPath("$.code").value("G002"))
                    .andExpect(jsonPath("$.retryable").doesNotExist())
                    .andExpect(jsonPath("$.retryStrategy").doesNotExist())
                    .andExpect(jsonPath("$.debugMessage").value("GatewayTimeoutException: Payment service did not respond within 5000ms"))
                    .andExpect(jsonPath("$.stackTrace").exists());
        }
    }

    @Nested
    @DisplayName("503 Service Unavailable in debug mode")
    class ServiceUnavailableDebug {

        @Test
        @DisplayName("ServiceUnavailableException includes debug info without retry")
        void serviceUnavailableWithDebug() throws Exception {
            mockMvc.perform(get("/api/samples/service-unavailable"))
                    .andDo(print())
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(jsonPath("$.code").value("G003"))
                    .andExpect(jsonPath("$.retryable").doesNotExist())
                    .andExpect(jsonPath("$.debugMessage").value("ServiceUnavailableException: Payment service is under maintenance"))
                    .andExpect(jsonPath("$.stackTrace").exists())
                    .andExpect(header().doesNotExist("Retry-After"));
        }
    }

    @Nested
    @DisplayName("503 Request In Progress in debug mode")
    class RequestInProgressDebug {

        @Test
        @DisplayName("RequestInProgressException includes debug info with RETRY_AFTER")
        void requestInProgressWithDebug() throws Exception {
            mockMvc.perform(get("/api/samples/request-in-progress"))
                    .andDo(print())
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(jsonPath("$.code").value("P001"))
                    .andExpect(jsonPath("$.retryable").value(true))
                    .andExpect(jsonPath("$.retryStrategy").value("RETRY_AFTER"))
                    .andExpect(jsonPath("$.debugMessage").value("RequestInProgressException: Request is being processed by payment service"))
                    .andExpect(jsonPath("$.stackTrace").exists())
                    .andExpect(header().string("Retry-After", "30"));
        }
    }
}
