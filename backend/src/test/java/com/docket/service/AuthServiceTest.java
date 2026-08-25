package com.docket.service;

import java.lang.reflect.Field;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.docket.dto.auth.AuthResponse;
import com.docket.dto.auth.LoginRequest;
import com.docket.dto.auth.SignupRequest;
import com.docket.entity.User;
import com.docket.entity.Workspace;
import com.docket.exception.ApiException;
import com.docket.repository.UserRepository;
import com.docket.repository.WorkspaceRepository;
import com.docket.security.JwtService;
import com.docket.security.LoginRateLimiter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private WorkspaceRepository workspaceRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private LoginRateLimiter loginRateLimiter;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                userRepository,
                workspaceRepository,
                passwordEncoder,
                jwtService,
                loginRateLimiter
        );
    }

    private void setEntityId(Object entity, Integer id) throws Exception {
        Field field = entity.getClass().getDeclaredField("id");
        field.setAccessible(true);
        field.set(entity, id);
    }

    @Test
    @DisplayName("Signup creates workspace, hashes password, saves user and issues JWT")
    void testSignupSuccess() throws Exception {
        SignupRequest request = new SignupRequest("alice@company.com", "SecurePass123!", "Acme Org");

        Workspace savedWorkspace = new Workspace("Acme Org");
        setEntityId(savedWorkspace, 1);

        User savedUser = new User("alice@company.com", "$2a$10$hashedpw", savedWorkspace);
        setEntityId(savedUser, 10);

        when(userRepository.existsByEmail("alice@company.com")).thenReturn(false);
        when(workspaceRepository.save(any(Workspace.class))).thenReturn(savedWorkspace);
        when(passwordEncoder.encode("SecurePass123!")).thenReturn("$2a$10$hashedpw");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtService.generateToken(any(User.class))).thenReturn("jwt.token.value");

        AuthResponse response = authService.signup(request);

        assertNotNull(response);
        assertEquals("jwt.token.value", response.token());
        assertEquals("alice@company.com", response.email());
        assertEquals(1, response.workspaceId());
        assertEquals("Acme Org", response.workspaceName());
    }

    @Test
    @DisplayName("Signup with existing email throws 409 Conflict")
    void testSignupDuplicateEmail() {
        SignupRequest request = new SignupRequest("existing@company.com", "Password123!", "My Workspace");
        when(userRepository.existsByEmail("existing@company.com")).thenReturn(true);

        ApiException ex = assertThrows(ApiException.class, () -> authService.signup(request));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        assertEquals("EMAIL_EXISTS", ex.getCode());
    }

    @Test
    @DisplayName("Login with valid credentials succeeds and resets rate limiter")
    void testLoginSuccess() throws Exception {
        Workspace workspace = new Workspace("Acme Org");
        setEntityId(workspace, 1);

        User user = new User("bob@company.com", "$2a$10$correcthash", workspace);
        setEntityId(user, 20);

        LoginRequest request = new LoginRequest("bob@company.com", "correctpassword");

        when(loginRateLimiter.isBlocked("bob@company.com")).thenReturn(false);
        when(userRepository.findByEmail("bob@company.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("correctpassword", "$2a$10$correcthash")).thenReturn(true);
        when(jwtService.generateToken(user)).thenReturn("jwt.token.bob");

        AuthResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("jwt.token.bob", response.token());
        verify(loginRateLimiter).recordSuccess("bob@company.com");
    }

    @Test
    @DisplayName("Login with invalid password throws 401 and records failure")
    void testLoginInvalidPassword() {
        Workspace workspace = new Workspace("Acme Org");
        User user = new User("bob@company.com", "$2a$10$correcthash", workspace);

        LoginRequest request = new LoginRequest("bob@company.com", "wrongpassword");

        when(loginRateLimiter.isBlocked("bob@company.com")).thenReturn(false);
        when(userRepository.findByEmail("bob@company.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongpassword", "$2a$10$correcthash")).thenReturn(false);

        ApiException ex = assertThrows(ApiException.class, () -> authService.login(request));

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
        assertEquals("INVALID_CREDENTIALS", ex.getCode());
        verify(loginRateLimiter).recordFailure("bob@company.com");
    }

    @Test
    @DisplayName("Login when account is blocked throws 429 Too Many Requests")
    void testLoginLockedOut() {
        LoginRequest request = new LoginRequest("attacked@company.com", "anypassword");

        when(loginRateLimiter.isBlocked("attacked@company.com")).thenReturn(true);

        ApiException ex = assertThrows(ApiException.class, () -> authService.login(request));

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, ex.getStatus());
        assertEquals("ACCOUNT_LOCKED", ex.getCode());
        verify(userRepository, never()).findByEmail(anyString());
    }
}
