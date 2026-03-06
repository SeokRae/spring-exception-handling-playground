package com.example.exception.playground.common;

import com.example.exception.playground.sample.SampleController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SampleController.class)
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Nested
    @DisplayName("Validation Errors (MethodArgumentNotValidException)")
    class ValidationTests {

        @Test
        @DisplayName("blank name returns 400 with field errors")
        void blankName() throws Exception {
            mockMvc.perform(post("/api/samples")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"name": "", "age": 25}
                                    """))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("C001"))
                    .andExpect(jsonPath("$.errors").isArray())
                    .andExpect(jsonPath("$.errors").isNotEmpty())
                    .andExpect(jsonPath("$.traceId").exists())
                    .andExpect(jsonPath("$.debugMessage").doesNotExist())
                    .andExpect(jsonPath("$.stackTrace").doesNotExist());
        }

        @Test
        @DisplayName("invalid age returns 400 with field errors")
        void invalidAge() throws Exception {
            mockMvc.perform(post("/api/samples")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"name": "valid", "age": 0}
                                    """))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("C001"))
                    .andExpect(jsonPath("$.errors[0].field").value("age"));
        }
    }

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
    @DisplayName("Method Not Allowed")
    class MethodNotAllowedTests {

        @Test
        @DisplayName("PATCH on GET-only endpoint returns 405")
        void methodNotAllowed() throws Exception {
            mockMvc.perform(patch("/api/samples/1"))
                    .andDo(print())
                    .andExpect(status().isMethodNotAllowed())
                    .andExpect(jsonPath("$.code").value("C005"));
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
    @DisplayName("Unsupported Media Type (HttpMediaTypeNotSupportedException)")
    class UnsupportedMediaTypeTests {

        @Test
        @DisplayName("XML content-type on JSON endpoint returns 415")
        void unsupportedMediaType() throws Exception {
            mockMvc.perform(post("/api/samples")
                            .contentType(MediaType.APPLICATION_XML)
                            .content("<name>test</name>"))
                    .andDo(print())
                    .andExpect(status().isUnsupportedMediaType())
                    .andExpect(jsonPath("$.code").value("C003"));
        }
    }

    @Nested
    @DisplayName("Message Not Readable (HttpMessageNotReadableException)")
    class MessageNotReadableTests {

        @Test
        @DisplayName("malformed JSON returns 400")
        void malformedJson() throws Exception {
            mockMvc.perform(post("/api/samples")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{invalid json"))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("C004"))
                    .andExpect(jsonPath("$.message").value("Malformed request body"));
        }
    }

    @Nested
    @DisplayName("Business Exceptions")
    class BusinessExceptionTests {

        @Test
        @DisplayName("NotFoundException returns 404 with message")
        void notFound() throws Exception {
            mockMvc.perform(get("/api/samples/0"))
                    .andDo(print())
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("C006"))
                    .andExpect(jsonPath("$.message").value("Sample with id 0 not found"));
        }

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

        @Test
        @DisplayName("DuplicateResourceException returns 409")
        void duplicate() throws Exception {
            mockMvc.perform(post("/api/samples")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"name": "duplicate", "age": 25}
                                    """))
                    .andDo(print())
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("B001"))
                    .andExpect(jsonPath("$.message").value("Sample with name 'duplicate' already exists"));
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
    }
}
