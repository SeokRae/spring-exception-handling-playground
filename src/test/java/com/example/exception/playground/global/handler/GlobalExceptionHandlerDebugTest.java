package com.example.exception.playground.global.handler;

import com.example.exception.playground.sample.adapter.in.web.SampleController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SampleController.class)
@TestPropertySource(properties = "app.debug=true")
@DisplayName("Debug Mode - ErrorResponse includes debugMessage and stackTrace")
class GlobalExceptionHandlerDebugTest {

    @Autowired
    private MockMvc mockMvc;

    @Nested
    @DisplayName("Validation Error in debug mode")
    class ValidationDebug {

        @Test
        @DisplayName("includes debugMessage and stackTrace")
        void validationWithDebug() throws Exception {
            mockMvc.perform(post("/api/samples")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"name": "", "age": 25}
                                    """))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.traceId").exists())
                    .andExpect(jsonPath("$.debugMessage").exists())
                    .andExpect(jsonPath("$.stackTrace").exists());
        }
    }

    @Nested
    @DisplayName("Business Exception in debug mode")
    class BusinessExceptionDebug {

        @Test
        @DisplayName("NotFoundException includes debug info")
        void notFoundWithDebug() throws Exception {
            mockMvc.perform(get("/api/samples/0"))
                    .andDo(print())
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("C006"))
                    .andExpect(jsonPath("$.traceId").exists())
                    .andExpect(jsonPath("$.debugMessage").value("NotFoundException: Sample with id 0 not found"))
                    .andExpect(jsonPath("$.stackTrace").exists());
        }
    }

    @Nested
    @DisplayName("Unexpected Error in debug mode")
    class UnexpectedErrorDebug {

        @Test
        @DisplayName("RuntimeException includes full debug info")
        void unexpectedErrorWithDebug() throws Exception {
            mockMvc.perform(get("/api/samples/unexpected-error"))
                    .andDo(print())
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.code").value("C999"))
                    .andExpect(jsonPath("$.traceId").exists())
                    .andExpect(jsonPath("$.debugMessage").value("RuntimeException: Something went terribly wrong"))
                    .andExpect(jsonPath("$.stackTrace").exists());
        }
    }

    @Nested
    @DisplayName("Gateway Error in debug mode")
    class GatewayErrorDebug {

        @Test
        @DisplayName("GatewayErrorException includes debug info with retryable fields")
        void gatewayErrorWithDebug() throws Exception {
            mockMvc.perform(get("/api/samples/gateway-error"))
                    .andDo(print())
                    .andExpect(status().isBadGateway())
                    .andExpect(jsonPath("$.code").value("G001"))
                    .andExpect(jsonPath("$.retryable").value(true))
                    .andExpect(jsonPath("$.retryStrategy").value("RESUBMIT"))
                    .andExpect(jsonPath("$.debugMessage").value("GatewayErrorException: Payment service connection refused"))
                    .andExpect(jsonPath("$.stackTrace").exists());
        }

        @Test
        @DisplayName("GatewayTimeoutException includes debug info")
        void gatewayTimeoutWithDebug() throws Exception {
            mockMvc.perform(get("/api/samples/gateway-timeout"))
                    .andDo(print())
                    .andExpect(status().isGatewayTimeout())
                    .andExpect(jsonPath("$.code").value("G002"))
                    .andExpect(jsonPath("$.retryable").value(true))
                    .andExpect(jsonPath("$.debugMessage").value("GatewayTimeoutException: Payment service did not respond within 5000ms"))
                    .andExpect(jsonPath("$.stackTrace").exists());
        }

        @Test
        @DisplayName("ServiceUnavailableException includes debug info")
        void serviceUnavailableWithDebug() throws Exception {
            mockMvc.perform(get("/api/samples/service-unavailable"))
                    .andDo(print())
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(jsonPath("$.code").value("G003"))
                    .andExpect(jsonPath("$.retryable").value(true))
                    .andExpect(jsonPath("$.debugMessage").value("ServiceUnavailableException: Payment service is under maintenance"))
                    .andExpect(jsonPath("$.stackTrace").exists())
                    .andExpect(header().string("Retry-After", "30"));
        }

        @Test
        @DisplayName("RequestInProgressException includes debug info with POLL_STATUS")
        void requestInProgressWithDebug() throws Exception {
            mockMvc.perform(get("/api/samples/request-in-progress"))
                    .andDo(print())
                    .andExpect(status().isAccepted())
                    .andExpect(jsonPath("$.code").value("P001"))
                    .andExpect(jsonPath("$.retryable").value(true))
                    .andExpect(jsonPath("$.retryStrategy").value("POLL_STATUS"))
                    .andExpect(jsonPath("$.debugMessage").value("RequestInProgressException: Request is being processed by payment service"))
                    .andExpect(jsonPath("$.stackTrace").exists());
        }
    }

    @Nested
    @DisplayName("Malformed JSON in debug mode")
    class MalformedJsonDebug {

        @Test
        @DisplayName("includes HttpMessageNotReadableException debug info")
        void malformedJsonWithDebug() throws Exception {
            mockMvc.perform(post("/api/samples")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{invalid json"))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("C004"))
                    .andExpect(jsonPath("$.debugMessage").exists())
                    .andExpect(jsonPath("$.stackTrace").exists());
        }
    }
}
