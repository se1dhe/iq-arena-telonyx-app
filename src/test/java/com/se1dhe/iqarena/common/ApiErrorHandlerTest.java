package com.se1dhe.iqarena.common;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ApiErrorHandlerTest {
    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new ValidationTestController())
            .setControllerAdvice(new ApiErrorHandler())
            .addFilters(new CorrelationIdFilter())
            .build();

    @Test
    void validationErrorUsesUnifiedJsonShape() throws Exception {
        mockMvc.perform(post("/test/validation")
                        .header(CorrelationIdFilter.HEADER_NAME, "trace-test")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"handle":"!"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(header().string(CorrelationIdFilter.HEADER_NAME, "trace-test"))
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.correlationId").value("trace-test"))
                .andExpect(jsonPath("$.violations[0].field").value("handle"));
    }

    @Test
    void malformedJsonUsesUnifiedJsonShape() throws Exception {
        mockMvc.perform(post("/test/validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_JSON"));
    }

    @RestController
    static class ValidationTestController {
        @PostMapping("/test/validation")
        TestResponse validate(@Valid @RequestBody TestRequest request) {
            return new TestResponse(request.handle());
        }
    }

    record TestRequest(
            @Pattern(regexp = "^[a-zA-Z0-9_]{3,24}$") String handle
    ) {}

    record TestResponse(String handle) {}
}
