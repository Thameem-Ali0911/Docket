package com.docket.exception;

import org.springframework.http.HttpStatus;

/**
 * Custom exception for expected, user-facing errors (e.g., "email already exists",
 * "invalid credentials"). Carries an HTTP status and a machine-readable code.
 *
 * Caught by {@link GlobalExceptionHandler} and returned as a structured JSON error.
 */
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    public ApiException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }
}
