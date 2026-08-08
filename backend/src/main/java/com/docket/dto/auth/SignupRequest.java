package com.docket.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for POST /api/auth/signup.
 *
 * @param email         user's email address
 * @param password      plaintext password (min 8 chars); will be bcrypt-hashed before storage
 * @param workspaceName display name for the new workspace
 */
public record SignupRequest(
        @NotBlank @Email
        String email,

        @NotBlank @Size(min = 8, message = "Password must be at least 8 characters")
        String password,

        @NotBlank
        String workspaceName
) {
}
