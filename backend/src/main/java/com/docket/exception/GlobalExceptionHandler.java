package com.docket.exception;

import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Global exception handler. Returns a consistent JSON error shape for all errors:
 * <pre>
 * { "error": { "message": "...", "code": "..." } }
 * </pre>
 *
 * Per rules.md §4: never leak stack traces to the client.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handles expected, user-facing errors (e.g., duplicate email, bad credentials).
     */
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Map<String, Object>> handleApiException(ApiException ex) {
        return ResponseEntity
                .status(ex.getStatus())
                .body(Map.of("error", Map.of(
                        "message", ex.getMessage(),
                        "code", ex.getCode()
                )));
    }

    /**
     * Handles Bean Validation failures on @Valid request bodies.
     * Returns the first field error as a user-readable message.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        String details = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", Map.of(
                        "message", details,
                        "code", "VALIDATION_ERROR"
                )));
    }

    /**
     * Catch-all for unexpected exceptions. Logs the full stack trace server-side
     * but returns only a generic message to the client — never leaks internals.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleUnexpected(Exception ex) {
        log.error("Unhandled exception", ex);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", Map.of(
                        "message", "An unexpected error occurred",
                        "code", "INTERNAL_ERROR"
                )));
    }
}
