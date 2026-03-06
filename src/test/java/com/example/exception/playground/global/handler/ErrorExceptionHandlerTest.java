package com.example.exception.playground.global.handler;

import com.example.exception.playground.sample.adapter.in.web.SampleErrorController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SampleErrorController.class)
class ErrorExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Nested
    @DisplayName("Type Mismatch (MethodArgumentTypeMismatchException)")
    class TypeMismatchTests {

        @Test
        @DisplayName("string for integer param returns 400")
        void typeMismatch() throws Exception {
            mockMvc.perform(get("/api/samples/type-mismatch")
                            .param("number", "abc"))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("C001"))
                    .andExpect(jsonPath("$.message").value("'number' should be of type 'Integer'"));
        }
    }

    @Nested
    @DisplayName("Missing Parameter (MissingServletRequestParameterException)")
    class MissingParameterTests {

        @Test
        @DisplayName("missing required query param returns 400")
        void missingParam() throws Exception {
            mockMvc.perform(get("/api/samples/missing-param"))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("C002"))
                    .andExpect(jsonPath("$.message").value("Required parameter 'required' of type 'String' is missing"));
        }
    }

    @Nested
    @DisplayName("Auth Exceptions")
    class AuthExceptionTests {

        @Test
        @DisplayName("UnauthorizedException returns 401")
        void unauthorized() throws Exception {
            mockMvc.perform(get("/api/samples/unauthorized"))
                    .andDo(print())
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("A001"))
                    .andExpect(jsonPath("$.message").value("Invalid or expired token"));
        }

        @Test
        @DisplayName("AccessDeniedException returns 403")
        void accessDenied() throws Exception {
            mockMvc.perform(get("/api/samples/access-denied"))
                    .andDo(print())
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("A002"))
                    .andExpect(jsonPath("$.message").value("Insufficient permissions to access this resource"));
        }
    }

    @Nested
    @DisplayName("Business Rule Violation")
    class BusinessRuleTests {

        @Test
        @DisplayName("BusinessRuleViolationException returns 422")
        void businessRuleViolation() throws Exception {
            mockMvc.perform(post("/api/samples/business-rule")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"name": "test", "age": 150}
                                    """))
                    .andDo(print())
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.code").value("B002"))
                    .andExpect(jsonPath("$.message").value("Age cannot exceed 100 for this operation"));
        }
    }

    @Nested
    @DisplayName("Unexpected Errors")
    class UnexpectedErrorTests {

        @Test
        @DisplayName("RuntimeException returns 500")
        void unexpectedError() throws Exception {
            mockMvc.perform(get("/api/samples/unexpected-error"))
                    .andDo(print())
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.code").value("C999"))
                    .andExpect(jsonPath("$.message").value("Internal server error"));
        }

        @Test
        @DisplayName("Unexpected error response does not include retryable/retryStrategy")
        void unexpectedErrorNoRetryFields() throws Exception {
            mockMvc.perform(get("/api/samples/unexpected-error"))
                    .andDo(print())
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.retryable").doesNotExist())
                    .andExpect(jsonPath("$.retryStrategy").doesNotExist());
        }
    }
}
