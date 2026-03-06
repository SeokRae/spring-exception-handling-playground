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
@DisplayName("Debug Mode - SampleController")
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
