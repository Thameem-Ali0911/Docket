package com.docket.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.docket.dto.auth.AuthResponse;
import com.docket.dto.auth.LoginRequest;
import com.docket.dto.auth.SignupRequest;
import com.docket.entity.User;
import com.docket.exception.ApiException;
import com.docket.repository.UserRepository;
import com.docket.service.AuthService;

import jakarta.validation.Valid;

/**
 * Authentication endpoints: signup, login, and session verification.
 *
 * POST /api/auth/signup  — register a new user + workspace, returns JWT
 * POST /api/auth/login   — authenticate, returns JWT
 * GET  /api/auth/me      — returns current user info from JWT (for session refresh)
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;

    public AuthController(AuthService authService, UserRepository userRepository) {
        this.authService = authService;
        this.userRepository = userRepository;
    }

    /**
     * Registers a new user with a workspace.
     *
     * @param request email, password (min 8 chars), workspaceName
     * @return 201 + AuthResponse with JWT token
     */
    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signup(@Valid @RequestBody SignupRequest request) {
        AuthResponse response = authService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Authenticates an existing user.
     *
     * @param request email, password
     * @return 200 + AuthResponse with JWT token
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Returns the current authenticated user's info.
     * Used by the frontend on page refresh to verify the session is still valid.
     *
     * @param authentication injected by Spring Security from the JWT filter
     * @return 200 + user email, workspaceId, workspaceName
     */
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> me(Authentication authentication) {
        Integer userId = (Integer) authentication.getPrincipal();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND",
                        "User not found"));

        return ResponseEntity.ok(Map.of(
                "email", user.getEmail(),
                "workspaceId", user.getWorkspace().getId(),
                "workspaceName", user.getWorkspace().getName()
        ));
    }
}
