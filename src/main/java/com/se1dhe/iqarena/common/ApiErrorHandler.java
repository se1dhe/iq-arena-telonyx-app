package com.se1dhe.iqarena.common;

import jakarta.validation.ConstraintViolationException;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;
import java.util.List;

// Единый JSON ошибок.
@RestControllerAdvice
public class ApiErrorHandler {
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(IllegalArgumentException.class)
    public ErrorResponse badRequest(IllegalArgumentException ex) {
        return error("BAD_REQUEST", ex.getMessage(), List.of());
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ErrorResponse validationFailed(MethodArgumentNotValidException ex) {
        List<FieldViolation> violations = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::fieldViolation)
                .toList();
        return error("VALIDATION_FAILED", "Некорректные параметры запроса", violations);
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(ConstraintViolationException.class)
    public ErrorResponse constraintViolation(ConstraintViolationException ex) {
        List<FieldViolation> violations = ex.getConstraintViolations()
                .stream()
                .map(violation -> new FieldViolation(
                        violation.getPropertyPath().toString(),
                        violation.getMessage()
                ))
                .toList();
        return error("VALIDATION_FAILED", "Некорректные параметры запроса", violations);
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ErrorResponse unreadableJson() {
        return error("MALFORMED_JSON", "Некорректный JSON запроса", List.of());
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ErrorResponse typeMismatch(MethodArgumentTypeMismatchException ex) {
        return error("VALIDATION_FAILED", "Некорректные параметры запроса", List.of(
                new FieldViolation(ex.getName(), "Некорректный тип значения")
        ));
    }

    private ErrorResponse error(String code, String message, List<FieldViolation> violations) {
        return new ErrorResponse(code, message, Instant.now().toString(), correlationId(), violations);
    }

    private FieldViolation fieldViolation(FieldError fieldError) {
        String message = fieldError.getDefaultMessage() == null ? "Некорректное значение" : fieldError.getDefaultMessage();
        return new FieldViolation(fieldError.getField(), message);
    }

    private String correlationId() {
        String value = MDC.get(CorrelationIdFilter.MDC_KEY);
        return value == null ? "" : value;
    }

    public record ErrorResponse(
            String code,
            String message,
            String timestamp,
            String correlationId,
            List<FieldViolation> violations
    ) {}

    public record FieldViolation(String field, String message) {}
}
