package com.docket.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Request body for POST /api/auth/login.
 *
 * @param email    user's email address
 * @param password plaintext password to verify against stored bcrypt hash
 */
public record LoginRequest(
        @NotBlank @Email
        String email,

        @NotBlank
        String password
) {
}
