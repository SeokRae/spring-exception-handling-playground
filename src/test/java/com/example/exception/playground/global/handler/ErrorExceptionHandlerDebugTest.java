package com.example.exception.playground.global.handler;

import com.example.exception.playground.sample.adapter.in.web.SampleErrorController;
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

@WebMvcTest(SampleErrorController.class)
@TestPropertySource(properties = "app.debug=true")
@DisplayName("Debug Mode - SampleErrorController")
class ErrorExceptionHandlerDebugTest {

    @Autowired
    private MockMvc mockMvc;

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
}
