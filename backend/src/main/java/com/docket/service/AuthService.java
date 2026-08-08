package com.docket.service;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.docket.dto.auth.AuthResponse;
import com.docket.dto.auth.LoginRequest;
import com.docket.dto.auth.SignupRequest;
import com.docket.entity.User;
import com.docket.entity.Workspace;
import com.docket.exception.ApiException;
import com.docket.repository.UserRepository;
import com.docket.repository.WorkspaceRepository;
import com.docket.security.JwtService;

/**
 * Handles signup and login business logic.
 *
 * Signup flow: check email uniqueness → create workspace → create user (bcrypt hash) → issue JWT.
 * Login flow: find user by email → verify password → issue JWT.
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final WorkspaceRepository workspaceRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository,
                       WorkspaceRepository workspaceRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.workspaceRepository = workspaceRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    /**
     * Registers a new user: creates a workspace, hashes the password, persists
     * the user, and returns a JWT.
     *
     * @param request signup details (email, password, workspaceName)
     * @return auth response with token and user/workspace info
     * @throws ApiException 409 if email is already registered
     */
    @Transactional
    public AuthResponse signup(SignupRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new ApiException(HttpStatus.CONFLICT, "EMAIL_EXISTS",
                    "An account with this email already exists");
        }

        Workspace workspace = workspaceRepository.save(new Workspace(request.workspaceName()));

        String hashedPassword = passwordEncoder.encode(request.password());
        User user = userRepository.save(new User(request.email(), hashedPassword, workspace));

        String token = jwtService.generateToken(user);

        return new AuthResponse(token, user.getEmail(), workspace.getId(), workspace.getName());
    }

    /**
     * Authenticates a user by email + password and returns a JWT.
     *
     * @param request login credentials
     * @return auth response with token and user/workspace info
     * @throws ApiException 401 if email not found or password doesn't match
     */
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS",
                        "Invalid email or password"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS",
                    "Invalid email or password");
        }

        String token = jwtService.generateToken(user);
        Workspace workspace = user.getWorkspace();

        return new AuthResponse(token, user.getEmail(), workspace.getId(), workspace.getName());
    }
}
