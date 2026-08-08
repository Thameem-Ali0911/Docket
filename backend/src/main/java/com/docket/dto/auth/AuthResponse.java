package com.docket.dto.auth;

/**
 * Response body returned after successful signup or login.
 *
 * @param token         signed JWT for subsequent authenticated requests
 * @param email         the user's email
 * @param workspaceId   the user's workspace ID (for frontend scoping)
 * @param workspaceName the user's workspace display name
 */
public record AuthResponse(
        String token,
        String email,
        Integer workspaceId,
        String workspaceName
) {
}
