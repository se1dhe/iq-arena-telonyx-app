package com.se1dhe.iqarena.common;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;

// Единый JSON ошибок.
@RestControllerAdvice
public class ApiErrorHandler {
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(IllegalArgumentException.class)
    public ErrorResponse badRequest(IllegalArgumentException ex) {
        return new ErrorResponse("BAD_REQUEST", ex.getMessage(), Instant.now().toString());
    }

    public record ErrorResponse(String code, String message, String timestamp) {}
}
