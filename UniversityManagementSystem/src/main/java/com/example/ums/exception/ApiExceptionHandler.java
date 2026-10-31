package com.example.ums.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

/**
 * Turns exceptions from controllers and services into JSON errors.
 * (401 and 403 happen before a controller runs, so SecurityConfig writes those.)
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    /** 404, 409 and the other errors the services raise. */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> handleStatus(ResponseStatusException ex) {
        String message = ex.getReason() == null ? "Request failed" : ex.getReason();
        return ResponseEntity.status(ex.getStatusCode()).body(Map.of("message", message));
    }

    /** 400: @Valid failed, so list every bad field. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        List<String> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .toList();
        return ResponseEntity.badRequest().body(Map.of("message", "Validation failed", "errors", errors));
    }

    /** 409: two requests raced for the same email or roll, and the database's unique rule caught it. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> handleConflict() {
        return ResponseEntity.status(409).body(Map.of("message", "That email or roll already exists"));
    }
}
